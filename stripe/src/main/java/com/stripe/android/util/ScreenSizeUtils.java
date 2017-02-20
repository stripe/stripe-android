package com.stripe.android.util;

import android.content.Context;
import android.support.annotation.DimenRes;
import android.support.annotation.IntegerRes;
import android.support.annotation.NonNull;

import com.stripe.android.R;

/**
 * Utility class for checking on screen size issues.
 */
public class ScreenSizeUtils {

    /**
     * Method to determine whether or not there is any extra width available
     * on the screen for a control. Useful when determining the size of touch buffers.
     *
     * @param context {@link Context} used to determine screen size and resource values
     * @param resDimen a resource id for a dimension value that you'd like to check
     * @return the (possibly negative) difference
     * between the width of the screen and the input dimension
     */
    public static int getScreenWidthExtraPixels(
            @NonNull Context context,
            @DimenRes int resDimen) {

        int widthPixels = context.getResources().getDisplayMetrics().widthPixels;
        int minPixels = context.getResources().getDimensionPixelSize(R.dimen.card_widget_min_width);
        return widthPixels - minPixels;
    }
}
