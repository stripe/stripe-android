package com.stripe.android.connect.example.ui.appearance

import android.content.Context
import android.os.Parcelable
import androidx.annotation.ColorRes
import androidx.annotation.StringRes
import androidx.core.content.ContextCompat
import com.stripe.android.connect.PrivateBetaConnectSDK
import com.stripe.android.connect.appearance.Appearance
import com.stripe.android.connect.appearance.Badge
import com.stripe.android.connect.appearance.Button
import com.stripe.android.connect.appearance.Colors
import com.stripe.android.connect.appearance.CornerRadius
import com.stripe.android.connect.appearance.Typography
import com.stripe.android.connect.example.R
import kotlinx.parcelize.Parcelize

@OptIn(PrivateBetaConnectSDK::class)
@Parcelize
data class AppearanceInfo(
    val appearanceId: AppearanceId,
    val appearance: Appearance
) : Parcelable {

    enum class AppearanceId(@StringRes val displayNameRes: Int) {
        Default(R.string.appearance_default),
        Ogre(R.string.appearance_ogre),
        HotDog(R.string.appearance_hot_dog),
        OceanBreeze(R.string.appearance_ocean_breeze),
        Link(R.string.appearance_link),
        Dynamic(R.string.appearance_dynamic),
        CustomFont(R.string.custom_font)
    }

    companion object {
        fun getAppearance(appearanceId: AppearanceId, context: Context): AppearanceInfo {
            return when (appearanceId) {
                AppearanceId.Default -> defaultAppearance()
                AppearanceId.Ogre -> ogreAppearance(context)
                AppearanceId.HotDog -> hotDogAppearance(context)
                AppearanceId.OceanBreeze -> oceanBreezeAppearance(context)
                AppearanceId.Link -> linkAppearance(context)
                AppearanceId.Dynamic -> dynamicAppearance(context)
                AppearanceId.CustomFont -> customFont()
            }
        }

        private fun defaultAppearance() = AppearanceInfo(
            appearanceId = AppearanceId.Default,
            appearance = Appearance.Builder().build()
        )

        private fun ogreAppearance(context: Context) = AppearanceInfo(
            appearanceId = AppearanceId.Ogre,
            appearance = Appearance.Builder()
                .setColors(
                    Colors.Builder()
                        .setPrimary(context.getColorCompat(R.color.ogre_primary))
                        .setBackground(context.getColorCompat(R.color.ogre_background))
                        .setText(context.getColorCompat(R.color.ogre_text))
                        .build()
                )
                .setButtonPrimary(
                    Button(
                        colorBackground = context.getColorCompat(R.color.ogre_button_primary_background),
                        colorBorder = context.getColorCompat(R.color.ogre_button_primary_border),
                        colorText = context.getColorCompat(R.color.ogre_button_primary_text)
                    )
                )
                .setButtonSecondary(
                    Button(
                        colorBackground = context.getColorCompat(R.color.ogre_button_secondary_background),
                        colorText = context.getColorCompat(R.color.ogre_button_secondary_text)
                    )
                )
                .setBadgeNeutral(
                    Badge(
                        colorBackground = context.getColorCompat(R.color.ogre_badge_neutral_background),
                        colorText = context.getColorCompat(R.color.ogre_badge_neutral_text)
                    )
                )
                .build()
        )

        private fun hotDogAppearance(context: Context) = AppearanceInfo(
            appearanceId = AppearanceId.HotDog,
            appearance = Appearance.Builder()
                .setColors(
                    Colors.Builder()
                        .setPrimary(context.getColorCompat(R.color.hot_dog_primary))
                        .setBackground(context.getColorCompat(R.color.hot_dog_background))
                        .setText(context.getColorCompat(R.color.hot_dog_text))
                        .setSecondaryText(context.getColorCompat(R.color.hot_dog_secondary_text))
                        .setOffsetBackground(context.getColorCompat(R.color.hot_dog_offset_background))
                        .build()
                )
                .setButtonPrimary(
                    Button(
                        colorBackground = context.getColorCompat(R.color.hot_dog_button_primary_background),
                        colorBorder = context.getColorCompat(R.color.hot_dog_button_primary_border),
                        colorText = context.getColorCompat(R.color.hot_dog_button_primary_text)
                    )
                )
                .setButtonSecondary(
                    Button(
                        colorBackground = context.getColorCompat(R.color.hot_dog_button_secondary_background),
                        colorBorder = context.getColorCompat(R.color.hot_dog_button_secondary_border)
                    )
                )
                .setBadgeDanger(
                    Badge(colorText = context.getColorCompat(R.color.hot_dog_badge_danger_text))
                )
                .setBadgeWarning(
                    Badge(colorBackground = context.getColorCompat(R.color.hot_dog_badge_warning_background))
                )
                .setCornerRadius(
                    @Suppress("MagicNumber")
                    CornerRadius.Builder()
                        .setBase(0f)
                        .build()
                )
                .build()
        )

        private fun oceanBreezeAppearance(context: Context) = AppearanceInfo(
            appearanceId = AppearanceId.OceanBreeze,
            appearance = Appearance.Builder()
                .setColors(
                    Colors.Builder()
                        .setBackground(context.getColorCompat(R.color.ocean_breeze_background))
                        .setPrimary(context.getColorCompat(R.color.ocean_breeze_primary))
                        .setText(context.getColorCompat(R.color.ocean_breeze_text))
                        .build()
                )
                .setButtonSecondary(
                    Button(
                        colorText = context.getColorCompat(R.color.ocean_breeze_button_secondary_text),
                        colorBorder = context.getColorCompat(R.color.ocean_breeze_button_secondary_border)
                    )
                )
                .setBadgeSuccess(Badge(colorText = context.getColorCompat(R.color.ocean_breeze_badge_success_text)))
                .setBadgeNeutral(Badge(colorText = context.getColorCompat(R.color.ocean_breeze_badge_neutral_text)))
                .setCornerRadius(
                    @Suppress("MagicNumber")
                    CornerRadius.Builder()
                        .setBase(23f)
                        .build()
                )
                .build()
        )

        private fun linkAppearance(context: Context) = AppearanceInfo(
            appearanceId = AppearanceId.Link,
            appearance = Appearance.Builder()
                .setColors(
                    Colors.Builder()
                        .setPrimary(context.getColorCompat(R.color.link_primary))
                        .setBackground(context.getColorCompat(R.color.link_background))
                        .setText(context.getColorCompat(R.color.link_text))
                        .setSecondaryText(context.getColorCompat(R.color.link_secondary_text))
                        .setActionPrimaryText(context.getColorCompat(R.color.link_action_primary_text))
                        .build()
                )
                .setButtonPrimary(
                    Button(
                        colorBackground = context.getColorCompat(R.color.link_button_primary_background),
                        colorBorder = context.getColorCompat(R.color.link_button_primary_border),
                        colorText = context.getColorCompat(R.color.link_button_primary_text)
                    )
                )
                .setBadgeSuccess(
                    Badge(
                        colorBackground = context.getColorCompat(R.color.link_badge_success_background),
                        colorBorder = context.getColorCompat(R.color.link_badge_success_border),
                        colorText = context.getColorCompat(R.color.link_badge_success_text)
                    )
                )
                .setBadgeNeutral(
                    Badge(
                        colorBackground = context.getColorCompat(R.color.link_badge_neutral_background),
                        colorText = context.getColorCompat(R.color.link_badge_neutral_text)
                    )
                )
                .setCornerRadius(
                    @Suppress("MagicNumber")
                    CornerRadius.Builder()
                        .setBase(5f)
                        .build()
                )
                .build()
        )

        @Suppress("LongMethod")
        private fun dynamicAppearance(context: Context) = AppearanceInfo(
            appearanceId = AppearanceId.Dynamic,
            appearance = Appearance.Builder()
                .setColors(
                    Colors.Builder()
                        .setPrimary(context.getColorCompat(R.color.dynamic_colors_primary))
                        .setText(context.getColorCompat(R.color.dynamic_colors_text))
                        .setBackground(context.getColorCompat(R.color.dynamic_colors_background))
                        .setBorder(context.getColorCompat(R.color.dynamic_colors_border))
                        .setSecondaryText(context.getColorCompat(R.color.dynamic_colors_secondary_text))
                        .setActionPrimaryText(context.getColorCompat(R.color.dynamic_colors_action_primary_text))
                        .setActionSecondaryText(context.getColorCompat(R.color.dynamic_colors_action_secondary_text))
                        .setFormAccent(context.getColorCompat(R.color.dynamic_colors_form_accent))
                        .setFormHighlightBorder(context.getColorCompat(R.color.dynamic_colors_form_highlight_border))
                        .setDanger(context.getColorCompat(R.color.dynamic_colors_danger))
                        .setOffsetBackground(context.getColorCompat(R.color.dynamic_colors_offset_background))
                        .build()
                )
                .setButtonPrimary(
                    Button(
                        colorBackground = context.getColorCompat(R.color.dynamic_colors_button_primary_background),
                        colorBorder = context.getColorCompat(R.color.dynamic_colors_button_primary_border),
                        colorText = context.getColorCompat(R.color.dynamic_colors_button_primary_text)
                    )
                )
                .setButtonSecondary(
                    Button(
                        colorBackground = context.getColorCompat(R.color.dynamic_colors_button_secondary_background),
                        colorBorder = context.getColorCompat(R.color.dynamic_colors_button_secondary_border),
                        colorText = context.getColorCompat(R.color.dynamic_colors_button_secondary_text)
                    )
                )
                .setBadgeSuccess(
                    Badge(
                        colorBorder = context.getColorCompat(R.color.dynamic_colors_badge_success_border),
                        colorText = context.getColorCompat(R.color.dynamic_colors_badge_success_text)
                    )
                )
                .setBadgeNeutral(
                    Badge(
                        colorBorder = context.getColorCompat(R.color.dynamic_colors_badge_neutral_border),
                        colorText = context.getColorCompat(R.color.dynamic_colors_badge_neutral_text),
                    )
                )
                .setBadgeWarning(
                    Badge(
                        colorBorder = context.getColorCompat(R.color.dynamic_colors_badge_warning_border),
                        colorText = context.getColorCompat(R.color.dynamic_colors_badge_warning_text)
                    )
                )
                .setBadgeDanger(
                    Badge(
                        colorBorder = context.getColorCompat(R.color.dynamic_colors_badge_danger_border),
                        colorText = context.getColorCompat(R.color.dynamic_colors_badge_danger_text)
                    )
                )
                .build()
        )

        private fun customFont() = AppearanceInfo(
            appearanceId = AppearanceId.CustomFont,
            appearance = Appearance.Builder()
                .setTypography(
                    Typography.Builder()
                        .setFontFamily("doto")
                        .build()
                )
                .build()
        )

        private fun Context.getColorCompat(@ColorRes colorRes: Int): Int =
            ContextCompat.getColor(this, colorRes)
    }
}
