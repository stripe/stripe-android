package com.stripe.android.view;

import android.content.Context;
import android.graphics.Rect;
import android.support.annotation.VisibleForTesting;
import android.support.design.widget.TextInputLayout;
import android.util.AttributeSet;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * This class uses Reflection to make the Support Library's floating hint text move above
 * a DrawableLeft, instead of just straight up beside it. If the Support Libraries ever
 * officially support this behavior, this class should be removed to avoid Reflection.
 */
public class IconTextInputLayout extends TextInputLayout {

    private static final String BOUNDS_FIELD_NAME = "mCollapsedBounds";
    private static final String TEXT_FIELD_NAME = "mCollapsingTextHelper";
    private static final String RECALCULATE_METHOD_NAME = "recalculate";

    @VisibleForTesting Rect mBounds;
    @VisibleForTesting Object mCollapsingTextHelper;
    @VisibleForTesting Method mRecalculateMethod;

    public IconTextInputLayout(Context context) {
        this(context, null);
    }

    public IconTextInputLayout(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public IconTextInputLayout(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        super.onLayout(changed, left, top, right, bottom);
        adjustBounds();
    }

    /**
     * Note: this method will break if we upgrade our version of the support library
     * and the variable and method names change. We should remove usage of reflection
     * at the first opportunity.
     */
    @SuppressWarnings("checkstyle:IllegalCatch")
    @VisibleForTesting
    void init() {
        try {
            Field textHeaderField = TextInputLayout.class.getDeclaredField(TEXT_FIELD_NAME);
            textHeaderField.setAccessible(true);
            mCollapsingTextHelper = textHeaderField.get(this);

            Field boundsField = mCollapsingTextHelper
                    .getClass()
                    .getDeclaredField(BOUNDS_FIELD_NAME);
            boundsField.setAccessible(true);
            mBounds = (Rect) boundsField.get(mCollapsingTextHelper);

            mRecalculateMethod = mCollapsingTextHelper
                    .getClass()
                    .getDeclaredMethod(RECALCULATE_METHOD_NAME);

        } catch (Exception e) {
            mCollapsingTextHelper = null;
            mBounds = null;
            mRecalculateMethod = null;
            e.printStackTrace();
        }
    }

    private void adjustBounds() {
        if (mCollapsingTextHelper == null || getEditText() == null) {
            return;
        }

        try {
            mBounds.left = getEditText().getLeft() + getEditText().getPaddingLeft();
            mRecalculateMethod.invoke(mCollapsingTextHelper);
        } catch (InvocationTargetException | IllegalAccessException | IllegalArgumentException e) {
            // No need to overreact here - this will result in the animation rendering differently
            e.printStackTrace();
        }
    }
}
