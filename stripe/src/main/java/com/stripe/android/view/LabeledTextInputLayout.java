package com.stripe.android.view;

import android.content.Context;
import android.graphics.Rect;
import android.support.design.widget.TextInputLayout;
import android.util.AttributeSet;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public class LabeledTextInputLayout extends TextInputLayout {
    private Object collapsingTextHelper;
    private Rect bounds;
    private Method recalculateMethod;

    public LabeledTextInputLayout(Context context) {
        this(context, null);
    }

    public LabeledTextInputLayout(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public LabeledTextInputLayout(Context context, AttributeSet attrs, int defStyleAttr) {
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
            Field cthField = TextInputLayout.class.getDeclaredField("mCollapsingTextHelper");
            cthField.setAccessible(true);
            collapsingTextHelper = cthField.get(this);

            Field boundsField = collapsingTextHelper.getClass().getDeclaredField("mCollapsedBounds");
            boundsField.setAccessible(true);
            bounds = (Rect) boundsField.get(collapsingTextHelper);

            recalculateMethod = collapsingTextHelper.getClass().getDeclaredMethod("recalculate");
        } catch (NoSuchFieldException noSuchField) {
            collapsingTextHelper = null;
            bounds = null;
            recalculateMethod = null;
            noSuchField.printStackTrace();
        } catch (IllegalAccessException illegalAccess) {
            collapsingTextHelper = null;
            bounds = null;
            recalculateMethod = null;
            illegalAccess.printStackTrace();
        } catch (NoSuchMethodException noSuchMethod) {
            collapsingTextHelper = null;
            bounds = null;
            recalculateMethod = null;
            noSuchMethod.printStackTrace();
        }
    }

    private void adjustBounds() {
        if (collapsingTextHelper == null) {
            return;
        }

        try {
            bounds.left = getEditText().getLeft() + getEditText().getPaddingLeft();
            recalculateMethod.invoke(collapsingTextHelper);
        }
        catch (InvocationTargetException | IllegalAccessException | IllegalArgumentException e) {
            e.printStackTrace();
        }
    }
}
