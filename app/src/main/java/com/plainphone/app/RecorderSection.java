package com.plainphone.app;

import android.app.Activity;
import android.content.Intent;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * The home Recorder section, as a reusable block of rows — shared by
 * {@link MainActivity} and a workspace {@link RecorderPanel}. Lock gating and
 * multi-select stay with the caller.
 */
final class RecorderSection {

    private RecorderSection() {}

    static void render(SectionHost host, List<Object> rows) {
        Activity a = host.activity();
        SearchResult.Kind K = SearchResult.Kind.RECORDING;

        boolean open = host.settingsOpen(HomeMode.RECORDER);
        rows.add(new SearchResult(K, (open ? "▾  " : "▸  ") + "Recorder settings", null, -1,
                () -> { host.setSettingsOpen(HomeMode.RECORDER, !open); host.refresh(); }));
        if (open) {
            rows.add(new SearchResult(K,
                    "Format: " + Config.getRecorderFormat(a).toUpperCase(Locale.US), null, -1,
                    () -> a.startActivity(new Intent(a, RecorderSettingsActivity.class))));
            rows.add(new SearchResult(K,
                    "Sample rate: " + Config.getRecorderSampleRate(a) + " Hz", null, -1,
                    () -> a.startActivity(new Intent(a, RecorderSettingsActivity.class))));
            rows.add(new SearchResult(K, "Locked: " + (Lock.RECORDER.isLocked(a) ? "On" : "Off"),
                    null, -1, () -> Lock.RECORDER.toggleLock(a, host::refresh)));
            int local = Recorder.all(a).size();
            if (local > 0 && VaultFormat.exists(VaultSession.vaultRoot(a))) {
                rows.add(new SearchResult(K, "Move all recordings to vault", null, -1,
                        () -> moveAllToVault(host, local)));
            }
        }

        if (SectionJobs.pendingFor(a, HomeMode.RECORDER)) {
            rows.add(SectionHost.inert(K, SectionJobs.progressLine(a, HomeMode.RECORDER)));
        } else if (ImportJobs.pendingForPlugin(a, HomeMode.RECORDER)) {
            rows.add(SectionHost.inert(K, ImportJobs.progressLine(a, HomeMode.RECORDER)));
        } else {
            rows.add(new SearchResult(K, "+ Import audio", null, -1,
                    () -> host.pickImport(HomeMode.RECORDER, "audio/*")));
        }
        rows.add(new SearchResult(K, "+ New recording", null, -1,
                () -> a.startActivity(new Intent(a, RecordActivity.class))));

        String detail = RecorderService.isActive(a) ? RecorderService.activeDetail() : null;
        if (detail != null && android.os.SystemClock.elapsedRealtime() >= suppressCardUntil) {
            if ("recording".equals(detail)) rows.add(recordingCard(host));
            else if ("playing a recording".equals(detail)) rows.add(playbackCard(host));
        }

        for (Recording r : Recorder.orderedAll(a)) {
            final String id = r.id;
            rows.add(new SearchResult(K, r.displayName(), r.subtitle(), -1, () -> open(a, id), r));
        }

        if (!VaultSession.get().isUnlocked() && VaultFormat.exists(VaultSession.vaultRoot(a))
                && Config.getRecordingCount(a) > 0) {
            int n = Config.getRecordingCount(a);
            rows.add(new SearchResult(K,
                    n + (n == 1 ? " recording in the vault" : " recordings in the vault"),
                    "Unlock to play", -1, host::unlockVault));
        }

        Recorder.healVaultDurations(a, () -> a.runOnUiThread(() -> {
            if (!a.isFinishing() && !a.isDestroyed()) host.refresh();
        }));
    }

    // Set the instant a Stop tap fires so the card hides right away instead of waiting on the
    // async ACTION_STOP round trip — a fixed window rather than "until the service goes idle",
    // since that observation depends on some render() landing in the gap, which isn't guaranteed
    // (a fast stop-then-restart could otherwise leave the card permanently suppressed).
    private static volatile long suppressCardUntil;
    private static final long SUPPRESS_MS = 1200;

    private static SearchResult recordingCard(SectionHost host) {
        Activity a = host.activity();
        boolean paused = RecorderService.activeRecordingPaused();
        return new SearchResult(SearchResult.Kind.RECORDING, "Recording",
                fmtTime(RecorderService.activeRecordingElapsedMs()), -1,
                () -> a.startActivity(new Intent(a, RecordActivity.class)))
                .live()
                .withAction(paused ? "▶" : "❚❚", () -> togglePause(host, paused))
                .withAction("■", () -> stopActive(host));
    }

