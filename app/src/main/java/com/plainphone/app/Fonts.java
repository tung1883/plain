package com.plainphone.app;

import android.content.Context;
import android.graphics.Typeface;

class Fonts {

    private static Typeface georgia;

    static synchronized Typeface georgia(Context context) {
        if (georgia == null) {
            georgia = Typeface.createFromAsset(context.getAssets(), "fonts/georgia.ttf");
        }
        return georgia;
    }
}
