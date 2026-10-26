package com.plainphone.app;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;

import java.util.ArrayList;
import java.util.List;

/**
 * The checks that stand between the user and an app they have restricted: a blocking time block, the
 * app PIN lock, the flagged-app countdown. Opening an app from the app list, and opening a file or link
 * that another app will show, go through the same rules so search cannot be used to get around them.
 */
final class GatedLaunch {

    private GatedLaunch() {}

    /**
     * Run the gates for opening {@code pkg}. Returns true when one was shown (the caller must not
     * start the app: the gate does it once passed). Otherwise returns false, after marking a locked
     * app as unlocked, and the caller starts the app.
     */
    static boolean gate(Activity host, String pkg, CharSequence label) {
        ForegroundExtras.beforeOpen(host, pkg);   // a blocking app: accessibility off before any gate

        TimeBlock blockingBlock = TimeBlockRules.getBlockingBlock(host, pkg);
        if (blockingBlock != null) {
            Intent gate = new Intent(host, TimeBlockGateActivity.class);
            gate.putExtra("package", pkg);
            gate.putExtra("blockId", blockingBlock.id);
            host.startActivity(gate);
            return true;
        }

        if (needsPin(host, pkg)) {
            Intent gate = new Intent(host, PinGateActivity.class);
            gate.putExtra("package", pkg);
            gate.putExtra("label", label.toString());
            host.startActivity(gate);
            return true;
        }

        if (Config.getFlaggedPackages(host).contains(pkg)) {
            Intent gate = new Intent(host, FlaggedGateActivity.class);
            gate.putExtra("package", pkg);
            gate.putExtra("label", label.toString());
            host.startActivity(gate);
            return true;
        }

        if (Config.getLockedPackages(host).contains(pkg)) {
            Config.markAppUnlocked(host, pkg);
        }
        return false;
    }

    private static boolean needsPin(Activity host, String pkg) {
        return Config.isLocksEnabled(host)
                && Config.isApplockEnabled(host)
                && Config.getLockedPackages(host).contains(pkg)
                && !Config.isAppRecentlyUnlocked(host, pkg);
    }

    /** True when opening {@code pkg} would currently meet a gate. */
    static boolean isGated(Activity host, String pkg) {
        return TimeBlockRules.getBlockingBlock(host, pkg) != null
                || needsPin(host, pkg)
                || Config.getFlaggedPackages(host).contains(pkg);
    }

    /**
     * Start {@code intent} unless the app that would handle it is gated (then its gate is shown). When
     * the system would ask which app to use, the choice is offered without the gated apps.
     *
     * @throws android.content.ActivityNotFoundException when nothing can handle it
     */
    static void start(Activity host, Intent intent) {
        ResolveInfo handler = host.getPackageManager().resolveActivity(intent, PackageManager.MATCH_DEFAULT_ONLY);
        if (handler != null && handler.activityInfo != null) {
            String pkg = handler.activityInfo.packageName;
            if ("android".equals(pkg)) {
                // no default: the system resolver would list every handler, gated ones too
                host.startActivity(chooser(host, intent, null));
                return;
            }
            if (!pkg.equals(host.getPackageName())
                    && gate(host, pkg, host.getPackageManager().getApplicationLabel(handler.activityInfo.applicationInfo))) {
                return;
            }
        }
        host.startActivity(intent);
    }

    /** A chooser for {@code intent} that leaves out the apps that are currently gated. */
    static Intent chooser(Activity host, Intent intent, CharSequence title) {
        Intent chooser = Intent.createChooser(intent, title);
        List<ComponentName> excluded = excludedComponents(host, intent);
        if (!excluded.isEmpty()) {
            chooser.putExtra(Intent.EXTRA_EXCLUDE_COMPONENTS, excluded.toArray(new ComponentName[0]));
        }
        return chooser;
    }

    /** Components of every gated app that could handle {@code intent}. */
    static List<ComponentName> excludedComponents(Activity host, Intent intent) {
        List<ComponentName> out = new ArrayList<>();
        for (ResolveInfo info : host.getPackageManager().queryIntentActivities(intent, PackageManager.MATCH_DEFAULT_ONLY)) {
            if (info.activityInfo == null) continue;
            String pkg = info.activityInfo.packageName;
            if (!pkg.equals(host.getPackageName()) && isGated(host, pkg)) {
                out.add(new ComponentName(pkg, info.activityInfo.name));
            }
        }
        return out;
    }
}
