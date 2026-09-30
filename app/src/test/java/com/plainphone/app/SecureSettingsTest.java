package com.plainphone.app;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class SecureSettingsTest {

    private static final String PKG = "com.plainphone.app";
    private static final String CLS = "com.plainphone.app.AppMonitorService";

    @Test
    public void removesOnlyOurEntry() {
        String list = "com.other/.Svc:" + PKG + "/" + CLS + ":com.third/.Svc";
        assertEquals("com.other/.Svc:com.third/.Svc", SecureSettings.removeComponent(list, PKG, CLS));
    }

    @Test
    public void removesShortSpellingToo() {
        assertEquals("", SecureSettings.removeComponent(PKG + "/.AppMonitorService", PKG, CLS));
    }

    @Test
    public void removeHandlesNullAndMissing() {
        assertEquals("", SecureSettings.removeComponent(null, PKG, CLS));
        assertEquals("com.other/.Svc", SecureSettings.removeComponent("com.other/.Svc", PKG, CLS));
    }

    @Test
    public void addAppendsOnceAndKeepsOthers() {
        assertEquals(PKG + "/" + CLS, SecureSettings.addComponent(null, PKG, CLS));
        assertEquals("com.other/.Svc:" + PKG + "/" + CLS,
                SecureSettings.addComponent("com.other/.Svc", PKG, CLS));
        String already = "com.other/.Svc:" + PKG + "/" + CLS;
        assertEquals(already, SecureSettings.addComponent(already, PKG, CLS));
    }
}
