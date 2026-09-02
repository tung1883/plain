package com.plainphone.app;

import android.content.Context;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Which plugins (Notes, To-do, Vault, …) have background work in flight right now.
 * A plugin with a running task is kept unlocked until it finishes, and a deliberate
 * lock of it is confirmed first ({@link PluginLock}).
 *
 * <p>Today only the vault produces tasks; other plugins register a {@link Source}
 * when they grow one.
 */
final class PluginTasks {

    private PluginTasks() {}

    interface Source {
        HomeMode plugin();
        boolean running(Context context);
        /** e.g. "importing Downloads", or null. */
        String detail(Context context);
    }

    static final class Running {
        final HomeMode plugin;
        final String detail;

        Running(HomeMode plugin, String detail) {
            this.plugin = plugin;
            this.detail = detail;
        }

        String line() {
            return detail != null ? plugin.label + " — " + detail : plugin.label;
        }
    }

    private static final CopyOnWriteArrayList<Source> sources = new CopyOnWriteArrayList<>();

    static {
        sources.add(new Source() {
            public HomeMode plugin() { return HomeMode.VAULT; }
            public boolean running(Context c) { return VaultJobs.anyPending(c); }
            public String detail(Context c) { return VaultJobs.activeLabel(c); }
        });
    }

    static void register(Source s) {
        sources.addIfAbsent(s);
    }

    static List<Running> running(Context context) {
        List<Running> out = new ArrayList<>();
        for (Source s : sources) {
            if (s.running(context)) out.add(new Running(s.plugin(), s.detail(context)));
        }
        return out;
    }

    static List<Running> runningIn(Context context, Set<HomeMode> plugins) {
        List<Running> out = new ArrayList<>();
        for (Running r : running(context)) {
            if (plugins.contains(r.plugin)) out.add(r);
        }
        return out;
    }

    static boolean any(Context context) {
        return !running(context).isEmpty();
    }
}
