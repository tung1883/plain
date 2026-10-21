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

/**
 * The PlainPhone developer keyboard. Typing assists (suggestions, auto-correct, auto-capital) are
 * off unless a language or the spell-check setting asks for them; see {@link TypingController}.
 */
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
        if (root != null) {
            root.refresh();
            root.setField(composeAllowed(info), suggestAllowed(info));
        }
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

    /** Passwords and non-text fields get the keys exactly as typed (no Vietnamese / Korean composing). */
    private static boolean composeAllowed(EditorInfo info) {
        if (info == null) return false;
        int t = info.inputType;
        if ((t & android.text.InputType.TYPE_MASK_CLASS) != android.text.InputType.TYPE_CLASS_TEXT) return false;
        int v = t & android.text.InputType.TYPE_MASK_VARIATION;
        return v != android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD
                && v != android.text.InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
                && v != android.text.InputType.TYPE_TEXT_VARIATION_WEB_PASSWORD;
    }

    /** Suggestions and auto-correct also stay out of URL and e-mail fields and fields that ask for none. */
    private static boolean suggestAllowed(EditorInfo info) {
        if (!composeAllowed(info)) return false;
        int t = info.inputType;
        int v = t & android.text.InputType.TYPE_MASK_VARIATION;
        return v != android.text.InputType.TYPE_TEXT_VARIATION_URI
                && v != android.text.InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS
                && v != android.text.InputType.TYPE_TEXT_VARIATION_WEB_EMAIL_ADDRESS
                && (t & android.text.InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS) == 0;
    }

    @Override
    public void onUpdateSelection(int oldSelStart, int oldSelEnd, int newSelStart, int newSelEnd,
                                  int candidatesStart, int candidatesEnd) {
        super.onUpdateSelection(oldSelStart, oldSelEnd, newSelStart, newSelEnd, candidatesStart, candidatesEnd);
        if (root != null) root.selectionChanged(newSelStart, newSelEnd, candidatesStart, candidatesEnd);
    }

    @Override
    public void onFinishInputView(boolean finishingInput) {
        super.onFinishInputView(finishingInput);
        if (root != null) root.release();
    }

    // Back first closes whatever is open inside the keyboard (a viewer, select mode, a sheet, the
    // camera); only when nothing is, it hides the keyboard as usual.
    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK && root != null && root.canGoBack()) {
            event.startTracking();
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }

    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK && event.isTracking() && !event.isCanceled()
                && root != null && root.canGoBack()) {
            root.goBack();
            return true;
        }
        return super.onKeyUp(keyCode, event);
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
    public void setComposing(String s) {
        InputConnection ic = getCurrentInputConnection();
        if (ic != null) ic.setComposingText(s, 1);
    }

    @Override
    public void finishComposing() {
        InputConnection ic = getCurrentInputConnection();
        if (ic != null) ic.finishComposingText();
    }

    @Override
    public String textBefore(int n) {
        InputConnection ic = getCurrentInputConnection();
        if (ic == null) return "";
        CharSequence c = ic.getTextBeforeCursor(n, 0);
        return c == null ? "" : c.toString();
    }

    @Override
    public void deleteBefore(int n) {
        InputConnection ic = getCurrentInputConnection();
        if (ic != null) ic.deleteSurroundingText(n, 0);
    }

    @Override
    public int capsMode() {
        InputConnection ic = getCurrentInputConnection();
        EditorInfo info = getCurrentInputEditorInfo();
        return ic == null || info == null ? 0 : ic.getCursorCapsMode(info.inputType);
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
    public void requestCameraPermission() {
        startActivity(new android.content.Intent(this, CameraPermissionActivity.class)
                .addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK));
    }

    @Override
    public void addSticker(String collection) {
        startActivity(new android.content.Intent(this, AddStickerActivity.class)
                .putExtra(AddStickerActivity.EXTRA_COLLECTION, collection)
                .addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK));
    }

    @Override
    public void sendSticker(java.io.File image) {
        InputConnection ic = getCurrentInputConnection();
        if (ic == null) return;
        android.net.Uri uri = PlainFileProvider.uriFor("com.plainphone.app.files", image);
        boolean sent = false;
        String mime = StickerStore.isGifFile(image) ? "image/gif" : "image/png";
        try {
            android.view.inputmethod.InputContentInfo info = new android.view.inputmethod.InputContentInfo(
                    uri, new ClipDescription("image", new String[]{mime}));
            sent = ic.commitContent(info, InputConnection.INPUT_CONTENT_GRANT_READ_URI_PERMISSION, null);
        } catch (RuntimeException e) {
            android.util.Log.w("PlainKb", "commitContent failed", e);
        }
        java.util.List<String> accepted = fieldMimeTypes();
        android.util.Log.i("PlainKb", "sent " + mime + " to " + (getCurrentInputEditorInfo() == null ? "?"
                : getCurrentInputEditorInfo().packageName) + ": " + sent + "; the field lists " + accepted);
        if (sent) return;
        boolean declared = !accepted.isEmpty() && !mimeAccepted(accepted, mime);
        // this field does not take images: put it on the clipboard so it can be pasted where it can go
        if (clipboard != null) {
            lastImageUri = uri;
            clipboard.setPrimaryClip(ClipData.newUri(getContentResolver(), "image", uri));
            android.widget.Toast.makeText(this, declared
                            ? "This field does not take " + mime.substring(6).toUpperCase() + "s. Copied, paste it elsewhere."
                            : "This field cannot take images. Copied, paste it elsewhere.",
                    android.widget.Toast.LENGTH_SHORT).show();
        }
    }

    /** The content types the focused field says it can receive from a keyboard (may be empty). */
    private java.util.List<String> fieldMimeTypes() {
        java.util.List<String> out = new java.util.ArrayList<>();
        android.view.inputmethod.EditorInfo info = getCurrentInputEditorInfo();
        if (info == null || info.extras == null) return out;
        for (String key : new String[]{"androidx.core.view.inputmethod.EditorInfoCompat.CONTENT_MIME_TYPES",
                "android.support.v13.view.inputmethod.EditorInfoCompat.CONTENT_MIME_TYPES"}) {
            String[] types = info.extras.getStringArray(key);
            if (types != null) out.addAll(java.util.Arrays.asList(types));
        }
        return out;
    }

    private static boolean mimeAccepted(java.util.List<String> accepted, String mime) {
        for (String a : accepted) {
            if (a.equals(mime) || a.equals("*/*") || (a.endsWith("/*") && mime.startsWith(a.substring(0, a.length() - 1)))) {
                return true;
            }
        }
        return false;
    }

    @Override
    public void copyText(String text) {
        if (clipboard == null) return;
        clipboard.setPrimaryClip(ClipData.newPlainText("text", text));
        android.widget.Toast.makeText(this, "Copied", android.widget.Toast.LENGTH_SHORT).show();
    }

    @Override
    public void copyImage(java.io.File image) {
        if (clipboard == null) return;
        android.net.Uri uri = PlainFileProvider.uriFor("com.plainphone.app.files", image);
        lastImageUri = uri;   // already in the history; do not record it again as a new image
        clipboard.setPrimaryClip(ClipData.newUri(getContentResolver(), "image", uri));
        android.widget.Toast.makeText(this, "Copied", android.widget.Toast.LENGTH_SHORT).show();
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
