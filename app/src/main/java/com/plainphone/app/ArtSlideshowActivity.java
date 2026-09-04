package com.plainphone.app;

/** Interval between home-art slideshow images (minutes; 0 = manual). */
public class ArtSlideshowActivity extends StepperActivity {

    @Override
    protected String title() {
        return "Change art every";
    }

    @Override
    protected String stepLabel() {
        return "1 min";
    }

    @Override
    protected int step() {
        return 1;
    }

    @Override
    protected int min() {
        return 1;
    }

    @Override
    protected int max() {
        return 60;
    }

    @Override
    protected String unitLabel() {
        return "min";
    }

    @Override
    protected int[] chips() {
        return new int[]{1, 5, 10, 15, 30};
    }

    @Override
    protected int currentValue() {
        return Config.getArtSlideshowMinutes(this);
    }

    @Override
    protected void save(int value) {
        Config.setArtSlideshowMinutes(this, value);
    }

    @Override
    protected String format(int value) {
        return "Every " + value + " min";
    }
}
