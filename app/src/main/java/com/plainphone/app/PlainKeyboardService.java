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
        if (root != null) {
            root.setField(InputKinds.isSecure(info.inputType, info.imeOptions),
                    InputKinds.noLearning(info.imeOptions));
            root.refresh();
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
        String mime = StickerStore.isGifFile(image) ? "image/gif" : "image/png";
        android.net.Uri uri = PlainFileProvider.uriFor("com.plainphone.app.files", image);
        boolean sent = commitFile(image, mime);
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

    /** Hand a file to the focused field with commitContent; false if the field refused it. */
    private boolean commitFile(java.io.File file, String mime) {
        InputConnection ic = getCurrentInputConnection();
        if (ic == null) return false;
        try {
            boolean copied = file.getParentFile() != null
                    && file.getParentFile().equals(new java.io.File(getCacheDir(), PlainFileProvider.ATTACH_DIR));
            android.net.Uri uri = copied
                    ? PlainFileProvider.attachUri("com.plainphone.app.files", file.getName())
                    : PlainFileProvider.uriFor("com.plainphone.app.files", file);
            android.view.inputmethod.InputContentInfo info = new android.view.inputmethod.InputContentInfo(
                    uri, new ClipDescription(file.getName(), new String[]{mime}));
            return ic.commitContent(info, InputConnection.INPUT_CONTENT_GRANT_READ_URI_PERMISSION, null);
        } catch (RuntimeException e) {
            android.util.Log.w("PlainKb", "commitContent failed", e);
            return false;
        }
    }

    @Override
    public void attachFile(java.io.File file) {
        String ext = android.webkit.MimeTypeMap.getFileExtensionFromUrl(android.net.Uri.fromFile(file).toString());
        String mime = ext == null || ext.isEmpty() ? null
                : android.webkit.MimeTypeMap.getSingleton().getMimeTypeFromExtension(ext.toLowerCase());
        String type = mime == null ? "application/octet-stream" : mime;
        // Apps like Messenger accept any commit but treat it as a photo, so only offer what a field
        // declares, or else images and video; everything else falls back to the name.
        java.util.List<String> declared = fieldMimeTypes();
        boolean offer = declared.isEmpty() ? type.startsWith("image/") || type.startsWith("video/")
                : mimeAccepted(declared, type);
        java.io.File copy = offer ? shareCopy(file) : null;
        boolean sent = offer && commitFile(copy != null ? copy : file, type);
        android.util.Log.i("PlainKb", "attach " + type + " to " + (getCurrentInputEditorInfo() == null ? "?"
                : getCurrentInputEditorInfo().packageName) + ": " + sent + "; the field lists " + fieldMimeTypes());
        if (sent) return;
        if (!offer) {   // not something this field takes: the share sheet reaches apps that accept any file
            startActivity(KeyboardOpenActivity.forShare(this, file));
            return;
        }
        insertText(file.getName());
        android.widget.Toast.makeText(this, "This field does not take this file. Inserted the name.",
                android.widget.Toast.LENGTH_SHORT).show();
    }

    /** A copy under the app's cache with a plain name: some apps cannot fetch a file from its original path. */
    private java.io.File shareCopy(java.io.File file) {
        try {
            java.io.File dir = new java.io.File(getCacheDir(), PlainFileProvider.ATTACH_DIR);
            java.io.File[] old = dir.listFiles();
            if (old != null) for (java.io.File f : old) f.delete();
            if (!dir.isDirectory() && !dir.mkdirs()) return null;
            java.io.File out = new java.io.File(dir, file.getName().replaceAll("[^A-Za-z0-9._-]", "_"));
            try (java.io.InputStream in = new java.io.FileInputStream(file);
                 java.io.OutputStream os = new java.io.FileOutputStream(out)) {
                byte[] buf = new byte[65536];
                for (int n; (n = in.read(buf)) > 0; ) os.write(buf, 0, n);
            }
            return out;
        } catch (java.io.IOException e) {
            return null;
        }
    }

    @Override
    public void openHit(KbHit hit) {
        android.content.Intent go = KeyboardOpenActivity.forHit(this, hit);
        if (go != null) startActivity(go);
    }

    @Override
    public void requestAccess(String access) {
        startActivity(KeyboardOpenActivity.forAccess(this, access));
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
