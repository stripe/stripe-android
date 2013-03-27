package com.stripe.android.widget;

import android.content.Context;
import android.text.Layout;
import android.util.AttributeSet;
import android.widget.EditText;

public class ClippingEditText extends EditText {
    private int clipX = 0;

    public ClippingEditText(Context context) {
        super(context);
    }

    public ClippingEditText(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public ClippingEditText(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    @Override
    public void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);

        Layout layout = getLayout();
        if (clipX != 0 && layout != null) {
            int width = (int) layout.getPrimaryHorizontal(getText().length());
            setMeasuredDimension(width - clipX, getMeasuredHeight());
        }
    }

    public void setClipX(int clipX) {
        if (this.clipX == clipX) {
            return;
        }
        if (this.clipX == 0 && clipX != 0) {
            // Put the cursor at the end so we show the last digits
            int n = getText().length();
            setSelection(n, n);
        }
        this.clipX = clipX;
        requestLayout();
    }
}