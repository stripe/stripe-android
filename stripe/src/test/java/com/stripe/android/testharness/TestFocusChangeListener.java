package com.stripe.android.testharness;

import android.support.annotation.IdRes;
import android.view.View;
import android.view.ViewTreeObserver;

/**
 * Test class that listens for global focus changes and stores the transition.
 */
public class TestFocusChangeListener implements ViewTreeObserver.OnGlobalFocusChangeListener {
    private View mOldFocus;
    private View mNewFocus;

    @Override
    public void onGlobalFocusChanged(View oldFocus, View newFocus) {
        mOldFocus = oldFocus;
        mNewFocus = newFocus;
    }

    @IdRes
    public int getOldFocusId() {
        return mOldFocus.getId();
    }

    @IdRes
    public int getNewFocusId() {
        return mNewFocus.getId();
    }

    public boolean hasOldFocus() {
        return mOldFocus != null;
    }

    public boolean hasNewFocus() {
        return mNewFocus != null;
    }
}
