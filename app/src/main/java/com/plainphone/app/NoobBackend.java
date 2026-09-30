package com.plainphone.app;

import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.Looper;

/**
 * Noob-mode gate backend: no accessibility overlay. To gate an app it sends the user Home
 * and puts one of Plain's own gate activities up on top of the launcher. Works because Plain
 * is the Home app and may start activities from the background.
 */
final class NoobBackend implements GateEngine.Backend {

    private final Context ctx;
    private final Handler handler = new Handler(Looper.getMainLooper());

    NoobBackend(Context context) {
        this.ctx = context.getApplicationContext();
    }

    @Override
    public void goHome() {
        goHome(ctx);
    }

    static void goHome(Context context) {
        DebugLog.i("goHome by " + DebugLog.caller());   // DEBUG
        Intent home = new Intent(Intent.ACTION_MAIN)
                .addCategory(Intent.CATEGORY_HOME)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        context.startActivity(home);
    }

    @Override
    public void showPin(String packageName, boolean overApp) {
        Intent gate = new Intent(ctx, PinGateActivity.class);
        gate.putExtra("package", packageName);
        gate.putExtra("label", AllAppsUsage.label(ctx.getPackageManager(), packageName));
        gate.putExtra("over", overApp);
        gate.putExtra("engine", true);
        gate.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        ctx.startActivity(gate);
    }

    @Override
    public void showTimeBlock(String packageName, boolean overApp) {
        TimeBlock block = TimeBlockRules.getBlockingBlock(ctx, packageName);
        Intent gate = new Intent(ctx, TimeBlockGateActivity.class);
        gate.putExtra("package", packageName);
        gate.putExtra("blockId", block != null ? block.id : null);
        gate.putExtra("over", overApp);
        gate.putExtra("engine", true);
        gate.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        ctx.startActivity(gate);
    }

    /** The gate is an activity of its own, so there is nothing to take down. */
    @Override
    public void removeGateUi() {
    }
}
