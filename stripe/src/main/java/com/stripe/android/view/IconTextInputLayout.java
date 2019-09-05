package com.stripe.android.view;

import android.content.Context;
import android.graphics.Rect;
import android.util.AttributeSet;

import androidx.annotation.VisibleForTesting;

import com.google.android.material.textfield.TextInputLayout;
import com.stripe.android.utils.ClassUtils;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * This class uses Reflection to make the Support Library's floating hint text move above
 * a DrawableLeft, instead of just straight up beside it. If the Support Libraries ever
 * officially support this behavior, this class should be removed to avoid Reflection.
 */
public class IconTextInputLayout extends TextInputLayout {

    private static final Set<String> BOUNDS_FIELD_NAMES = new HashSet<>(
            Arrays.asList("mCollapsedBounds", "collapsedBounds")
    );
    private static final Set<String> TEXT_FIELD_NAMES = new HashSet<>(
            Arrays.asList("mCollapsingTextHelper", "collapsingTextHelper")
    );
    private static final Set<String> RECALCULATE_METHOD_NAMES =
            Collections.singleton("recalculate");

    @VisibleForTesting private final Object mCollapsingTextHelper;
    @VisibleForTesting private final Rect mBounds;
    @VisibleForTesting private final Method mRecalculateMethod;

    public IconTextInputLayout(Context context) {
        this(context, null);
    }

    public IconTextInputLayout(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public IconTextInputLayout(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);

        /*
         * Note: this method will break if we upgrade our version of the support library
         * and the variable and method names change. We should remove usage of reflection
         * at the first opportunity.
         */
        mCollapsingTextHelper = ClassUtils.getInternalObject(TextInputLayout.class,
                TEXT_FIELD_NAMES, this);
        if (mCollapsingTextHelper == null) {
            mBounds = null;
            mRecalculateMethod = null;
        } else {
            mBounds = (Rect) ClassUtils.getInternalObject(mCollapsingTextHelper.getClass(),
                    BOUNDS_FIELD_NAMES, mCollapsingTextHelper);
            mRecalculateMethod = ClassUtils.findMethod(mCollapsingTextHelper.getClass(),
                    RECALCULATE_METHOD_NAMES);
        }
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        super.onLayout(changed, left, top, right, bottom);
        adjustBounds();
    }

    private void adjustBounds() {
        if (mCollapsingTextHelper == null || getEditText() == null) {
            return;
        }

        try {
            mBounds.left = getEditText().getLeft() + getEditText().getPaddingStart();
            mRecalculateMethod.invoke(mCollapsingTextHelper);
        } catch (InvocationTargetException | IllegalAccessException | IllegalArgumentException e) {
            // No need to overreact here - this will result in the animation rendering differently
            e.printStackTrace();
        }
    }

    @VisibleForTesting
    boolean hasObtainedCollapsingTextHelper() {
        return mCollapsingTextHelper != null && mBounds != null && mRecalculateMethod != null;
    }

}
