package com.plainphone.app;

import android.content.Context;
import android.graphics.Typeface;

class Fonts {

    private static Typeface georgia;
    private static Typeface ibmPlexMono;

    static Typeface current(Context context) {
        return switch (Config.getFontChoice(context)) {
            case IBM_PLEX_MONO -> ibmPlexMono(context);
            case GEORGIA -> georgia(context);
        };
    }

    static synchronized Typeface georgia(Context context) {
        if (georgia == null) {
            georgia = Typeface.createFromAsset(context.getAssets(), "fonts/georgia.ttf");
        }
        return georgia;
    }

    static synchronized Typeface ibmPlexMono(Context context) {
        if (ibmPlexMono == null) {
            ibmPlexMono = Typeface.createFromAsset(context.getAssets(), "fonts/ibmplexmono.ttf");
        }
        return ibmPlexMono;
    }
}

