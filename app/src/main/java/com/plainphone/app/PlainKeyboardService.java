package com.plainphone.app;

import android.content.ClipData;
import android.content.ClipDescription;
import android.content.ClipboardManager;
import android.inputmethodservice.InputMethodService;
import android.os.Build;
import android.os.SystemClock;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputConnection;
import android.view.inputmethod.InputMethodManager;

/** The PlainPhone developer keyboard: no suggestions, autocorrect or auto-capitalisation. */
public class PlainKeyboardService extends InputMethodService
        implements DevKeyboardView.Listener, KeyboardRoot.Host {

    private KeyboardRoot root;
    private ClipboardManager clipboard;
    private final ClipboardManager.OnPrimaryClipChangedListener clipListener = this::recordClip;

    @Override
    public void onCreate() {
        super.onCreate();
        clipboard = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
        if (clipboard != null) clipboard.addPrimaryClipChangedListener(clipListener);
    }

    @Override
    public void onDestroy() {
        if (clipboard != null) clipboard.removePrimaryClipChangedListener(clipListener);
        super.onDestroy();
    }

    @Override
    public View onCreateInputView() {
        root = new KeyboardRoot(this, this, this);
        root.refresh();
        return root;
    }

    @Override
    public void onStartInputView(EditorInfo info, boolean restarting) {
        super.onStartInputView(info, restarting);
        recordClip();   // a copy made while another keyboard was active
        if (root != null) root.refresh();
        if (StickerStore.justAdded && root != null) {
            StickerStore.justAdded = false;
            root.showImages();
        }
        String scanned = ScanTextActivity.pending;
        if (scanned != null && root != null) {
            ScanTextActivity.pending = null;
            root.showScanResult(scanned);   // shown for the user to insert, not typed on its own
        }
    }

    @Override
    public void onFinishInputView(boolean finishingInput) {
        super.onFinishInputView(finishingInput);
        if (root != null) root.release();
    }

    @Override
    public boolean onEvaluateFullscreenMode() {
        return false;
    }

    /** Add the current clipboard's text or image to the history, except what the source marked
     * sensitive. */
    private void recordClip() {
        if (clipboard == null) return;
        try {
            ClipData clip = clipboard.getPrimaryClip();
            if (clip == null || clip.getItemCount() == 0) return;
            ClipDescription d = clip.getDescription();
            if (d != null && d.getExtras() != null && Build.VERSION.SDK_INT >= 33
                    && d.getExtras().getBoolean(ClipDescription.EXTRA_IS_SENSITIVE)) return;
            android.net.Uri uri = clip.getItemAt(0).getUri();
            if (uri != null && d != null && d.hasMimeType("image/*")) {
                if (uri.equals(lastImageUri)) return;
                lastImageUri = uri;
                ClipboardHistory.addImage(this, uri);
                return;
            }
            ClipboardHistory.add(this, clip.getItemAt(0).coerceToText(this));
        } catch (SecurityException ignored) {
            // not allowed to read the clipboard right now
        }
    }

    /** The last image clip already recorded, so a redundant OnPrimaryClipChanged doesn't copy it
     * in twice. */
    private android.net.Uri lastImageUri;

    // ----------------------------------------------------------- DevKeyboardView.Listener

    @Override
    public void text(String s) {
        insertText(s);
    }

    @Override
    public void key(int keyCode, int meta) {
        InputConnection ic = getCurrentInputConnection();
        if (ic == null) return;
        long now = SystemClock.uptimeMillis();
        ic.sendKeyEvent(new KeyEvent(now, now, KeyEvent.ACTION_DOWN, keyCode, 0, meta));
        ic.sendKeyEvent(new KeyEvent(now, now, KeyEvent.ACTION_UP, keyCode, 0, meta));
    }

    @Override
    public void enter() {
        InputConnection ic = getCurrentInputConnection();
        EditorInfo info = getCurrentInputEditorInfo();
        if (ic == null) return;
        if (info != null && (info.imeOptions & EditorInfo.IME_FLAG_NO_ENTER_ACTION) == 0) {
            int action = info.imeOptions & EditorInfo.IME_MASK_ACTION;
            boolean multiLine = (info.inputType & android.text.InputType.TYPE_TEXT_FLAG_MULTI_LINE) != 0;
            if (!multiLine && action != EditorInfo.IME_ACTION_NONE && action != EditorInfo.IME_ACTION_UNSPECIFIED) {
                ic.performEditorAction(action);
                return;
            }
        }
        key(KeyEvent.KEYCODE_ENTER, 0);
    }

    @Override
    public void switchKeyboard() {
        InputMethodManager imm = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
        if (imm != null) imm.showInputMethodPicker();
    }

    // ------------------------------------------------------------------ KeyboardRoot.Host

    @Override
    public void insertText(String s) {
        InputConnection ic = getCurrentInputConnection();
        if (ic == null) return;
        String close = Config.isKbAutoPair(this) ? closer(s) : null;
        if (close == null) {
            ic.commitText(s, 1);
        } else {
            ic.commitText(s + close, 1);
            key(KeyEvent.KEYCODE_DPAD_LEFT, 0);   // back between the pair, in every kind of editor
        }
    }

    private static String closer(String open) {
        switch (open) {
            case "(": return ")";
            case "[": return "]";
            case "{": return "}";
            default: return null;
        }
    }

    @Override
    public String fieldText() {
        InputConnection ic = getCurrentInputConnection();
        if (ic == null) return "";
        CharSequence sel = ic.getSelectedText(0);
        if (sel != null && sel.length() > 0) return sel.toString();
        CharSequence before = ic.getTextBeforeCursor(4000, 0);
        CharSequence after = ic.getTextAfterCursor(4000, 0);
        return (before == null ? "" : before.toString()) + (after == null ? "" : after.toString());
    }

    @Override
    public void replaceFieldText(String s) {
        InputConnection ic = getCurrentInputConnection();
        if (ic == null) return;
        ic.beginBatchEdit();
        CharSequence sel = ic.getSelectedText(0);
        if (sel == null || sel.length() == 0) {
            CharSequence before = ic.getTextBeforeCursor(4000, 0);
            CharSequence after = ic.getTextAfterCursor(4000, 0);
            ic.deleteSurroundingText(before == null ? 0 : before.length(), after == null ? 0 : after.length());
        }
        ic.commitText(s, 1);   // over a selection this replaces it
        ic.endBatchEdit();
    }

    @Override
    public void sendKey(int keyCode) {
        key(keyCode, 0);
    }

    @Override
    public void scanText() {
        startActivity(new android.content.Intent(this, ScanTextActivity.class)
                .addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK));
    }

    @Override
    public void addSticker() {
        startActivity(new android.content.Intent(this, AddStickerActivity.class)
                .addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK));
    }

    @Override
    public void sendSticker(java.io.File image) {
        InputConnection ic = getCurrentInputConnection();
        if (ic == null) return;
        android.net.Uri uri = PlainFileProvider.uriFor("com.plainphone.app.files", image);
        boolean sent = false;
        try {
            android.view.inputmethod.InputContentInfo info = new android.view.inputmethod.InputContentInfo(
                    uri, new ClipDescription("image", new String[]{"image/png"}));
            sent = ic.commitContent(info, InputConnection.INPUT_CONTENT_GRANT_READ_URI_PERMISSION, null);
        } catch (RuntimeException ignored) {
        }
        if (sent) return;
        // this field does not take images: put it on the clipboard so it can be pasted where it can go
        if (clipboard != null) {
            clipboard.setPrimaryClip(ClipData.newUri(getContentResolver(), "image", uri));
            android.widget.Toast.makeText(this, "This field cannot take images. Copied, paste it elsewhere.",
                    android.widget.Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void settingsChanged() {
        if (root != null) root.applySettings();
    }

    @Override
    public void closePanel() {
        if (root != null) root.showPanel(-1);
    }
}
