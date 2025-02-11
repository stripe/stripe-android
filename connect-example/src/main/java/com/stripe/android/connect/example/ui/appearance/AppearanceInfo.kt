package com.stripe.android.connect.example.ui.appearance

import android.content.Context
import android.os.Parcelable
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
            appearance = Appearance()
        )

        private fun ogreAppearance(context: Context) = AppearanceInfo(
            appearanceId = AppearanceId.Ogre,
            appearance = Appearance(
                colors = Colors(
                    primary = ContextCompat.getColor(context, R.color.ogre_primary),
                    background = ContextCompat.getColor(context, R.color.ogre_background),
                    text = ContextCompat.getColor(context, R.color.ogre_text)
                ),
                buttonPrimary = Button(
                    colorBackground = ContextCompat.getColor(context, R.color.ogre_button_primary_background),
                    colorBorder = ContextCompat.getColor(context, R.color.ogre_button_primary_border),
                    colorText = ContextCompat.getColor(context, R.color.ogre_button_primary_text)
                ),
                buttonSecondary = Button(
                    colorBackground = ContextCompat.getColor(context, R.color.ogre_button_secondary_background),
                    colorText = ContextCompat.getColor(context, R.color.ogre_button_secondary_text)
                ),
                badgeNeutral = Badge(
                    colorBackground = ContextCompat.getColor(context, R.color.ogre_badge_neutral_background),
                    colorText = ContextCompat.getColor(context, R.color.ogre_badge_neutral_text)
                )
            )
        )

        private fun hotDogAppearance(context: Context) = AppearanceInfo(
            appearanceId = AppearanceId.HotDog,
            appearance = Appearance(
                colors = Colors(
                    primary = ContextCompat.getColor(context, R.color.hot_dog_primary),
                    background = ContextCompat.getColor(context, R.color.hot_dog_background),
                    text = ContextCompat.getColor(context, R.color.hot_dog_text),
                    secondaryText = ContextCompat.getColor(context, R.color.hot_dog_secondary_text),
                    offsetBackground = ContextCompat.getColor(context, R.color.hot_dog_offset_background)
                ),
                buttonPrimary = Button(
                    colorBackground = ContextCompat.getColor(context, R.color.hot_dog_button_primary_background),
                    colorBorder = ContextCompat.getColor(context, R.color.hot_dog_button_primary_border),
                    colorText = ContextCompat.getColor(context, R.color.hot_dog_button_primary_text)
                ),
                buttonSecondary = Button(
                    colorBackground = ContextCompat.getColor(context, R.color.hot_dog_button_secondary_background),
                    colorBorder = ContextCompat.getColor(context, R.color.hot_dog_button_secondary_border)
                ),
                badgeDanger = Badge(
                    colorText = ContextCompat.getColor(context, R.color.hot_dog_badge_danger_text)
                ),
                badgeWarning = Badge(
                    colorBackground = ContextCompat.getColor(context, R.color.hot_dog_badge_warning_background)
                ),
                cornerRadius = CornerRadius(base = 0f)
            )
        )

        private fun oceanBreezeAppearance(context: Context) = AppearanceInfo(
            appearanceId = AppearanceId.OceanBreeze,
            appearance = Appearance(
                colors = Colors(
                    background = ContextCompat.getColor(context, R.color.ocean_breeze_background),
                    primary = ContextCompat.getColor(context, R.color.ocean_breeze_primary)
                ),
                buttonSecondary = Button(
                    colorText = ContextCompat.getColor(context, R.color.ocean_breeze_button_secondary_text),
                    colorBorder = ContextCompat.getColor(context, R.color.ocean_breeze_button_secondary_border)
                ),
                badgeSuccess = Badge(
                    colorText = ContextCompat.getColor(context, R.color.ocean_breeze_badge_success_text)
                ),
                badgeNeutral = Badge(
                    colorText = ContextCompat.getColor(context, R.color.ocean_breeze_badge_neutral_text)
                ),
                cornerRadius = CornerRadius(base = 23f)
            )
        )

        private fun linkAppearance(context: Context) = AppearanceInfo(
            appearanceId = AppearanceId.Link,
            appearance = Appearance(
                colors = Colors(
                    primary = ContextCompat.getColor(context, R.color.link_primary),
                    text = ContextCompat.getColor(context, R.color.link_text),
                    secondaryText = ContextCompat.getColor(context, R.color.link_secondary_text),
                    actionPrimaryText = ContextCompat.getColor(context, R.color.link_action_primary_text)
                ),
                buttonPrimary = Button(
                    colorBackground = ContextCompat.getColor(context, R.color.link_button_primary_background),
                    colorBorder = ContextCompat.getColor(context, R.color.link_button_primary_border),
                    colorText = ContextCompat.getColor(context, R.color.link_button_primary_text)
                ),
                badgeSuccess = Badge(
                    colorBackground = ContextCompat.getColor(context, R.color.link_badge_success_background),
                    colorBorder = ContextCompat.getColor(context, R.color.link_badge_success_border),
                    colorText = ContextCompat.getColor(context, R.color.link_badge_success_text)
                ),
                badgeNeutral = Badge(
                    colorBackground = ContextCompat.getColor(context, R.color.link_badge_neutral_background),
                    colorText = ContextCompat.getColor(context, R.color.link_badge_neutral_text)
                ),
                cornerRadius = CornerRadius(base = 5f)
            )
        )

        private fun dynamicAppearance(context: Context) = AppearanceInfo(
            appearanceId = AppearanceId.Dynamic,
            appearance = Appearance(
                colors = Colors(
                    primary = ContextCompat.getColor(context, R.color.dynamic_colors_primary),
                    text = ContextCompat.getColor(context, R.color.dynamic_colors_text),
                    background = ContextCompat.getColor(context, R.color.dynamic_colors_background),
                    border = ContextCompat.getColor(context, R.color.dynamic_colors_border),
                    secondaryText = ContextCompat.getColor(context, R.color.dynamic_colors_secondary_text),
                    actionPrimaryText = ContextCompat.getColor(context, R.color.dynamic_colors_action_primary_text),
                    actionSecondaryText = ContextCompat.getColor(context, R.color.dynamic_colors_action_secondary_text),
                    formAccent = ContextCompat.getColor(context, R.color.dynamic_colors_form_accent),
                    formHighlightBorder = ContextCompat.getColor(context, R.color.dynamic_colors_form_highlight_border),
                    danger = ContextCompat.getColor(context, R.color.dynamic_colors_danger),
                    offsetBackground = ContextCompat.getColor(context, R.color.dynamic_colors_offset_background),
                ),
                buttonPrimary = Button(
                    colorBackground = ContextCompat.getColor(context, R.color.dynamic_colors_button_primary_background),
                    colorBorder = ContextCompat.getColor(context, R.color.dynamic_colors_button_primary_border),
                    colorText = ContextCompat.getColor(context, R.color.dynamic_colors_button_primary_text)
                ),
                buttonSecondary = Button(
                    colorBackground = ContextCompat.getColor(
                        context,
                        R.color.dynamic_colors_button_secondary_background
                    ),
                    colorBorder = ContextCompat.getColor(context, R.color.dynamic_colors_button_secondary_border),
                    colorText = ContextCompat.getColor(context, R.color.dynamic_colors_button_secondary_text)
                ),
                badgeSuccess = Badge(
                    colorBorder = ContextCompat.getColor(context, R.color.dynamic_colors_badge_success_border),
                    colorText = ContextCompat.getColor(context, R.color.dynamic_colors_badge_success_text)
                ),
                badgeNeutral = Badge(
                    colorBorder = ContextCompat.getColor(context, R.color.dynamic_colors_badge_neutral_border),
                    colorText = ContextCompat.getColor(context, R.color.dynamic_colors_badge_neutral_text),
                ),
                badgeWarning = Badge(
                    colorBorder = ContextCompat.getColor(context, R.color.dynamic_colors_badge_warning_border),
                    colorText = ContextCompat.getColor(context, R.color.dynamic_colors_badge_warning_text)
                ),
                badgeDanger = Badge(
                    colorBorder = ContextCompat.getColor(context, R.color.dynamic_colors_badge_danger_border),
                    colorText = ContextCompat.getColor(context, R.color.dynamic_colors_badge_danger_text)
                ),
            )
        )

        private fun customFont() = AppearanceInfo(
            appearanceId = AppearanceId.Link,
            appearance = Appearance(
                typography = Typography(
                    fontFamily = "doto",
                )
            )
        )
    }
}
