package com.plainphone.app;

import android.app.Activity;
import android.app.Dialog;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.GradientDrawable;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

/**
 * The ask-first flow for Sandbox language packs: "needs a download" dialog, progress, failure with
 * Retry. Nothing is fetched until the user taps Download; retrying later is just asking again,
 * and a cancelled or failed download resumes from its partial file (see {@link PackStore}).
 */
final class PackUi {

    private PackUi() {}

    private static final Handler MAIN = new Handler(Looper.getMainLooper());

    /** Runs {@code onReady} now when {@code pack} is null or installed, else asks to download it first. */
    static void ensure(Activity a, PackStore.Pack pack, Runnable onReady) {
        ensure(a, pack, onReady, null);
    }

    static void ensure(Activity a, PackStore.Pack pack, Runnable onReady, Runnable onDeclined) {
        if (pack == null || PackStore.installed(a, pack)) {
            onReady.run();
            return;
        }
        PackStore.cleanStale(a);
        Dialog d = dialog(a);
        LinearLayout box = (LinearLayout) d.findViewById(android.R.id.content).getTag();
        box.addView(title(a, pack.label + (pack.docs ? " manuals need a download" : " needs a download")));
        box.addView(body(a, pack.docs
                ? pack.desc + ". Download it once to read it offline."
                : pack.id.equals("c")
                ? "Headers and the C reference are not built in. Download them once to compile and read about C."
                : "The JavaScript and TypeScript checker, runner and manuals are not built in. Download them once."));
        box.addView(meta(a, pack.id + "-pack · " + PackStore.mb(pack.zipBytes) + " · works offline after"));
        buttons(a, box, "Not now", () -> {
            d.dismiss();
            if (onDeclined != null) onDeclined.run();
        }, "Download", () -> {
            d.dismiss();
            download(a, pack, onReady, onDeclined);
        });
        d.show();
        size(a, d);
    }

    private static void download(Activity a, PackStore.Pack pack, Runnable onReady, Runnable onDeclined) {
        Dialog d = dialog(a);
        d.setCancelable(false);
        LinearLayout box = (LinearLayout) d.findViewById(android.R.id.content).getTag();
        TextView head = title(a, "Downloading " + pack.label);
        android.widget.ProgressBar bar = new android.widget.ProgressBar(a, null, android.R.attr.progressBarStyleHorizontal);
        bar.setMax(1000);
        TextView status = meta(a, "Starting…");
        box.addView(head);
        box.addView(bar, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(a, 12)));
        box.addView(status);
        AtomicBoolean cancel = new AtomicBoolean();
        buttons(a, box, "Cancel", () -> {
            cancel.set(true);
            d.dismiss();
        }, null, null);
        d.show();
        size(a, d);

