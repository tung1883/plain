package com.plainphone.app;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class GateEngineTest {

    @Test
    public void noGateWhenNothingApplies() {
        assertNull(GateEngine.gateFor(false, false, false, false));
    }

    @Test
    public void timeBlockBeatsEverything() {
        assertEquals(GateEngine.GateKind.TIME_BLOCK, GateEngine.gateFor(true, true, false, true));
    }

    @Test
    public void lockedAppNeedsPinUntilRecentlyUnlocked() {
        assertEquals(GateEngine.GateKind.PIN, GateEngine.gateFor(false, true, false, false));
        assertNull(GateEngine.gateFor(false, true, true, false));
    }

    @Test
    public void flaggedAppGetsWait() {
        assertEquals(GateEngine.GateKind.COUNTDOWN, GateEngine.gateFor(false, false, false, true));
    }

    @Test
    public void lockedAndFlaggedPassesPinFirstThenWait() {
        assertEquals(GateEngine.GateKind.PIN, GateEngine.gateFor(false, true, false, true));
        assertEquals(GateEngine.GateKind.COUNTDOWN, GateEngine.gateFor(false, true, true, true));
    }
}
