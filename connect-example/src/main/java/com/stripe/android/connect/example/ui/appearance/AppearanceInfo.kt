package com.stripe.android.connect.example.ui.appearance

import android.content.Context
import androidx.annotation.StringRes
import android.os.Parcelable
import com.stripe.android.connect.EmbeddedComponentManager.Appearance
import com.stripe.android.connect.EmbeddedComponentManager.Badge
import com.stripe.android.connect.EmbeddedComponentManager.Button
import com.stripe.android.connect.EmbeddedComponentManager.Colors
import com.stripe.android.connect.EmbeddedComponentManager.CornerRadius
import com.stripe.android.connect.PrivateBetaConnectSDK
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
    }

    companion object {
        fun getAppearance(appearanceId: AppearanceId, context: Context) {
            when (appearanceId) {
                AppearanceId.Default -> defaultAppearance()
                AppearanceId.Ogre -> ogreAppearance(context)
                AppearanceId.HotDog -> hotDogAppearance(context)
                AppearanceId.OceanBreeze -> oceanBreezeAppearance(context)
                AppearanceId.Link -> linkAppearance(context)
            }
        }

        private fun defaultAppearance() = AppearanceInfo(
            appearanceId = AppearanceId.Default,
            appearance = Appearance()
        )

        private fun ogreAppearance(context: Context) = AppearanceInfo(
            appearanceId = AppearanceId.Ogre,
            appearance = Appearance(
                colorsLight = Colors(
                    primary = context.getColor(R.color.ogre_primary),
                    background = context.getColor(R.color.ogre_background),
                    text = context.getColor(R.color.ogre_text)
                ),
                buttonPrimaryLight = Button(
                    colorBackground = context.getColor(R.color.ogre_button_primary_background),
                    colorBorder = context.getColor(R.color.ogre_button_primary_border),
                    colorText = context.getColor(R.color.ogre_button_primary_text)
                ),
                buttonSecondaryLight = Button(
                    colorBackground = context.getColor(R.color.ogre_button_secondary_background),
                    colorText = context.getColor(R.color.ogre_button_secondary_text)
                ),
                badgeNeutralLight = Badge(
                    colorBackground = context.getColor(R.color.ogre_badge_neutral_background),
                    colorText = context.getColor(R.color.ogre_badge_neutral_text)
                )
            )
        )

        private fun hotDogAppearance(context: Context) = AppearanceInfo(
            appearanceId = AppearanceId.HotDog,
            appearance = Appearance(
                colorsLight = Colors(
                    primary = context.getColor(R.color.hot_dog_primary),
                    background = context.getColor(R.color.hot_dog_background),
                    text = context.getColor(R.color.hot_dog_text),
                    secondaryText = context.getColor(R.color.hot_dog_secondary_text),
                    offsetBackground = context.getColor(R.color.hot_dog_offset_background)
                ),
                buttonPrimaryLight = Button(
                    colorBackground = context.getColor(R.color.hot_dog_button_primary_background),
                    colorBorder = context.getColor(R.color.hot_dog_button_primary_border),
                    colorText = context.getColor(R.color.hot_dog_button_primary_text)
                ),
                buttonSecondaryLight = Button(
                    colorBackground = context.getColor(R.color.hot_dog_button_secondary_background),
                    colorBorder = context.getColor(R.color.hot_dog_button_secondary_border)
                ),
                badgeDangerLight = Badge(
                    colorText = context.getColor(R.color.hot_dog_badge_danger_text)
                ),
                badgeWarningLight = Badge(
                    colorBackground = context.getColor(R.color.hot_dog_badge_warning_background)
                ),
                cornerRadius = CornerRadius(base = 0f)
            )
        )

        private fun oceanBreezeAppearance(context: Context) = AppearanceInfo(
            appearanceId = AppearanceId.OceanBreeze,
            appearance = Appearance(
                colorsLight = Colors(
                    background = context.getColor(R.color.ocean_breeze_background),
                    primary = context.getColor(R.color.ocean_breeze_primary)
                ),
                buttonSecondaryLight = Button(
                    colorText = context.getColor(R.color.ocean_breeze_button_secondary_text),
                    colorBorder = context.getColor(R.color.ocean_breeze_button_secondary_border)
                ),
                badgeSuccessLight = Badge(
                    colorText = context.getColor(R.color.ocean_breeze_badge_success_text)
                ),
                badgeNeutralLight = Badge(
                    colorText = context.getColor(R.color.ocean_breeze_badge_neutral_text)
                ),
                cornerRadius = CornerRadius(base = 23f)
            )
        )

        private fun linkAppearance(context: Context) = AppearanceInfo(
            appearanceId = AppearanceId.Link,
            appearance = Appearance(
                colorsLight = Colors(
                    primary = context.getColor(R.color.link_primary),
                    text = context.getColor(R.color.link_text),
                    secondaryText = context.getColor(R.color.link_secondary_text),
                    actionPrimaryText = context.getColor(R.color.link_action_primary_text)
                ),
                buttonPrimaryLight = Button(
                    colorBackground = context.getColor(R.color.link_button_primary_background),
                    colorBorder = context.getColor(R.color.link_button_primary_border),
                    colorText = context.getColor(R.color.link_button_primary_text)
                ),
                badgeSuccessLight = Badge(
                    colorBackground = context.getColor(R.color.link_badge_success_background),
                    colorBorder = context.getColor(R.color.link_badge_success_border),
                    colorText = context.getColor(R.color.link_badge_success_text)
                ),
                badgeNeutralLight = Badge(
                    colorBackground = context.getColor(R.color.link_badge_neutral_background),
                    colorText = context.getColor(R.color.link_badge_neutral_text)
                ),
                cornerRadius = CornerRadius(base = 5f)
            )
        )
    }
}
