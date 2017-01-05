package com.stripe.android.view;

import android.content.Context;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.widget.HorizontalScrollView;

/**
 * An extension of the {@link HorizontalScrollView} class that can be locked and
 * unlocked programmatically. Based on the solution in https://stackoverflow.com/a/5763815, and
 * a PR by <a href="https://github.com/simon-marino">Simon Kenny.</a>
 */
public class LockableHorizontalScrollView extends HorizontalScrollView {

    // True if and only if we can scroll the view -- it is in the not locked state.
    private boolean mScrollable;

    public LockableHorizontalScrollView(Context context) {
        super(context);
    }

    public LockableHorizontalScrollView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public LockableHorizontalScrollView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public boolean isScrollable() {
        return mScrollable;
    }

    public void setScrollable(boolean scrollable) {
        mScrollable = scrollable;
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (event.getAction() != MotionEvent.ACTION_DOWN) {
            return super.onTouchEvent(event);
        }

        // We only pass the event to the base handler if mScrollable is true
        return mScrollable && super.onTouchEvent(event);
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent event) {
        // Reject all intercept events if mScrollable is false.
        return mScrollable && super.onInterceptTouchEvent(event);
    }
}
