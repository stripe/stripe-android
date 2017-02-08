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

    private LockableScrollChangedListener mLockableScrollChangedListener;
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

    /**
     * @return {@code true} if the view is scrollable, otherwise {@code false}.
     */
    public boolean isScrollable() {
        return mScrollable;
    }

    /**
     * Sets whether or not the control is scrollable.
     *
     * @param scrollable {@code true} if it should be scrollable, or {@code false} if not
     */
    public void setScrollable(boolean scrollable) {
        mScrollable = scrollable;
        setSmoothScrollingEnabled(scrollable);
    }

    /**
     * Without this override, keyboard and accessibility scrolling can still cause the "locked"
     * {@link HorizontalScrollView} to scroll.
     *
     * @param x the x position to scroll to
     * @param y the y position to scroll to
     */
    @Override
    public void scrollTo(int x, int y) {
        if (!mScrollable) {
            return;
        }
        super.scrollTo(x, y);
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

    void setScrollChangedListener(LockableScrollChangedListener listener) {
        mLockableScrollChangedListener = listener;
    }

    /**
     * Wrapping the {@link HorizontalScrollView#smoothScrollBy(int, int)} function to increase
     * testability.
     *
     * @param dx the number of pixels to scroll by on the X axis
     * @param dy the number of pixels to scroll by on the Y axis
     */
    void wrappedSmoothScrollBy(int dx, int dy) {
        if (!mScrollable) {
            return;
        }

        smoothScrollBy(dx, dy);
        if (mLockableScrollChangedListener != null) {
            mLockableScrollChangedListener.onSmoothScrollBy(dx, dy);
        }
    }

    /**
     * Allows you to listen to {@link HorizontalScrollView#smoothScrollBy(int, int)}
     * commands being sent to a {@link LockableHorizontalScrollView}. Useful for testing.
     */
    interface LockableScrollChangedListener {

        /**
         * Reacts to smoothScroll commands.
         *
         * @param dx the number of pixels to scroll by on the X axis
         * @param dy the number of pixels to scroll by on the Y axis
         */
        void onSmoothScrollBy(int dx, int dy);
    }
}
