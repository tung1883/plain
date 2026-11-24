package com.plainphone.app;

import android.content.Context;
import android.graphics.Typeface;

class Fonts {

    private static Typeface georgia;
    private static Typeface ibmPlexMono;
    private static Typeface excalifont;
    private static Typeface cascadiaMono;

    static Typeface current(Context context) {
        return switch (Config.getFontChoice(context)) {
            case CASCADIA_MONO -> cascadiaMono(context);
            case IBM_PLEX_MONO -> ibmPlexMono(context);
            case GEORGIA -> georgia(context);
            case EXCALIFONT -> excalifont(context);
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

    static synchronized Typeface excalifont(Context context) {
        if (excalifont == null) {
            excalifont = Typeface.createFromAsset(context.getAssets(), "fonts/excalifont.ttf");
        }
        return excalifont;
    }

    private static final String[] CAPTION_FILES = {"baloo2", "pangolin", "lora", "nunito"};
    private static final Typeface[] caption = new Typeface[CAPTION_FILES.length];

    /**
     * Fonts for captions on images: 0 Round (Baloo 2), 1 Hand (Pangolin), 2 Serif (Lora), 3 Bold (Nunito).
     * All four are subset to Latin + the full Vietnamese alphabet, so tone marks sit on their letters
     * (the earlier Fredoka / Excalifont / Georgia files lack most Vietnamese letters).
     */
    static synchronized Typeface caption(Context context, int index) {
        int i = Math.max(0, Math.min(index, CAPTION_FILES.length - 1));
        if (caption[i] == null) {
            caption[i] = Typeface.createFromAsset(context.getAssets(), "fonts/" + CAPTION_FILES[i] + ".ttf");
        }
        return caption[i];
    }

    /** The variable-font weight to use for a caption font, or 0 when the font is not variable. */
    static int captionWeight(int index) {
        return index == 0 ? 700 : index == 2 ? 700 : index == 3 ? 900 : 0;
    }

    static synchronized Typeface cascadiaMono(Context context) {
        if (cascadiaMono == null) {
            cascadiaMono = Typeface.createFromAsset(context.getAssets(), "fonts/cascadiamono.ttf");
        }
        return cascadiaMono;
    }
}

