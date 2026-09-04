package com.plainphone.app;

/** How often the home tip / quote strip rotates on its own. */
public class TipRotateActivity extends StepperActivity {

    @Override
    protected String title() {
        return "Rotate the tip / quote every";
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
        return 0;
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
        return new int[]{0, 1, 5, 15, 30};
    }

    @Override
    protected String chipText(int value) {
        return value == 0 ? "Off" : super.chipText(value);
    }

    @Override
    protected int currentValue() {
        return Config.getTipRotateMinutes(this);
    }

    @Override
    protected void save(int value) {
        Config.setTipRotateMinutes(this, value);
    }

    @Override
    protected String format(int value) {
        return value <= 0 ? "On tap only" : value + " min";
    }
}
