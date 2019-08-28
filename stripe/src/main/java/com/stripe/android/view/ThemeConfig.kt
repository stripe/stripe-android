package com.stripe.android.view

import android.content.Context
import android.support.annotation.ColorInt
import android.support.annotation.ColorRes
import android.support.v4.content.ContextCompat
import android.support.v4.graphics.ColorUtils
import com.stripe.android.R
import com.stripe.android.view.ViewUtils.getThemeAccentColor
import com.stripe.android.view.ViewUtils.getThemeColorControlNormal
import com.stripe.android.view.ViewUtils.getThemeTextColorSecondary

internal class ThemeConfig(context: Context) {
    @ColorInt
    private val selectedColorInt = determineColor(
        context,
        getThemeAccentColor(context).data,
        R.color.accent_color_default
    )

    @ColorInt
    private val unselectedColorInt = determineColor(
        context,
        getThemeColorControlNormal(context).data,
        R.color.control_normal_color_default
    )

    @ColorInt
    private val unselectedTextColorInt = determineColor(
        context,
        getThemeTextColorSecondary(context).data,
        R.color.color_text_secondary_default
    )

    @ColorInt
    private val selectedTextAlphaColorInt = ColorUtils.setAlphaComponent(
        selectedColorInt,
        context.resources.getInteger(R.integer.light_text_alpha_hex)
    )

    @ColorInt
    private val unselectedTextAlphaColorInt = ColorUtils.setAlphaComponent(
        unselectedTextColorInt,
        context.resources.getInteger(R.integer.light_text_alpha_hex)
    )

    val textColorValues = intArrayOf(
        selectedColorInt,
        selectedTextAlphaColorInt,
        unselectedTextColorInt,
        unselectedTextAlphaColorInt
    )

    @ColorInt
    fun getTintColor(isSelected: Boolean): Int {
        return if (isSelected) selectedColorInt else unselectedColorInt
    }

    @ColorInt
    fun getTextColor(isSelected: Boolean): Int {
        return if (isSelected) selectedColorInt else unselectedTextColorInt
    }

    @ColorInt
    fun getTextAlphaColor(isSelected: Boolean): Int {
        return if (isSelected) selectedTextAlphaColorInt else unselectedTextAlphaColorInt
    }

    @ColorInt
    private fun determineColor(
        context: Context,
        @ColorInt defaultColor: Int,
        @ColorRes colorIfTransparent: Int
    ): Int {
        return if (ViewUtils.isColorTransparent(defaultColor))
            ContextCompat.getColor(context, colorIfTransparent)
        else
            defaultColor
    }
}
