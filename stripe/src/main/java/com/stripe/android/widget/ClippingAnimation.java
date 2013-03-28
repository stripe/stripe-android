package com.stripe.android.widget;

import android.view.animation.Animation;
import android.view.animation.Transformation;

public class ClippingAnimation extends Animation {
    private ClippingEditText mView;

    private float mFromXDelta;
    private float mToXDelta;

    public ClippingAnimation(ClippingEditText view, float fromXDelta, float toXDelta) {
        mView = view;
        mFromXDelta = fromXDelta;
        mToXDelta = toXDelta;
    }

    @Override
    protected void applyTransformation(float interpolatedTime, Transformation t) {
        float dx = mFromXDelta;
        if (mFromXDelta != mToXDelta) {
            dx = mFromXDelta + ((mToXDelta - mFromXDelta) * interpolatedTime);
        }
        mView.setClipX((int) dx);
    }
}