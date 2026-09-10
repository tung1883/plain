package com.plainphone.app;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Color;
import android.view.Gravity;
import android.widget.LinearLayout;
import android.widget.TextView;

/** The "🔒 Locked — tap to unlock" cover a panel shows over a locked plugin. */
@SuppressLint("ViewConstructor")
final class LockGate extends LinearLayout {

    LockGate(Context ctx, String label, Runnable onUnlock) {
        super(ctx);
        setOrientation(VERTICAL);
        setGravity(Gravity.CENTER);
        setBackgroundColor(Color.BLACK);
        setOnClickListener(v -> onUnlock.run());

        TextView t = new TextView(ctx);
        t.setText(label);
        t.setTextColor(0xFF8B8B8B);
        t.setTextSize(14);
        t.setTypeface(Fonts.current(ctx));
        t.setGravity(Gravity.CENTER);
        addView(t);
    }
}
