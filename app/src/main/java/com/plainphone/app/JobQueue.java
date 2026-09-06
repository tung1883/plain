package com.plainphone.app;

import android.content.Context;
import android.content.Intent;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Global persistent job queue. Feature-specific facades such as {@link VaultJobs}
 * and {@link ImportJobs} keep their public API, but their records live here.
 */
final class JobQueue {

    private JobQueue() {}

    static final String STATUS_QUEUED = "queued";
    static final String STATUS_RUNNING = "running";
    static final String STATUS_PAUSED = "paused";

    static final String AREA_VAULT = "vault";
    static final String AREA_NOTES = "notes";
    static final String AREA_TODOS = "todos";
    static final String AREA_RECORDER = "recorder";
    static final String AREA_SEARCH = "search";

    interface Listener { void onJobsChanged(); }

    private static final CopyOnWriteArrayList<Listener> listeners = new CopyOnWriteArrayList<>();

    static void addListener(Listener l) { listeners.addIfAbsent(l); }
    static void removeListener(Listener l) { listeners.remove(l); }

    static final class Job {
        String id;
        String type;
        String label;
        String status;
        long createdAt;
        int priority;
        Set<String> keepUnlockedAreas = new LinkedHashSet<>();
        Set<String> requiredUnlockedAreas = new LinkedHashSet<>();
        Map<String, String> data = new LinkedHashMap<>();

        File dir(Context context) {
            return new File(dir(context), id);
        }
    }

    static final class Spec {
        final String type;
        String label;
        int priority;
        final Set<String> keepUnlockedAreas = new LinkedHashSet<>();
        final Set<String> requiredUnlockedAreas = new LinkedHashSet<>();
        final Map<String, String> data = new LinkedHashMap<>();
        final Map<String, String> textFiles = new LinkedHashMap<>();

        Spec(String type) {
            this.type = type;
        }

        Spec label(String value) {
            label = value;
            return this;
        }

        Spec priority(int value) {
            priority = value;
            return this;
        }

        Spec keep(String... areas) {
            keepUnlockedAreas.addAll(Arrays.asList(areas));
            return this;
        }

        Spec require(String... areas) {
            requiredUnlockedAreas.addAll(Arrays.asList(areas));
            return this;
        }

        Spec put(String key, String value) {
            if (value != null) data.put(key, value);
            return this;
        }

        Spec file(String name, String content) {
            textFiles.put(name, content == null ? "" : content);
            return this;
        }
    }

    static Job enqueue(Context context, Spec spec) {
        Job job = enqueue(dir(context), spec);
        publish();
        kick(context);
        return job;
    }

    static Job enqueueForTest(File root, Spec spec) {
        Job job = enqueue(root, spec);
        publish();
        return job;
    }

    static Job enqueueMigrated(Context context, Spec spec) {
        Job job = enqueue(dir(context), spec);
        publish();
        return job;
    }

    private static Job enqueue(File root, Spec spec) {
        File d = newJobDir(root);
        Job job = new Job();
        job.id = d.getName();
        job.type = spec.type;
        job.label = spec.label;
        job.status = STATUS_QUEUED;
        job.createdAt = System.currentTimeMillis();
        job.priority = spec.priority;
        job.keepUnlockedAreas.addAll(spec.keepUnlockedAreas);
        job.requiredUnlockedAreas.addAll(spec.requiredUnlockedAreas);
        job.data.putAll(spec.data);
        writeJob(d, job);
        for (Map.Entry<String, String> e : spec.textFiles.entrySet()) {
            try {
                atomicWrite(new File(d, e.getKey()), e.getValue());
            } catch (IOException ex) {
                android.util.Log.w("JobQueue", "job sidecar write failed", ex);
            }
        }
        return job;
    }

    static void resumeIfPending(Context context) {
        if (anyPending(context)) kick(context);
    }

    static boolean anyPending(Context context) {
        return !pending(context).isEmpty();
    }

    static boolean anyOfType(Context context, String type) {
        for (Job job : pending(context)) if (type.equals(job.type)) return true;
        return false;
    }

