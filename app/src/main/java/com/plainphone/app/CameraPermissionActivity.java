package com.plainphone.app;

import android.Manifest;
import android.app.Activity;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.widget.Toast;

/**
 * A keyboard cannot ask for a permission itself, so the scan panel's Camera button opens this
 * invisible screen once to ask. Afterwards the panel opens the camera in place.
 */
public class CameraPermissionActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (savedInstanceState != null) return;
        requestPermissions(new String[]{Manifest.permission.CAMERA}, 1);
    }

    @Override
    public void onRequestPermissionsResult(int code, String[] permissions, int[] results) {
        super.onRequestPermissionsResult(code, permissions, results);
        boolean granted = results.length > 0 && results[0] == PackageManager.PERMISSION_GRANTED;
        Toast.makeText(this, granted ? "Camera allowed. Tap Camera again." : "Camera not allowed",
                Toast.LENGTH_SHORT).show();
        finish();
    }
}
