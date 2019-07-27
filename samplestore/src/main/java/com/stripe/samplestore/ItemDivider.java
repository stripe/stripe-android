package com.stripe.samplestore;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.view.View;

import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

class ItemDivider extends RecyclerView.ItemDecoration {

    @NonNull private final Drawable divider;

    /**
     * Custom divider will be used in the list.
     */
    ItemDivider(@NonNull Context context, @DrawableRes int resId) {
        divider = ContextCompat.getDrawable(context, resId);
    }

    @Override
    public void onDraw(@NonNull Canvas c, @NonNull RecyclerView parent,
                       @NonNull RecyclerView.State state) {
        final int start = parent.getPaddingStart();
        final int end = parent.getWidth() - parent.getPaddingEnd();

        int childCount = parent.getChildCount();
        for (int i = 0; i < childCount; i++) {
            View child = parent.getChildAt(i);

            RecyclerView.LayoutParams params = (RecyclerView.LayoutParams) child.getLayoutParams();

            int top = child.getBottom() + params.bottomMargin;
            int bottom = top + divider.getIntrinsicHeight();

            divider.setBounds(start, top, end, bottom);
            divider.draw(c);
        }
    }
}
