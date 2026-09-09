package com.plainphone.app;

import android.app.Application;

/** Process entry point — wires up {@link Nav} so Plain reopens where you left off. */
public class PlainApp extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        Nav.install(this);
    }
}
