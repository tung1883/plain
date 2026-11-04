package com.plainphone.app;

import android.app.Activity;
import android.os.Bundle;

/**
 * Manual test harness for {@link PinPromptView}'s layout -- not portrait-locked, so rotating the
 * device exercises the landscape (side-by-side) arrangement without needing Nerd mode or a locked
 * app. No real PIN check: any 4+ digits "accept".
 */
public class PinPreviewActivity extends Activity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(new PinPromptView(this, new PinPromptView.Listener() {
            @Override
            public void onPin(String pin) {
            }

            @Override
            public void onCancel() {
                finish();
            }
        }));
    }
}
