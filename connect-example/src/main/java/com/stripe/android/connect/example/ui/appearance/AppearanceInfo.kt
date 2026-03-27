@file:OptIn(com.stripe.android.connect.PreviewConnectSDK::class)

package com.stripe.android.connect.example.ui.appearance

import android.content.Context
import android.os.Parcelable
import androidx.annotation.ColorRes
import androidx.annotation.StringRes
import androidx.core.content.ContextCompat
import com.stripe.android.connect.appearance.Action
import com.stripe.android.connect.appearance.Appearance
import com.stripe.android.connect.appearance.Badge
import com.stripe.android.connect.appearance.BadgeDefaults
import com.stripe.android.connect.appearance.Button
import com.stripe.android.connect.appearance.ButtonDefaults
import com.stripe.android.connect.appearance.Colors
import com.stripe.android.connect.appearance.CornerRadius
import com.stripe.android.connect.appearance.Form
import com.stripe.android.connect.appearance.TextTransform
import com.stripe.android.connect.appearance.Typography
import com.stripe.android.connect.example.R
import kotlinx.parcelize.Parcelize

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
        CustomFont(R.string.custom_font),
        Retro(R.string.appearance_retro),
        Forest(R.string.appearance_forest),
        DarkMode(R.string.appearance_dark_mode),
        NewTokens(R.string.appearance_new_tokens)
    }

    companion object {
        fun getAppearance(
            appearanceId: AppearanceId,
            context: Context,
            overrides: NewTokenOverrides = NewTokenOverrides(),
        ): AppearanceInfo {
            val builder = builderFor(appearanceId, context)
            builder.applyNewTokenOverrides(overrides)
            return AppearanceInfo(appearanceId, builder.build())
        }

        private fun builderFor(id: AppearanceId, context: Context): Appearance.Builder {
            return when (id) {
                AppearanceId.Default -> defaultBuilder()
                AppearanceId.Ogre -> ogreBuilder(context)
                AppearanceId.HotDog -> hotDogBuilder(context)
                AppearanceId.OceanBreeze -> oceanBreezeBuilder(context)
                AppearanceId.Link -> linkBuilder(context)
                AppearanceId.Dynamic -> dynamicBuilder(context)
                AppearanceId.CustomFont -> customFontBuilder()
                AppearanceId.Retro -> retroBuilder(context)
                AppearanceId.Forest -> forestBuilder(context)
                AppearanceId.DarkMode -> darkModeBuilder(context)
                AppearanceId.NewTokens -> newTokensBuilder(context)
            }
        }

        private fun defaultBuilder() = Appearance.Builder()

        private fun ogreBuilder(context: Context) = Appearance.Builder()
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

        private fun hotDogBuilder(context: Context) = Appearance.Builder()
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

        private fun oceanBreezeBuilder(context: Context) = Appearance.Builder()
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

        private fun linkBuilder(context: Context) = Appearance.Builder()
            .colors(
                Colors.Builder()
                    .primary(context.getColorCompat(R.color.link_primary))
                    .background(context.getColorCompat(R.color.link_background))
                    .text(context.getColorCompat(R.color.link_text))
                    .secondaryText(context.getColorCompat(R.color.link_secondary_text))
                    .build()
            )
            .actionPrimaryText(
                Action.Builder()
                    .colorText(context.getColorCompat(R.color.link_action_primary_text))
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
            .spacingUnit(
                @Suppress("MagicNumber")
                11f
            )

        @Suppress("LongMethod")
        private fun dynamicBuilder(context: Context) = Appearance.Builder()
            .colors(
                Colors.Builder()
                    .primary(context.getColorCompat(R.color.dynamic_colors_primary))
                    .text(context.getColorCompat(R.color.dynamic_colors_text))
                    .background(context.getColorCompat(R.color.dynamic_colors_background))
                    .border(context.getColorCompat(R.color.dynamic_colors_border))
                    .secondaryText(context.getColorCompat(R.color.dynamic_colors_secondary_text))
                    .danger(context.getColorCompat(R.color.dynamic_colors_danger))
                    .offsetBackground(context.getColorCompat(R.color.dynamic_colors_offset_background))
                    .build()
            )
            .actionPrimaryText(
                Action.Builder()
                    .colorText(context.getColorCompat(R.color.dynamic_colors_action_primary_text))
                    .build()
            )
            .actionSecondaryText(
                Action.Builder()
                    .colorText(context.getColorCompat(R.color.dynamic_colors_action_secondary_text))
                    .build()
            )
            .form(
                Form.Builder()
                    .accent(context.getColorCompat(R.color.dynamic_colors_form_accent))
                    .highlightBorder(context.getColorCompat(R.color.dynamic_colors_form_highlight_border))
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

        private fun customFontBuilder() = Appearance.Builder()
            .typography(
                Typography.Builder()
                    .fontFamily("doto")
                    .fontSizeBase(
                        @Suppress("MagicNumber")
                        24f
                    )
                    .build()
            )

        @Suppress("LongMethod")
        private fun retroBuilder(context: Context) = Appearance.Builder()
            .colors(
                Colors.Builder()
                    .primary(context.getColorCompat(R.color.retro_primary))
                    .text(context.getColorCompat(R.color.retro_text))
                    .background(context.getColorCompat(R.color.retro_background))
                    .secondaryText(context.getColorCompat(R.color.retro_secondary_text))
                    .border(context.getColorCompat(R.color.retro_border))
                    .offsetBackground(context.getColorCompat(R.color.retro_offset_background))
                    .build()
            )
            .actionSecondaryText(
                Action.Builder()
                    .colorText(context.getColorCompat(R.color.retro_action_secondary_text))
                    .build()
            )
            .buttonSecondary(
                Button(
                    colorBackground = context.getColorCompat(R.color.retro_button_secondary_background),
                    colorText = context.getColorCompat(R.color.retro_button_secondary_text)
                )
            )
            .badgeNeutral(
                Badge(
                    colorBackground = context.getColorCompat(R.color.retro_badge_neutral_background),
                    colorBorder = context.getColorCompat(R.color.retro_badge_neutral_border),
                    colorText = context.getColorCompat(R.color.retro_badge_neutral_text)
                )
            )
            .badgeSuccess(
                Badge(
                    colorBackground = context.getColorCompat(R.color.retro_badge_success_background),
                    colorBorder = context.getColorCompat(R.color.retro_badge_success_border),
                    colorText = context.getColorCompat(R.color.retro_badge_success_text)
                )
            )
            .badgeWarning(
                Badge(
                    colorBackground = context.getColorCompat(R.color.retro_badge_warning_background),
                    colorBorder = context.getColorCompat(R.color.retro_badge_warning_border),
                    colorText = context.getColorCompat(R.color.retro_badge_warning_text)
                )
            )
            .badgeDanger(
                Badge(
                    colorBackground = context.getColorCompat(R.color.retro_badge_danger_background),
                    colorBorder = context.getColorCompat(R.color.retro_badge_danger_border),
                    colorText = context.getColorCompat(R.color.retro_badge_danger_text)
                )
            )
            .cornerRadius(
                @Suppress("MagicNumber")
                CornerRadius.Builder()
                    .base(0f)
                    .build()
            )
            .typography(
                Typography.Builder()
                    .fontFamily("courier")
                    .build()
            )

        @Suppress("LongMethod")
        private fun forestBuilder(context: Context) = Appearance.Builder()
            .colors(
                Colors.Builder()
                    .primary(context.getColorCompat(R.color.forest_primary))
                    .background(context.getColorCompat(R.color.forest_background))
                    .secondaryText(context.getColorCompat(R.color.forest_secondary_text))
                    .border(context.getColorCompat(R.color.forest_border))
                    .danger(context.getColorCompat(R.color.forest_danger))
                    .offsetBackground(context.getColorCompat(R.color.forest_offset_background))
                    .build()
            )
            .buttonSecondary(
                Button(
                    colorBackground = context.getColorCompat(R.color.forest_button_secondary_background),
                    colorText = context.getColorCompat(R.color.forest_button_secondary_text)
                )
            )
            .badgeNeutral(
                Badge(
                    colorBackground = context.getColorCompat(R.color.forest_badge_neutral_background),
                    colorBorder = context.getColorCompat(R.color.forest_badge_neutral_border)
                )
            )
            .badgeSuccess(
                Badge(
                    colorBackground = context.getColorCompat(R.color.forest_badge_success_background),
                    colorBorder = context.getColorCompat(R.color.forest_badge_success_border),
                    colorText = context.getColorCompat(R.color.forest_badge_success_text)
                )
            )
            .badgeWarning(
                Badge(
                    colorBackground = context.getColorCompat(R.color.forest_badge_warning_background),
                    colorBorder = context.getColorCompat(R.color.forest_badge_warning_border),
                    colorText = context.getColorCompat(R.color.forest_badge_warning_text)
                )
            )
            .badgeDanger(
                Badge(
                    colorBackground = context.getColorCompat(R.color.forest_badge_danger_background),
                    colorBorder = context.getColorCompat(R.color.forest_badge_danger_border),
                    colorText = context.getColorCompat(R.color.forest_badge_danger_text)
                )
            )
            .cornerRadius(
                @Suppress("MagicNumber")
                CornerRadius.Builder()
                    .base(24f)
                    .build()
            )

        @Suppress("LongMethod")
        private fun darkModeBuilder(context: Context) = Appearance.Builder()
            .colors(
                Colors.Builder()
                    .primary(context.getColorCompat(R.color.dark_mode_primary))
                    .text(context.getColorCompat(R.color.dark_mode_text))
                    .background(context.getColorCompat(R.color.dark_mode_background))
                    .secondaryText(context.getColorCompat(R.color.dark_mode_secondary_text))
                    .border(context.getColorCompat(R.color.dark_mode_border))
                    .danger(context.getColorCompat(R.color.dark_mode_danger))
                    .offsetBackground(context.getColorCompat(R.color.dark_mode_offset_background))
                    .build()
            )
            .actionSecondaryText(
                Action.Builder()
                    .colorText(context.getColorCompat(R.color.dark_mode_action_secondary_text))
                    .build()
            )
            .buttonSecondary(
                Button(
                    colorBackground = context.getColorCompat(R.color.dark_mode_button_secondary_background),
                    colorText = context.getColorCompat(R.color.dark_mode_button_secondary_text)
                )
            )
            .badgeNeutral(
                Badge(
                    colorBackground = context.getColorCompat(R.color.dark_mode_badge_neutral_background),
                    colorBorder = context.getColorCompat(R.color.dark_mode_badge_neutral_border),
                    colorText = context.getColorCompat(R.color.dark_mode_badge_neutral_text)
                )
            )
            .badgeSuccess(
                Badge(
                    colorBackground = context.getColorCompat(R.color.dark_mode_badge_success_background),
                    colorBorder = context.getColorCompat(R.color.dark_mode_badge_success_border),
                    colorText = context.getColorCompat(R.color.dark_mode_badge_success_text)
                )
            )
            .badgeWarning(
                Badge(
                    colorBackground = context.getColorCompat(R.color.dark_mode_badge_warning_background),
                    colorBorder = context.getColorCompat(R.color.dark_mode_badge_warning_border),
                    colorText = context.getColorCompat(R.color.dark_mode_badge_warning_text)
                )
            )
            .badgeDanger(
                Badge(
                    colorBackground = context.getColorCompat(R.color.dark_mode_badge_danger_background),
                    colorBorder = context.getColorCompat(R.color.dark_mode_badge_danger_border),
                    colorText = context.getColorCompat(R.color.dark_mode_badge_danger_text)
                )
            )

        @Suppress("LongMethod", "MagicNumber")
        private fun newTokensBuilder(context: Context) = Appearance.Builder()
            .colors(
                Colors.Builder()
                    .primary(context.getColorCompat(R.color.full_tokens_primary))
                    .background(context.getColorCompat(R.color.full_tokens_background))
                    .text(context.getColorCompat(R.color.full_tokens_text))
                    .secondaryText(context.getColorCompat(R.color.full_tokens_secondary_text))
                    .border(context.getColorCompat(R.color.full_tokens_border))
                    .danger(context.getColorCompat(R.color.full_tokens_danger))
                    .offsetBackground(context.getColorCompat(R.color.full_tokens_offset_background))
                    .build()
            )
            .buttonDefaults(
                ButtonDefaults(
                    paddingX = 20f,
                    paddingY = 20f,
                    labelTypography = Typography.Style(
                        fontSize = 15f,
                        fontWeight = 600,
                        textTransform = TextTransform.None,
                    ),
                )
            )
            .buttonPrimary(
                Button(
                    colorBackground = context.getColorCompat(R.color.full_tokens_button_primary_background),
                    colorBorder = context.getColorCompat(R.color.full_tokens_button_primary_border),
                    colorText = context.getColorCompat(R.color.full_tokens_button_primary_text),
                )
            )
            .buttonSecondary(
                Button(
                    colorBackground = context.getColorCompat(R.color.full_tokens_button_secondary_background),
                    colorBorder = context.getColorCompat(R.color.full_tokens_button_secondary_border),
                    colorText = context.getColorCompat(R.color.full_tokens_button_secondary_text),
                )
            )
            .buttonDanger(
                Button(
                    colorBackground = context.getColorCompat(R.color.full_tokens_button_danger_background),
                    colorBorder = context.getColorCompat(R.color.full_tokens_button_danger_border),
                    colorText = context.getColorCompat(R.color.full_tokens_button_danger_text),
                )
            )
            .actionPrimaryText(
                Action.Builder()
                    .colorText(context.getColorCompat(R.color.full_tokens_action_primary_text))
                    .textTransform(TextTransform.None)
                    .build()
            )
            .actionSecondaryText(
                Action.Builder()
                    .colorText(context.getColorCompat(R.color.full_tokens_action_secondary_text))
                    .textTransform(TextTransform.None)
                    .build()
            )
            .badgeDefaults(
                BadgeDefaults(
                    paddingX = 10f,
                    paddingY = 4f,
                    labelTypography = Typography.Style(
                        fontSize = 12f,
                        fontWeight = 200,
                        textTransform = TextTransform.None,
                    ),
                )
            )
            .badgeNeutral(
                Badge(
                    colorBackground = context.getColorCompat(R.color.full_tokens_badge_neutral_background),
                    colorBorder = context.getColorCompat(R.color.full_tokens_badge_neutral_border),
                    colorText = context.getColorCompat(R.color.full_tokens_badge_neutral_text),
                )
            )
            .badgeSuccess(
                Badge(
                    colorBackground = context.getColorCompat(R.color.full_tokens_badge_success_background),
                    colorBorder = context.getColorCompat(R.color.full_tokens_badge_success_border),
                    colorText = context.getColorCompat(R.color.full_tokens_badge_success_text),
                )
            )
            .badgeWarning(
                Badge(
                    colorBackground = context.getColorCompat(R.color.full_tokens_badge_warning_background),
                    colorBorder = context.getColorCompat(R.color.full_tokens_badge_warning_border),
                    colorText = context.getColorCompat(R.color.full_tokens_badge_warning_text),
                )
            )
            .badgeDanger(
                Badge(
                    colorBackground = context.getColorCompat(R.color.full_tokens_badge_danger_background),
                    colorBorder = context.getColorCompat(R.color.full_tokens_badge_danger_border),
                    colorText = context.getColorCompat(R.color.full_tokens_badge_danger_text),
                )
            )
            .form(
                Form.Builder()
                    .placeholderTextColor(context.getColorCompat(R.color.full_tokens_form_placeholder))
                    .inputFieldPaddingX(14f)
                    .inputFieldPaddingY(10f)
                    .build()
            )
            .tableRowPaddingY(20f)

        private fun Context.getColorCompat(@ColorRes colorRes: Int): Int =
            ContextCompat.getColor(this, colorRes)
    }
}
