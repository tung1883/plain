package com.plainphone.app;

import android.app.Activity;
import android.content.Context;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

/**
 * The one entry point for locking a plugin (Notes, To-do, Vault, …) or for
 * "Lock all". A plugin with a background task running is never locked silently:
 * a single confirmation lists the running tasks, and locking pauses them (they
 * resume on the next unlock — see {@link VaultJobService} park logic).
 *
 * <p>Today only the vault runs tasks; {@link PluginTasks} is the registry the
 * rest hook into later.
 */
final class PluginLock {

    private PluginLock() {}

    /** Plugins that can carry a pausable background task. */
    private static Set<HomeMode> taskPlugins() {
        return EnumSet.of(HomeMode.VAULT);
    }

    /**
     * Lock one plugin (typically {@code {VAULT}}). Confirms only if it has a task
     * running. {@code onDone} runs after the user's choice, on the UI thread.
     */
    static void requestLock(Activity host, Set<HomeMode> toLock, Runnable onDone) {
        List<PluginTasks.Running> busy = PluginTasks.runningIn(host, toLock);
        if (busy.isEmpty()) {
            lockEach(host, toLock);
            done(onDone);
            return;
        }
        VaultUi.tasksDialog(host, title(busy), lines(busy),
                new String[]{"Lock", "Keep unlocked"},
                new VaultUi.Choice[]{
                        () -> { lockEach(host, toLock); done(onDone); },
                        () -> done(onDone),
                });
    }

    /**
     * "Lock all". {@code lockOther} locks everything that isn't a task-carrying
     * plugin (the launcher sections + per-app grace) — run in every branch except
     * "Keep unlocked". If any plugin is busy the user picks:
     * <ul>
     *   <li><b>Lock all</b> — also lock the busy plugins (pausing their tasks)</li>
     *   <li><b>Lock the rest</b> — leave the busy plugins running</li>
     *   <li><b>Keep unlocked</b> — lock nothing</li>
     * </ul>
     */
    static void requestLockAll(Activity host, Runnable lockOther, Runnable onDone) {
        List<PluginTasks.Running> busy = PluginTasks.running(host);
        Set<HomeMode> all = taskPlugins();

        if (busy.isEmpty()) {
            lockOther.run();
            lockEach(host, all);
            done(onDone);
            return;
        }

        Set<HomeMode> rest = EnumSet.copyOf(all);
        for (PluginTasks.Running r : busy) rest.remove(r.plugin);

        VaultUi.tasksDialog(host, title(busy), lines(busy),
                new String[]{"Lock all", "Lock the rest", "Keep unlocked"},
                new VaultUi.Choice[]{
                        () -> { lockOther.run(); lockEach(host, all); done(onDone); },
                        () -> { lockOther.run(); lockEach(host, rest); done(onDone); },
                        () -> done(onDone),
                });
    }

    // --- helpers -----------------------------------------------

    private static String title(List<PluginTasks.Running> busy) {
        return busy.size() + (busy.size() == 1 ? " task still running" : " tasks still running");
    }

    private static List<String> lines(List<PluginTasks.Running> busy) {
        List<String> out = new ArrayList<>();
        for (PluginTasks.Running r : busy) out.add(r.line());
        return out;
    }

    private static void done(Runnable r) {
        if (r != null) r.run();
    }

    private static void lockEach(Context ctx, Set<HomeMode> plugins) {
        for (HomeMode p : plugins) {
            if (p == HomeMode.VAULT) {
                if (VaultSession.get().isUnlocked()) {
                    VaultUnlockService.stop(ctx);
                    VaultSession.get().lock(ctx.getApplicationContext());
                }
            } else if (p == HomeMode.NOTES) {
                lockSection(ctx, Lock.NOTES);
            } else if (p == HomeMode.TODOS) {
                lockSection(ctx, Lock.TODOS);
            }
        }
    }

    private static void lockSection(Context ctx, Lock lock) {
        lock.setLocked(ctx, true);
        Config.setUnlockUntil(ctx, lock.area, 0L);
    }
}