        new Thread(() -> {
            long[] last = {0};
            try {
                PackStore.install(a, pack, (done, total, phase) -> {
                    long now = System.currentTimeMillis();
                    if (now - last[0] < 120) return;
                    last[0] = now;
                    MAIN.post(() -> {
                        if (total > 0) {
                            bar.setProgress((int) (done * 1000 / total));
                            status.setText(phase + "  " + PackStore.mb(done) + " of " + PackStore.mb(total));
                        } else {
                            bar.setProgress(1000);
                            status.setText(phase + "…");
                        }
                    });
                }, cancel);
                MAIN.post(() -> {
                    d.dismiss();
                    if (!a.isFinishing()) onReady.run();
                });
            } catch (PackStore.Cancelled e) {
                MAIN.post(() -> {
                    if (onDeclined != null) onDeclined.run();
                });
            } catch (IOException e) {
                MAIN.post(() -> {
                    d.dismiss();
                    if (!a.isFinishing()) failed(a, pack, e.getMessage(), onReady, onDeclined);
                });
            }
        }, "pack-install").start();
    }

    private static void failed(Activity a, PackStore.Pack pack, String why, Runnable onReady, Runnable onDeclined) {
        Dialog d = dialog(a);
        LinearLayout box = (LinearLayout) d.findViewById(android.R.id.content).getTag();
        box.addView(title(a, "Download failed"));
        TextView t = body(a, why == null ? "Something went wrong." : why);
        t.setTextColor(0xFFE57373);
        box.addView(t);
        buttons(a, box, "Close", () -> {
            d.dismiss();
            if (onDeclined != null) onDeclined.run();
        }, "Retry", () -> {
            d.dismiss();
            download(a, pack, onReady, onDeclined);
        });
        d.show();
        size(a, d);
    }

    /** Language chooser: installed ones switch at once, missing ones ask to download first. */
    static void pickLanguage(Activity a, SandboxRunner.Lang current, Consumer<SandboxRunner.Lang> onPicked) {
        Dialog d = dialog(a);
        LinearLayout box = (LinearLayout) d.findViewById(android.R.id.content).getTag();
        box.addView(title(a, "Language"));
        for (SandboxRunner.Lang l : SandboxRunner.Lang.values()) {
            PackStore.Pack p = PackStore.forLang(l);
            boolean have = PackStore.installed(a, p);
            LinearLayout row = new LinearLayout(a);
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setGravity(Gravity.CENTER_VERTICAL);
            row.setPadding(0, dp(a, 10), 0, dp(a, 10));
            TextView name = new TextView(a);
            name.setText(l.label);
            name.setTypeface(Fonts.current(a));
            name.setTextSize(16);
            name.setTextColor(l == current ? 0xFFEEEEEE : 0xFF9E9E9E);
            row.addView(name, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
            TextView state = meta(a, p == null ? "built in" : have ? "installed"
                    : "download " + PackStore.mb(p.zipBytes));
            state.setPadding(0, 0, 0, 0);
            row.addView(state);
            row.setOnClickListener(v -> {
                d.dismiss();
                if (l == current) return;
                ensure(a, p, () -> onPicked.accept(l));
            });
            box.addView(row);
        }
        d.show();
        size(a, d);
    }

    // ---- views ----------------------------------------------------------------------------------

    /** A rounded dark dialog; its content box is stored as the content view's tag. */
    private static Dialog dialog(Activity a) {
        Dialog d = new Dialog(a);
        d.requestWindowFeature(Window.FEATURE_NO_TITLE);
        LinearLayout box = new LinearLayout(a);
        box.setOrientation(LinearLayout.VERTICAL);
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(0xFF1C1C1C);
        bg.setStroke(dp(a, 1), 0xFF333333);
        bg.setCornerRadius(dp(a, 18));
        box.setBackground(bg);
        box.setPadding(dp(a, 18), dp(a, 18), dp(a, 18), dp(a, 8));
        d.setContentView(box);
        d.findViewById(android.R.id.content).setTag(box);
        if (d.getWindow() != null) d.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        return d;
    }

    private static void size(Activity a, Dialog d) {
        if (d.getWindow() == null) return;
        int w = (int) (a.getResources().getDisplayMetrics().widthPixels * 0.88f);
        d.getWindow().setLayout(w, ViewGroup.LayoutParams.WRAP_CONTENT);
    }

    private static TextView title(Activity a, String s) {
        TextView t = new TextView(a);
        t.setText(s);
        t.setTypeface(Fonts.current(a));
        t.setTextSize(18);
        t.setTextColor(0xFFEEEEEE);
        t.setPadding(0, 0, 0, dp(a, 8));
        return t;
    }

    private static TextView body(Activity a, String s) {
        TextView t = new TextView(a);
        t.setText(s);
        t.setTypeface(Fonts.current(a));
        t.setTextSize(14);
        t.setTextColor(0xFFB5B5B5);
        t.setLineSpacing(0, 1.15f);
        t.setPadding(0, 0, 0, dp(a, 12));
        return t;
    }

    private static TextView meta(Activity a, String s) {
        TextView t = new TextView(a);
        t.setText(s);
        t.setTypeface(Typeface.MONOSPACE);
        t.setTextSize(12);
        t.setTextColor(0xFF9E9E9E);
        t.setPadding(0, dp(a, 4), 0, dp(a, 12));
        return t;
    }

    private static void buttons(Activity a, LinearLayout box, String left, Runnable onLeft, String right, Runnable onRight) {
        LinearLayout row = new LinearLayout(a);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.END);
        row.addView(button(a, left, 0xFF9E9E9E, onLeft));
        if (right != null) row.addView(button(a, right, 0xFFEEEEEE, onRight));
        box.addView(row);
    }

    private static TextView button(Activity a, String label, int color, Runnable action) {
        TextView b = new TextView(a);
        b.setText(label);
        b.setTypeface(Fonts.current(a));
        b.setTextSize(15);
        b.setTextColor(color);
        b.setPadding(dp(a, 14), dp(a, 12), dp(a, 14), dp(a, 12));
        b.setOnClickListener((View v) -> action.run());
        return b;
    }

    private static int dp(Activity a, int v) {
        return (int) (v * a.getResources().getDisplayMetrics().density);
    }
}
