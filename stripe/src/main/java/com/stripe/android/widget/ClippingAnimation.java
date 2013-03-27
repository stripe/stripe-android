package com.stripe.android.widget;

import android.view.animation.Animation;
import android.view.animation.Transformation;

public class ClippingAnimation extends Animation {
    private ClippingEditText view;

    private float fromXDelta;
    private float toXDelta;

    public ClippingAnimation(ClippingEditText view, float fromXDelta, float toXDelta) {
        this.view = view;
        this.fromXDelta = fromXDelta;
        this.toXDelta = toXDelta;
    }

    @Override
    protected void applyTransformation(float interpolatedTime, Transformation t) {
        float dx = fromXDelta;
        if (fromXDelta != toXDelta) {
            dx = fromXDelta + ((toXDelta - fromXDelta) * interpolatedTime);
        }
        view.setClipX((int) dx);
    }
}