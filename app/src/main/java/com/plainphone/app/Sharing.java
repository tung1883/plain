package com.plainphone.app;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

/** Send-to-app for exported files, shared by the home screen and workspace panels. */
final class Sharing {

    private Sharing() {}

    static void sendFiles(Activity a, List<Uri> uris, String mime, String chooser) {
        if (uris.isEmpty()) {
            Toast.makeText(a, "Nothing to export", Toast.LENGTH_SHORT).show();
            return;
        }
        try {
            ArrayList<Uri> list = new ArrayList<>(uris);
            Intent send = new Intent(list.size() == 1
                    ? Intent.ACTION_SEND : Intent.ACTION_SEND_MULTIPLE);
            send.setType(mime);
            if (list.size() == 1) send.putExtra(Intent.EXTRA_STREAM, list.get(0));
            else send.putParcelableArrayListExtra(Intent.EXTRA_STREAM, list);
            send.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            a.startActivity(Intent.createChooser(send, chooser));
        } catch (Exception e) {
            Toast.makeText(a, "Export failed", Toast.LENGTH_SHORT).show();
        }
    }
}
