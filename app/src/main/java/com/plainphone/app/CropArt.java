package com.plainphone.app;

/** A home-art preview that {@link PhotoCropActivity} can pan and zoom. */
interface CropArt {
    float getFocusX();
    float getFocusY();
    float getZoom();
    void setZoom(float zoom);
    void panBy(float dx, float dy);
}