    static boolean anyWithPrefix(Context context, String prefix) {
        for (Job job : pending(context)) {
            if (job.type != null && job.type.startsWith(prefix)) return true;
        }
        return false;
    }

    static boolean keepsUnlocked(Context context, String area) {
        for (Job job : pending(context)) {
            if (job.keepUnlockedAreas.contains(area)) return true;
        }
        return false;
    }

    static boolean touchesArea(Context context, String area) {
        for (Job job : pending(context)) {
            if (job.keepUnlockedAreas.contains(area)
                    || job.requiredUnlockedAreas.contains(area)) return true;
        }
        return false;
    }

    static List<Job> pending(Context context) {
        return pending(dir(context));
    }

    static List<Job> pendingForTest(File root) {
        return pending(root);
    }

    private static List<Job> pending(File root) {
        List<Job> out = new ArrayList<>();
        root.mkdirs();
        File[] kids = root.listFiles();
        if (kids == null) return out;
        Arrays.sort(kids, (a, b) -> Long.compare(a.lastModified(), b.lastModified()));
        for (File d : kids) {
            if (!d.isDirectory()) continue;
            Job job = readJob(d);
            if (job != null) out.add(job);
        }
        out.sort((a, b) -> {
            if (a.priority != b.priority) return Integer.compare(b.priority, a.priority);
            return Long.compare(a.createdAt, b.createdAt);
        });
        return out;
    }

    static List<Job> pendingByType(Context context, String type) {
        List<Job> out = new ArrayList<>();
        for (Job job : pending(context)) if (type.equals(job.type)) out.add(job);
        return out;
    }

    static List<Job> pendingByPrefix(Context context, String prefix) {
        List<Job> out = new ArrayList<>();
        for (Job job : pending(context)) {
            if (job.type != null && job.type.startsWith(prefix)) out.add(job);
        }
        return out;
    }

    static Job first(Context context) {
        List<Job> jobs = pending(context);
        return jobs.isEmpty() ? null : jobs.get(0);
    }

    static void setStatus(Context context, String id, String status) {
        setStatus(dir(context), id, status);
        publish();
    }

    static void setStatusForTest(File root, String id, String status) {
        setStatus(root, id, status);
        publish();
    }

    private static void setStatus(File root, String id, String status) {
        File d = new File(root, id);
        Job job = readJob(d);
        if (job == null) return;
        job.status = status;
        writeJob(d, job);
    }

    static void clear(Context context, String id) {
        deleteRecursively(new File(dir(context), id));
        publish();
    }

    static void clearForTest(File root, String id) {
        deleteRecursively(new File(root, id));
        publish();
    }

    static File file(Context context, String id, String name) {
        return new File(new File(dir(context), id), name);
    }

    static String readText(Context context, String id, String name) {
        return readAll(file(context, id, name));
    }

    static void writeText(Context context, String id, String name, String content) {
        try {
            atomicWrite(file(context, id, name), content == null ? "" : content);
            publish();
        } catch (IOException e) {
            android.util.Log.w("JobQueue", "checkpoint write failed", e);
        }
    }

    static void clearAll(Context context) {
        deleteRecursively(dir(context));
        dir(context).mkdirs();
        publish();
    }

    static void clearAllForTest(File root) {
        deleteRecursively(root);
        root.mkdirs();
        publish();
    }

    static void publish() {
        for (Listener l : listeners) l.onJobsChanged();
    }

    static File dir(Context context) {
        File d = new File(context.getFilesDir(), "jobs");
        d.mkdirs();
        return d;
    }

    private static void kick(Context context) {
        context.getApplicationContext().startForegroundService(
                new Intent(context, JobService.class));
    }

    private static File newJobDir(Context context) {
        return newJobDir(dir(context));
    }

    private static File newJobDir(File root) {
        long stamp = System.currentTimeMillis();
        File d = new File(root, Long.toString(stamp));
        for (int i = 1; d.exists(); i++) d = new File(root, stamp + "-" + i);
        d.mkdirs();
        return d;
    }

