package com.stripe.android.view

import android.content.Context
import androidx.annotation.ColorInt
import androidx.annotation.ColorRes
import androidx.core.content.ContextCompat
import androidx.core.graphics.ColorUtils
import com.stripe.android.R

internal class ThemeConfig(context: Context) {
    private val colorUtils = StripeColorUtils(context)

    @ColorInt
    private val selectedColorInt = determineColor(
        context,
        colorUtils.colorAccent,
        R.color.stripe_accent_color_default
    )

    @ColorInt
    private val unselectedColorInt = determineColor(
        context,
        colorUtils.colorControlNormal,
        R.color.stripe_control_normal_color_default
    )

    @ColorInt
    private val unselectedTextColorInt = determineColor(
        context,
        colorUtils.textColorSecondary,
        R.color.stripe_color_text_secondary_default
    )

    @ColorInt
    private val selectedTextAlphaColorInt = ColorUtils.setAlphaComponent(
        selectedColorInt,
        context.resources.getInteger(R.integer.stripe_light_text_alpha_hex)
    )

    @ColorInt
    private val unselectedTextAlphaColorInt = ColorUtils.setAlphaComponent(
        unselectedTextColorInt,
        context.resources.getInteger(R.integer.stripe_light_text_alpha_hex)
    )

    internal val textColorValues = intArrayOf(
        selectedColorInt,
        selectedTextAlphaColorInt,
        unselectedTextColorInt,
        unselectedTextAlphaColorInt
    )

    @ColorInt
    internal fun getTintColor(isSelected: Boolean): Int {
        return if (isSelected) selectedColorInt else unselectedColorInt
    }

    @ColorInt
    internal fun getTextColor(isSelected: Boolean): Int {
        return if (isSelected) selectedColorInt else unselectedTextColorInt
    }

    @ColorInt
    internal fun getTextAlphaColor(isSelected: Boolean): Int {
        return if (isSelected) selectedTextAlphaColorInt else unselectedTextAlphaColorInt
    }

    @ColorInt
    private fun determineColor(
        context: Context,
        @ColorInt defaultColor: Int,
        @ColorRes colorIfTransparent: Int
    ): Int {
        return if (StripeColorUtils.isColorTransparent(defaultColor)) {
            ContextCompat.getColor(context, colorIfTransparent)
        } else {
            defaultColor
        }
    }
}