    private static SearchResult playbackCard(SectionHost host) {
        Activity a = host.activity();
        String name = RecorderService.activePlayName();
        String sub = fmtTime(RecorderService.activePlayPositionMs()) + " / "
                + fmtTime(RecorderService.activePlayDurationMs());
        boolean playing = RecorderService.activePlayingNow();
        return new SearchResult(SearchResult.Kind.RECORDING, name == null ? "Recording" : name, sub, -1,
                () -> a.startActivity(RecorderService.activePlayerIntent(a)))
                .live()
                .withAction(playing ? "❚❚" : "▶", () -> {
                    boolean want = !playing;
                    a.startForegroundService(new Intent(a, RecorderService.class)
                            .setAction(RecorderService.ACTION_PLAY_TOGGLE));
                    host.refresh();
                    pollUntil(host, () -> RecorderService.activePlayingNow() == want, 12);
                })
                .withAction("■", () -> stopActive(host));
    }

    private static void togglePause(SectionHost host, boolean paused) {
        Activity a = host.activity();
        boolean want = !paused;
        a.startForegroundService(new Intent(a, RecorderService.class)
                .setAction(paused ? RecorderService.ACTION_RESUME : RecorderService.ACTION_PAUSE));
        host.refresh();
        pollUntil(host, () -> RecorderService.activeRecordingPaused() == want, 12);
    }

    // ACTION_PAUSE/RESUME/PLAY_TOGGLE are handled async — startForegroundService() round-trips
    // through the system before the service flips its static state, so a single fixed-delay
    // refresh either fires too early (stale) or leaves a visible gap. Poll in short bursts and
    // stop as soon as the state actually matches instead of guessing a delay.
    private static void pollUntil(SectionHost host, java.util.function.BooleanSupplier done, int triesLeft) {
        if (triesLeft <= 0) return;
        new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(() -> {
            host.refresh();
            if (!done.getAsBoolean()) pollUntil(host, done, triesLeft - 1);
        }, 30);
    }

    private static void stopActive(SectionHost host) {
        suppressCardUntil = android.os.SystemClock.elapsedRealtime() + SUPPRESS_MS;
        Activity a = host.activity();
        a.startForegroundService(new Intent(a, RecorderService.class)
                .setAction(RecorderService.ACTION_STOP));
        host.refresh();
        // ACTION_STOP is handled async (posted to the service's queue) — persistRecording()
        // hasn't run yet at this point, so the new recording isn't in the list. Catch it once
        // that's had time to land rather than waiting on the 1s home tick.
        new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(host::refresh, 400);
    }

    private static String fmtTime(long ms) {
        long s = Math.max(0, ms) / 1000;
        return (s / 60) + ":" + String.format(Locale.US, "%02d", s % 60);
    }

    static String selectionId(Object payload) {
        return payload instanceof Recording ? ((Recording) payload).id : null;
    }

    private static boolean isPlayingAnyOf(List<Recording> sel) {
        if (!"playing a recording".equals(RecorderService.activeDetail())) return false;
        String recId = RecorderService.activePlayRecId();
        String docId = RecorderService.activePlayDocId();
        for (Recording r : sel) {
            if (Recorder.isVaulted(r.id)) {
                if (docId != null && Recorder.docIdOf(r.id).equals(docId)) return true;
            } else if (recId != null && r.id.equals(recId)) {
                return true;
            }
        }
        return false;
    }