    private static void writeJob(File dir, Job job) {
        Properties p = new Properties();
        p.setProperty("id", job.id);
        p.setProperty("type", job.type);
        p.setProperty("status", job.status == null ? STATUS_QUEUED : job.status);
        p.setProperty("createdAt", Long.toString(job.createdAt));
        p.setProperty("priority", Integer.toString(job.priority));
        if (job.label != null) p.setProperty("label", job.label);
        p.setProperty("keep", join(job.keepUnlockedAreas));
        p.setProperty("require", join(job.requiredUnlockedAreas));
        for (Map.Entry<String, String> e : job.data.entrySet()) {
            p.setProperty("data." + e.getKey(), e.getValue());
        }
        File f = new File(dir, "job.properties");
        File tmp = new File(dir, "job.properties.tmp");
        try (FileOutputStream out = new FileOutputStream(tmp)) {
            p.store(out, null);
            out.getFD().sync();
        } catch (IOException e) {
            android.util.Log.w("JobQueue", "job write failed", e);
        }
        if (!tmp.renameTo(f)) {
            try (FileOutputStream out = new FileOutputStream(f)) {
                p.store(out, null);
                out.getFD().sync();
            } catch (IOException e) {
                android.util.Log.w("JobQueue", "job overwrite failed", e);
            }
            tmp.delete();
        }
    }

    private static Job readJob(File dir) {
        File f = new File(dir, "job.properties");
        if (!f.exists()) return null;
        Properties p = new Properties();
        try (FileInputStream in = new FileInputStream(f)) {
            p.load(in);
        } catch (IOException e) {
            return null;
        }
        Job job = new Job();
        job.id = p.getProperty("id", dir.getName());
        job.type = p.getProperty("type");
        if (job.type == null) return null;
        job.label = p.getProperty("label");
        job.status = p.getProperty("status", STATUS_QUEUED);
        try {
            job.createdAt = Long.parseLong(p.getProperty("createdAt", "0"));
        } catch (NumberFormatException e) {
            job.createdAt = dir.lastModified();
        }
        try {
            job.priority = Integer.parseInt(p.getProperty("priority", "0"));
        } catch (NumberFormatException e) {
            job.priority = 0;
        }
        job.keepUnlockedAreas.addAll(split(p.getProperty("keep", "")));
        job.requiredUnlockedAreas.addAll(split(p.getProperty("require", "")));
        for (String name : p.stringPropertyNames()) {
            if (name.startsWith("data.")) {
                job.data.put(name.substring(5), p.getProperty(name));
            }
        }
        return job;
    }

    private static String join(Set<String> values) {
        StringBuilder sb = new StringBuilder();
        for (String value : values) {
            if (sb.length() > 0) sb.append(',');
            sb.append(value);
        }
        return sb.toString();
    }

    private static List<String> split(String value) {
        List<String> out = new ArrayList<>();
        if (value == null || value.isEmpty()) return out;
        for (String part : value.split(",")) {
            String trimmed = part.trim();
            if (!trimmed.isEmpty()) out.add(trimmed);
        }
        return out;
    }

    static void atomicWrite(File f, String content) throws IOException {
        File parent = f.getParentFile();
        if (parent != null) parent.mkdirs();
        File tmp = new File(parent, f.getName() + ".tmp");
        try (FileOutputStream out = new FileOutputStream(tmp)) {
            out.write(content.getBytes(StandardCharsets.UTF_8));
            out.getFD().sync();
        }
        if (!tmp.renameTo(f)) {
            try (FileOutputStream out = new FileOutputStream(f)) {
                out.write(content.getBytes(StandardCharsets.UTF_8));
                out.getFD().sync();
            }
            tmp.delete();
        }
    }

    static String readAll(File f) {
        try (FileInputStream in = new FileInputStream(f)) {
            byte[] buf = new byte[(int) f.length()];
            int n = 0;
            while (n < buf.length) {
                int r = in.read(buf, n, buf.length - n);
                if (r < 0) break;
                n += r;
            }
            return new String(buf, 0, n, StandardCharsets.UTF_8);
        } catch (IOException e) {
            return "";
        }
    }

    private static void deleteRecursively(File file) {
        if (file == null || !file.exists()) return;
        if (file.isDirectory()) {
            File[] kids = file.listFiles();
            if (kids != null) for (File kid : kids) deleteRecursively(kid);
        }
        file.delete();
    }
}
