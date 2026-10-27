package com.plainphone.app;

import android.text.InputType;
import android.view.inputmethod.EditorInfo;

/** Pure checks on what kind of field the keyboard is typing into. */
final class InputKinds {

    private InputKinds() {}

    /** True for password fields, where nothing may be searched, shown or remembered. */
    static boolean isSecure(int inputType, int imeFlags) {
        int cls = inputType & InputType.TYPE_MASK_CLASS;
        int variation = inputType & InputType.TYPE_MASK_VARIATION;
        if (cls == InputType.TYPE_CLASS_TEXT) {
            return variation == InputType.TYPE_TEXT_VARIATION_PASSWORD
                    || variation == InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
                    || variation == InputType.TYPE_TEXT_VARIATION_WEB_PASSWORD;
        }
        if (cls == InputType.TYPE_CLASS_NUMBER) {
            return variation == InputType.TYPE_NUMBER_VARIATION_PASSWORD;
        }
        return false;
    }

    /** True when the field asks the keyboard not to learn from what is typed. */
    static boolean noLearning(int imeFlags) {
        return (imeFlags & EditorInfo.IME_FLAG_NO_PERSONALIZED_LEARNING) != 0;
    }
}
