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
                .colors(
                    Colors.Builder()
                        .primary(context.getColorCompat(R.color.ogre_primary))
                        .background(context.getColorCompat(R.color.ogre_background))
                        .text(context.getColorCompat(R.color.ogre_text))
                        .build()
                )
                .buttonPrimary(
                    Button(
                        colorBackground = context.getColorCompat(R.color.ogre_button_primary_background),
                        colorBorder = context.getColorCompat(R.color.ogre_button_primary_border),
                        colorText = context.getColorCompat(R.color.ogre_button_primary_text)
                    )
                )
                .buttonSecondary(
                    Button(
                        colorBackground = context.getColorCompat(R.color.ogre_button_secondary_background),
                        colorText = context.getColorCompat(R.color.ogre_button_secondary_text)
                    )
                )
                .badgeNeutral(
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
                .colors(
                    Colors.Builder()
                        .primary(context.getColorCompat(R.color.hot_dog_primary))
                        .background(context.getColorCompat(R.color.hot_dog_background))
                        .text(context.getColorCompat(R.color.hot_dog_text))
                        .secondaryText(context.getColorCompat(R.color.hot_dog_secondary_text))
                        .offsetBackground(context.getColorCompat(R.color.hot_dog_offset_background))
                        .build()
                )
                .buttonPrimary(
                    Button(
                        colorBackground = context.getColorCompat(R.color.hot_dog_button_primary_background),
                        colorBorder = context.getColorCompat(R.color.hot_dog_button_primary_border),
                        colorText = context.getColorCompat(R.color.hot_dog_button_primary_text)
                    )
                )
                .buttonSecondary(
                    Button(
                        colorBackground = context.getColorCompat(R.color.hot_dog_button_secondary_background),
                        colorBorder = context.getColorCompat(R.color.hot_dog_button_secondary_border)
                    )
                )
                .badgeDanger(
                    Badge(colorText = context.getColorCompat(R.color.hot_dog_badge_danger_text))
                )
                .badgeWarning(
                    Badge(colorBackground = context.getColorCompat(R.color.hot_dog_badge_warning_background))
                )
                .cornerRadius(
                    @Suppress("MagicNumber")
                    CornerRadius.Builder()
                        .base(0f)
                        .build()
                )
                .build()
        )

        private fun oceanBreezeAppearance(context: Context) = AppearanceInfo(
            appearanceId = AppearanceId.OceanBreeze,
            appearance = Appearance.Builder()
                .colors(
                    Colors.Builder()
                        .background(context.getColorCompat(R.color.ocean_breeze_background))
                        .primary(context.getColorCompat(R.color.ocean_breeze_primary))
                        .text(context.getColorCompat(R.color.ocean_breeze_text))
                        .build()
                )
                .buttonSecondary(
                    Button(
                        colorText = context.getColorCompat(R.color.ocean_breeze_button_secondary_text),
                        colorBorder = context.getColorCompat(R.color.ocean_breeze_button_secondary_border)
                    )
                )
                .badgeSuccess(Badge(colorText = context.getColorCompat(R.color.ocean_breeze_badge_success_text)))
                .badgeNeutral(Badge(colorText = context.getColorCompat(R.color.ocean_breeze_badge_neutral_text)))
                .cornerRadius(
                    @Suppress("MagicNumber")
                    CornerRadius.Builder()
                        .base(23f)
                        .build()
                )
                .build()
        )

        private fun linkAppearance(context: Context) = AppearanceInfo(
            appearanceId = AppearanceId.Link,
            appearance = Appearance.Builder()
                .colors(
                    Colors.Builder()
                        .primary(context.getColorCompat(R.color.link_primary))
                        .background(context.getColorCompat(R.color.link_background))
                        .text(context.getColorCompat(R.color.link_text))
                        .secondaryText(context.getColorCompat(R.color.link_secondary_text))
                        .actionPrimaryText(context.getColorCompat(R.color.link_action_primary_text))
                        .build()
                )
                .buttonPrimary(
                    Button(
                        colorBackground = context.getColorCompat(R.color.link_button_primary_background),
                        colorBorder = context.getColorCompat(R.color.link_button_primary_border),
                        colorText = context.getColorCompat(R.color.link_button_primary_text)
                    )
                )
                .badgeSuccess(
                    Badge(
                        colorBackground = context.getColorCompat(R.color.link_badge_success_background),
                        colorBorder = context.getColorCompat(R.color.link_badge_success_border),
                        colorText = context.getColorCompat(R.color.link_badge_success_text)
                    )
                )
                .badgeNeutral(
                    Badge(
                        colorBackground = context.getColorCompat(R.color.link_badge_neutral_background),
                        colorText = context.getColorCompat(R.color.link_badge_neutral_text)
                    )
                )
                .cornerRadius(
                    @Suppress("MagicNumber")
                    CornerRadius.Builder()
                        .base(5f)
                        .build()
                )
                .spacingUnit(9f)
                .build()
        )

        @Suppress("LongMethod")
        private fun dynamicAppearance(context: Context) = AppearanceInfo(
            appearanceId = AppearanceId.Dynamic,
            appearance = Appearance.Builder()
                .colors(
                    Colors.Builder()
                        .primary(context.getColorCompat(R.color.dynamic_colors_primary))
                        .text(context.getColorCompat(R.color.dynamic_colors_text))
                        .background(context.getColorCompat(R.color.dynamic_colors_background))
                        .border(context.getColorCompat(R.color.dynamic_colors_border))
                        .secondaryText(context.getColorCompat(R.color.dynamic_colors_secondary_text))
                        .actionPrimaryText(context.getColorCompat(R.color.dynamic_colors_action_primary_text))
                        .actionSecondaryText(context.getColorCompat(R.color.dynamic_colors_action_secondary_text))
                        .formAccent(context.getColorCompat(R.color.dynamic_colors_form_accent))
                        .formHighlightBorder(context.getColorCompat(R.color.dynamic_colors_form_highlight_border))
                        .danger(context.getColorCompat(R.color.dynamic_colors_danger))
                        .offsetBackground(context.getColorCompat(R.color.dynamic_colors_offset_background))
                        .build()
                )
                .buttonPrimary(
                    Button(
                        colorBackground = context.getColorCompat(R.color.dynamic_colors_button_primary_background),
                        colorBorder = context.getColorCompat(R.color.dynamic_colors_button_primary_border),
                        colorText = context.getColorCompat(R.color.dynamic_colors_button_primary_text)
                    )
                )
                .buttonSecondary(
                    Button(
                        colorBackground = context.getColorCompat(R.color.dynamic_colors_button_secondary_background),
                        colorBorder = context.getColorCompat(R.color.dynamic_colors_button_secondary_border),
                        colorText = context.getColorCompat(R.color.dynamic_colors_button_secondary_text)
                    )
                )
                .badgeSuccess(
                    Badge(
                        colorBorder = context.getColorCompat(R.color.dynamic_colors_badge_success_border),
                        colorText = context.getColorCompat(R.color.dynamic_colors_badge_success_text)
                    )
                )
                .badgeNeutral(
                    Badge(
                        colorBorder = context.getColorCompat(R.color.dynamic_colors_badge_neutral_border),
                        colorText = context.getColorCompat(R.color.dynamic_colors_badge_neutral_text),
                    )
                )
                .badgeWarning(
                    Badge(
                        colorBorder = context.getColorCompat(R.color.dynamic_colors_badge_warning_border),
                        colorText = context.getColorCompat(R.color.dynamic_colors_badge_warning_text)
                    )
                )
                .badgeDanger(
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
                .typography(
                    Typography.Builder()
                        .fontFamily("doto")
                        .build()
                )
                .build()
        )

        private fun Context.getColorCompat(@ColorRes colorRes: Int): Int =
            ContextCompat.getColor(this, colorRes)
    }
}
