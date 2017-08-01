package com.stripe.android.view;

import android.content.Context;
import android.graphics.Rect;
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
class IconTextInputLayout extends TextInputLayout {
    private Object mCollapsingTextHelper;
    private Rect mBounds;
    private Method mRecalculateMethod;

    IconTextInputLayout(Context context) {
        this(context, null);
    }

    IconTextInputLayout(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    IconTextInputLayout(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        super.onLayout(changed, left, top, right, bottom);

        adjustBounds();
    }

    private void init() {
        try {
            Field textHeaderField = TextInputLayout.class.getDeclaredField("mCollapsingTextHelper");
            textHeaderField.setAccessible(true);
            mCollapsingTextHelper = textHeaderField.get(this);

            Field boundsField = mCollapsingTextHelper.getClass().getDeclaredField("mCollapsedBounds");
            boundsField.setAccessible(true);
            mBounds = (Rect) boundsField.get(mCollapsingTextHelper);

            mRecalculateMethod = mCollapsingTextHelper.getClass().getDeclaredMethod("recalculate");

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
        }
        catch (InvocationTargetException | IllegalAccessException | IllegalArgumentException e) {
            e.printStackTrace();
        }
    }
}
