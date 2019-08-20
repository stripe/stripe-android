package com.stripe.android.view;

import android.content.Context;
import android.support.annotation.ColorInt;
import android.support.annotation.ColorRes;
import android.support.annotation.NonNull;
import android.support.v4.content.ContextCompat;
import android.support.v4.graphics.ColorUtils;

import com.stripe.android.R;

import static com.stripe.android.view.ViewUtils.getThemeAccentColor;
import static com.stripe.android.view.ViewUtils.getThemeColorControlNormal;
import static com.stripe.android.view.ViewUtils.getThemeTextColorSecondary;

final class ThemeConfig {
    @NonNull
    final int[] textColorValues;

    @ColorInt
    private final int selectedTextAlphaColorInt;
    @ColorInt
    private final int selectedColorInt;
    @ColorInt
    private final int unselectedColorInt;
    @ColorInt
    private final int unselectedTextAlphaColorInt;
    @ColorInt
    private final int unselectedTextColorInt;

    ThemeConfig(@NonNull Context context) {
        selectedColorInt = determineColor(
                context,
                getThemeAccentColor(context).data,
                R.color.accent_color_default
        );
        unselectedColorInt = determineColor(
                context,
                getThemeColorControlNormal(context).data,
                R.color.control_normal_color_default
        );
        unselectedTextColorInt = determineColor(
                context,
                getThemeTextColorSecondary(context).data,
                R.color.color_text_secondary_default
        );

        selectedTextAlphaColorInt = ColorUtils.setAlphaComponent(selectedColorInt,
                context.getResources().getInteger(R.integer.light_text_alpha_hex));
        unselectedTextAlphaColorInt = ColorUtils.setAlphaComponent(unselectedTextColorInt,
                context.getResources().getInteger(R.integer.light_text_alpha_hex));

        textColorValues = new int[]{
                selectedColorInt,
                selectedTextAlphaColorInt,
                unselectedTextColorInt,
                unselectedTextAlphaColorInt
        };
    }

    @ColorInt
    int getTintColor(boolean isSelected) {
        return isSelected ? selectedColorInt : unselectedColorInt;
    }

    @ColorInt
    int getTextColor(boolean isSelected) {
        return isSelected ? selectedColorInt : unselectedTextColorInt;
    }

    @ColorInt
    int getTextAlphaColor(boolean isSelected) {
        return isSelected ? selectedTextAlphaColorInt : unselectedTextAlphaColorInt;
    }

    @ColorInt
    private int determineColor(@NonNull Context context,
                               @ColorInt int defaultColor,
                               @ColorRes int colorIfTransparent) {
        return ViewUtils.isColorTransparent(defaultColor) ?
                ContextCompat.getColor(context, colorIfTransparent) :
                defaultColor;
    }
}
