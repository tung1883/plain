package com.plainphone.app;

import android.os.Handler;
import android.os.Looper;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * One shared background pool for all Dev-plugin service panels. Deliberately
 * never shut down: a panel dropped without {@link PanelContent#onClose()} (the
 * device-gone path) can't leak a thread. Staleness is handled per-panel with a
 * {@code generation} int, not by cancelling futures.
 *
 * <p>Also a courtesy per-host throttle so a burst of panels can't hammer an API.
 */
final class NetIo {

    private NetIo() {}

    static final Handler MAIN = new Handler(Looper.getMainLooper());

    static final ExecutorService POOL = Executors.newFixedThreadPool(2, new ThreadFactory() {
        private final AtomicInteger n = new AtomicInteger(1);
        @Override public Thread newThread(Runnable r) {
            Thread t = new Thread(r, "dev-netio-" + n.getAndIncrement());
            t.setDaemon(true);
            t.setPriority(Thread.NORM_PRIORITY - 1);
            return t;
        }
    });

    private static final ConcurrentHashMap<String, Long> nextAllowedAt = new ConcurrentHashMap<>();

    /** True while {@code host} is in a back-off window set by {@link #backOff}. */
    static boolean throttled(String host) {
        Long until = nextAllowedAt.get(host);
        return until != null && System.currentTimeMillis() < until;
    }

    /** Milliseconds until {@code host} is allowed again, or 0. */
    static long retryAfterMs(String host) {
        Long until = nextAllowedAt.get(host);
        if (until == null) return 0;
        long left = until - System.currentTimeMillis();
        return left > 0 ? left : 0;
    }

    static void backOff(String host, long forMs) {
        nextAllowedAt.put(host, System.currentTimeMillis() + Math.max(0, forMs));
    }

    static void clearBackOff(String host) {
        nextAllowedAt.remove(host);
    }
}
