package com.plainphone.app;

/** A home-art preview that {@link PhotoCropActivity} can pan and zoom. */
interface CropArt {
    float getFocusX();
    float getFocusY();
    float getZoom();
    void setZoom(float zoom);
    void panBy(float dx, float dy);
    /** Clockwise quarter turns, 0..3. */
    int getQuarterTurns();
    void setQuarterTurns(int turns);
}