    static void renderSelection(SelectionHost host, List<Object> rows) {
        Activity a = host.activity();
        SearchResult.Kind K = SearchResult.Kind.RECORDING;
        List<Recording> list = Recorder.orderedAll(a);

        List<String> ids = new ArrayList<>();
        for (Recording r : list) ids.add(r.id);
        List<Recording> sel = new ArrayList<>();
        for (Recording r : list) if (host.selection().contains(r.id)) sel.add(r);
        int locals = 0, vaulted = 0;
        for (Recording r : sel) { if (Recorder.isVaulted(r.id)) vaulted++; else locals++; }

        List<BarAction> actions = new ArrayList<>();
        if (locals > 0 && VaultFormat.exists(VaultSession.vaultRoot(a))) {
            actions.add(new BarAction("Move to vault", () -> {
                if (!VaultSession.get().isUnlocked()) { host.unlockVault(); return; }
                List<String> mv = new ArrayList<>();
                for (Recording r : sel) if (!Recorder.isVaulted(r.id)) mv.add(r.id);
                SectionJobs.startRecorderToVault(a, mv);
                Toast.makeText(a, "Move queued", Toast.LENGTH_SHORT).show();
                host.exitSelection();
            }));
        }
        if (vaulted > 0) {
            actions.add(new BarAction("Move out", () -> {
                List<String> mv = new ArrayList<>();
                for (Recording r : sel) if (Recorder.isVaulted(r.id)) mv.add(r.id);
                SectionJobs.startRecorderFromVault(a, mv);
                Toast.makeText(a, "Move queued", Toast.LENGTH_SHORT).show();
                host.exitSelection();
            }));
        }
        if (locals > 0) {
            actions.add(new BarAction("Export", () -> {
                List<android.net.Uri> uris = new ArrayList<>();
                for (Recording r : sel) {
                    if (Recorder.isVaulted(r.id)) continue;
                    java.io.File f = Recorder.fileFor(a, r);
                    if (f.isFile()) uris.add(PlainFileProvider.uriFor(a.getPackageName() + ".files", f));
                }
                Sharing.sendFiles(a, uris, "audio/*", "Send recordings");
            }));
        }
        if (sel.size() == 1 && locals == 1) {
            Recording one = sel.get(0);
            actions.add(new BarAction("Rename", () -> UiKit.textPrompt(a, "Rename recording",
                    one.displayName(), "Save", name -> {
                        Recorder.rename(a, one.id, name);
                        host.exitSelection();
                    })));
        }
        actions.add(new BarAction("Delete", () -> VaultUi.confirm(a,
                "Delete " + sel.size() + " recording" + (sel.size() == 1 ? "" : "s") + "?",
                null, "Delete", () -> {
                    // The file's still open in the MediaPlayer, so this wouldn't crash even
                    // without the check — it'd just keep playing a file with no list entry
                    // until something needs to reopen it (skip, vault relock/resume), which
                    // then fails silently. Stop it up front instead of leaving that orphan.
                    if (isPlayingAnyOf(sel)) {
                        suppressCardUntil = android.os.SystemClock.elapsedRealtime() + SUPPRESS_MS;
                        a.startForegroundService(new Intent(a, RecorderService.class)
                                .setAction(RecorderService.ACTION_STOP));
                    }
                    for (Recording r : sel) {
                        if (Recorder.isVaulted(r.id)) Recorder.deleteVaultRecording(a, r.id);
                        else Recorder.deleteLocal(a, r);
                    }
                    host.exitSelection();
                }, "Cancel", null)));
        host.selectionBar().bind(host, ids, actions);

        for (Recording r : list) {
            final String id = r.id;
            rows.add(new SearchResult(K, r.displayName(), r.subtitle(), -1,
                    () -> host.toggle(id), r).check(host.selection().contains(id)));
        }
    }

    private static void open(Activity a, String id) {
        Intent i = new Intent(a, RecordingPlayerActivity.class);
        if (Recorder.isVaulted(id)) {
            String name = Recorder.vaultRecordingName(a, id);
            int dot = name.lastIndexOf('.');
            i.putExtra("docId", Recorder.docIdOf(id));
            i.putExtra("name", dot > 0 ? name.substring(0, dot) : name);
            i.putExtra("format", dot >= 0 ? name.substring(dot + 1).toLowerCase() : "m4a");
        } else {
            i.putExtra("recId", id);
        }
        a.startActivity(i);
    }

    private static void moveAllToVault(SectionHost host, int count) {
        Activity a = host.activity();
        if (!VaultSession.get().isUnlocked()) { host.unlockVault(); return; }
        VaultUi.confirm(a, "Move " + count + " recording" + (count == 1 ? "" : "s") + " to the vault?",
                "They'll be encrypted and only playable while the vault is unlocked.",
                "Move", () -> {
                    List<String> ids = new ArrayList<>();
                    for (Recording r : Recorder.all(a)) ids.add(r.id);
                    SectionJobs.startRecorderToVault(a, ids);
                    Toast.makeText(a, "Move queued", Toast.LENGTH_SHORT).show();
                    host.refresh();
                }, "Cancel", null);
    }
}
