package com.plainphone.app;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/** The things you can do with a search result besides tapping it (the long-press menu). */
final class ResultActions {

    private ResultActions() {}

    static final class Option {
        final String icon;       // a MiniIcons name, or null
        final String label;
        final String detail;     // dim text on the right (a phone number ...), or null
        final Runnable run;

        Option(String icon, String label, String detail, Runnable run) {
            this.icon = icon;
            this.label = label;
            this.detail = detail;
            this.run = run;
        }
    }

    private static void launch(Activity host, Intent intent) {
        try {
            GatedLaunch.start(host, intent);
        } catch (Exception e) {
            android.widget.Toast.makeText(host, "No app can do that", android.widget.Toast.LENGTH_SHORT).show();
        }
    }

    /** The menu for {@code r}; empty when it has none (then long-press keeps doing what it did). */
    static List<Option> forResult(Activity host, SearchResult r) {
        List<Option> out = new ArrayList<>();
        Object p = r.payload;

        if (p instanceof FileIndex.Entry) {
            FileIndex.Entry e = (FileIndex.Entry) p;
            out.add(new Option("open", "Open", null, () -> DeviceSearch.open(host, e.file, e.directory)));
            if (!e.directory) {
                out.add(new Option("apps", "Open with…", null, () -> DeviceSearch.openWithChooser(host, e.file)));
                out.add(new Option("share", "Share", null, () -> DeviceSearch.shareFile(host, e.file)));
                out.add(new Option("copy", "Copy name", null, () -> DeviceSearch.copyText(host, "File name", e.name)));
            }
            out.add(new Option("copy", "Copy path", null, () -> DeviceSearch.copyText(host, "Path", e.file.getAbsolutePath())));
            if (!e.directory) {
                out.add(new Option("folder", "Show in file manager", null, () -> DeviceSearch.revealInFileManager(host, e.file)));
            }
        } else if (p instanceof ContentHit) {
            ContentHit h = (ContentHit) p;
            File f = h.file;
            out.add(new Option("open", "Open", null, () -> DeviceSearch.open(host, f, false)));
            out.add(new Option("apps", "Open with…", null, () -> DeviceSearch.openWithChooser(host, f)));
            out.add(new Option("share", "Share", null, () -> DeviceSearch.shareFile(host, f)));
            out.add(new Option("copy", "Copy snippet", null, () -> DeviceSearch.copyText(host, "Snippet", h.snippet)));
            out.add(new Option("copy", "Copy path", null, () -> DeviceSearch.copyText(host, "Path", f.getAbsolutePath())));
            out.add(new Option("folder", "Show in file manager", null, () -> DeviceSearch.revealInFileManager(host, f)));
        } else if (p instanceof DeviceSearch.MediaFile) {
            DeviceSearch.MediaFile m = (DeviceSearch.MediaFile) p;
            out.add(new Option("open", "Open", null, r::activate));
            out.add(new Option("apps", "Open with…", null, () -> DeviceSearch.openWithChooser(host, m.uri, m.mime)));
            out.add(new Option("share", "Share", null, () -> DeviceSearch.shareUri(host, m.uri, m.mime)));
            out.add(new Option("copy", "Copy name", null, () -> DeviceSearch.copyText(host, "File name", r.title)));
        } else if (p instanceof DeviceSearch.Contact) {
            DeviceSearch.Contact c = (DeviceSearch.Contact) p;
            List<String> numbers = DeviceSearch.phoneNumbers(host, c.id);
            out.add(new Option("open", "Open contact", null, r::activate));
            for (String n : numbers) {
                out.add(new Option("call", "Call", n, () -> launch(host,
                        new Intent(Intent.ACTION_DIAL, Uri.parse("tel:" + Uri.encode(n))))));
                out.add(new Option("message", "Message", n, () -> launch(host,
                        new Intent(Intent.ACTION_SENDTO, Uri.parse("smsto:" + Uri.encode(n))))));
                out.add(new Option("copy", "Copy number", n,
                        () -> DeviceSearch.copyText(host, "Phone number", n)));
            }
        } else if (r.kind == SearchResult.Kind.WEB && p instanceof String) {
            String url = (String) p;
            out.add(new Option("open", "Open", null, r::activate));
            out.add(new Option("link", "Copy link", null, () -> DeviceSearch.copyText(host, "Link", url)));
        }
        return out;
    }
}
