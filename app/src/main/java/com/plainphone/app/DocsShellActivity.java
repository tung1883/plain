package com.plainphone.app;

import android.app.Activity;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.Toast;

import java.io.IOException;

/**
 * The docs terminal: the real {@code pman} (see {@link PtyProcess}) in a {@link ShellSurface}.
 * Doc packs are downloaded by the app ({@link PackStore}); pman only reads them.
 * Opened from Sandbox → Man with {@link TerminalActivity#EXTRA_TOPIC}.
 */
public class DocsShellActivity extends Activity {

    private ShellSurface surface;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        surface = new ShellSurface(this);
        surface.setCallbacks(new ShellSurface.Callbacks() {
            @Override public void onHostCommand(String cmd) {
                handleHostCommand(cmd);
            }

            @Override public void onExit(int code) {
                if (code == 127) {
                    Toast.makeText(DocsShellActivity.this, "Cannot start pman", Toast.LENGTH_SHORT).show();
                }
                finish();
            }
        });

        LinearLayout head = UiKit.header(this, "pman");
        head.addView(surface.keyboardButton(this), new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.MATCH_PARENT));
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(Color.BLACK);
        root.addView(head, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        View hair = new View(this);
        hair.setBackgroundColor(0xFF1C1C1C);
        root.addView(hair, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 1));
        root.addView(surface, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));
        setContentView(root);

        String topic = getIntent().getStringExtra(TerminalActivity.EXTRA_TOPIC);
        if (topic == null) {
            start(null);
            return;
        }
        // The pack for the topic is the app's job: fetch the list if needed, ask, download.
        new Thread(() -> {
            if (!PackStore.hasRegistry(this)) {
                try {
                    PackStore.refreshRegistry(this);
                } catch (IOException ignored) {
                    // offline: pman just reports that the topic is missing
                }
            }
            runOnUiThread(() -> {
                if (isFinishing()) return;
                PackUi.ensure(this, PackStore.docPackForTopic(this, topic), () -> start(topic), () -> start(null));
            });
        }, "docs-prepare").start();
    }

    /** pman asks the app for what it cannot do itself: download a pack, or insert text into the editor. */
    private void handleHostCommand(String cmd) {
        int sp = cmd.indexOf(' ');
        String verb = sp < 0 ? cmd : cmd.substring(0, sp);
        String arg = sp < 0 ? "" : cmd.substring(sp + 1).trim();
        if (verb.equals("insert")) {
            try {
                String text = new String(android.util.Base64.decode(arg, android.util.Base64.DEFAULT),
                        java.nio.charset.StandardCharsets.UTF_8);
                setResult(RESULT_OK, new android.content.Intent().putExtra(TerminalActivity.EXTRA_SNIPPET, text));
                finish();
            } catch (IllegalArgumentException e) {
                surface.replyLocal("err");
            }
        } else if (verb.equals("install")) {
            installFor(arg);
        } else {
            surface.replyLocal("err unknown request");
        }
    }

    /** Downloads the doc pack {@code id} (asking first) and tells pman how it went. */
    private void installFor(String id) {
        new Thread(() -> {
            if (PackStore.docPack(this, id) == null) {
                try {
                    PackStore.refreshRegistry(this);
                } catch (IOException ignored) {
                    // reported below
                }
            }
            runOnUiThread(() -> {
                PackStore.Pack p = PackStore.docPack(this, id);
                if (p == null) {
                    surface.replyLocal("err no pack " + id);
                    return;
                }
                if (PackStore.installed(this, p)) {
                    surface.replyLocal("ok");
                    return;
                }
                PackUi.ensure(this, p, () -> surface.replyLocal("ok"), () -> surface.replyLocal("err cancelled"));
            });
        }, "docs-install").start();
    }

    private void start(String topic) {
        surface.post(() -> {
            surface.startLocal(topic == null ? new String[]{"--repl"} : new String[]{"--repl", topic});
            surface.focusKeyboardSoon();
        });
    }

    @Override
    protected void onDestroy() {
        surface.closeLocal();
        super.onDestroy();
    }
}
