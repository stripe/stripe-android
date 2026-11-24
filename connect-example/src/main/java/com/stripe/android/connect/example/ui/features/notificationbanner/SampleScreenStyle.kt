package com.stripe.android.connect.example.ui.features.notificationbanner

import android.content.Context
import androidx.annotation.ColorInt
import androidx.annotation.ColorRes
import androidx.core.content.ContextCompat
import com.stripe.android.connect.example.R
import com.stripe.android.connect.example.ui.appearance.AppearanceInfo.AppearanceId

/**
 * The colors and corner radius the sample screen uses to match each appearance preset,
 * with the components' defaults for values a preset doesn't set.
 */
internal data class SampleScreenStyle(
    @ColorInt val background: Int,
    @ColorInt val text: Int,
    @ColorInt val secondaryText: Int,
    @ColorInt val border: Int,
    val cornerRadiusDp: Int,
) {
    companion object {
        @Suppress("CyclomaticComplexMethod", "MagicNumber")
        fun from(appearanceId: AppearanceId, context: Context): SampleScreenStyle {
            fun color(@ColorRes res: Int) = ContextCompat.getColor(context, res)
            val default = SampleScreenStyle(
                background = 0xFFFFFFFF.toInt(),
                text = 0xFF354052.toInt(),
                secondaryText = 0xFF6B7380.toInt(),
                border = 0xFFD6D9DE.toInt(),
                cornerRadiusDp = 6,
            )
            return when (appearanceId) {
                AppearanceId.Default, AppearanceId.CustomFont -> default
                AppearanceId.Ogre -> default.copy(
                    background = color(R.color.ogre_background),
                    text = color(R.color.ogre_text),
                )
                AppearanceId.HotDog -> default.copy(
                    background = color(R.color.hot_dog_background),
                    text = color(R.color.hot_dog_text),
                    secondaryText = color(R.color.hot_dog_secondary_text),
                    cornerRadiusDp = 0,
                )
                AppearanceId.OceanBreeze -> default.copy(
                    background = color(R.color.ocean_breeze_background),
                    text = color(R.color.ocean_breeze_text),
                    cornerRadiusDp = 23,
                )
                AppearanceId.Link -> default.copy(
                    background = color(R.color.link_background),
                    text = color(R.color.link_text),
                    secondaryText = color(R.color.link_secondary_text),
                    cornerRadiusDp = 5,
                )
                AppearanceId.Dynamic -> default.copy(
                    background = color(R.color.dynamic_colors_background),
                    text = color(R.color.dynamic_colors_text),
                    secondaryText = color(R.color.dynamic_colors_secondary_text),
                    border = color(R.color.dynamic_colors_border),
                )
                AppearanceId.Retro -> default.copy(
                    background = color(R.color.retro_background),
                    text = color(R.color.retro_text),
                    secondaryText = color(R.color.retro_secondary_text),
                    border = color(R.color.retro_border),
                    cornerRadiusDp = 0,
                )
                AppearanceId.Forest -> default.copy(
                    background = color(R.color.forest_background),
                    secondaryText = color(R.color.forest_secondary_text),
                    border = color(R.color.forest_border),
                    cornerRadiusDp = 24,
                )
                AppearanceId.DarkMode -> default.copy(
                    background = color(R.color.dark_mode_background),
                    text = color(R.color.dark_mode_text),
                    secondaryText = color(R.color.dark_mode_secondary_text),
                    border = color(R.color.dark_mode_border),
                )
            }
        }
    }
}
