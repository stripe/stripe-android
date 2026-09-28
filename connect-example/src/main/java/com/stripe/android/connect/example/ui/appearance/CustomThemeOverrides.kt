package com.stripe.android.connect.example.ui.appearance

import androidx.annotation.ColorInt
import com.stripe.android.connect.appearance.Action
import com.stripe.android.connect.appearance.Appearance
import com.stripe.android.connect.appearance.BadgeDefaults
import com.stripe.android.connect.appearance.Button
import com.stripe.android.connect.appearance.ButtonDefaults
import com.stripe.android.connect.appearance.Form
import com.stripe.android.connect.appearance.TextTransform
import com.stripe.android.connect.appearance.Typography

data class CustomThemeOverrides(
    val buttonLabelTextTransform: TextTransform? = null,
    val buttonLabelFontWeight: Int? = null,
    val buttonLabelFontSize: Float? = null,
    val buttonPaddingY: Float? = null,
    val buttonPaddingX: Float? = null,
    @ColorInt val buttonDangerColorBackground: Int? = null,
    @ColorInt val buttonDangerColorBorder: Int? = null,
    @ColorInt val buttonDangerColorText: Int? = null,
    val badgeLabelTextTransform: TextTransform? = null,
    val badgeLabelFontWeight: Int? = null,
    val badgeLabelFontSize: Float? = null,
    val badgePaddingY: Float? = null,
    val badgePaddingX: Float? = null,
    val actionPrimaryTextTransform: TextTransform? = null,
    val actionSecondaryTextTransform: TextTransform? = null,
    @ColorInt val formPlaceholderTextColor: Int? = null,
    val inputFieldPaddingX: Float? = null,
    val inputFieldPaddingY: Float? = null,
    val tableRowPaddingY: Float? = null,
    val spacingUnit: Float? = null,
)

internal fun Appearance.Builder.applyCustomThemeOverrides(overrides: CustomThemeOverrides): Appearance.Builder {
    applyButtonDefaults(overrides)
    applyButtonDanger(overrides)
    applyBadgeDefaults(overrides)
    applyActions(overrides)
    applyForm(overrides)
    overrides.tableRowPaddingY?.let { tableRowPaddingY(it) }
    overrides.spacingUnit?.let { spacingUnit(it) }
    return this
}

private fun Appearance.Builder.applyButtonDefaults(overrides: CustomThemeOverrides) {
    val hasButtonLabel = overrides.buttonLabelTextTransform != null ||
        overrides.buttonLabelFontWeight != null ||
        overrides.buttonLabelFontSize != null
    val hasButtonPadding = overrides.buttonPaddingX != null || overrides.buttonPaddingY != null
    if (!hasButtonLabel && !hasButtonPadding) return
    val buttonDefaultsBuilder = ButtonDefaults.Builder()
        .paddingX(overrides.buttonPaddingX)
        .paddingY(overrides.buttonPaddingY)
    if (hasButtonLabel) {
        buttonDefaultsBuilder.labelTypography(
            Typography.Style(
                fontSize = overrides.buttonLabelFontSize,
                fontWeight = overrides.buttonLabelFontWeight,
                textTransform = overrides.buttonLabelTextTransform ?: TextTransform.None,
            )
        )
    }
    buttonDefaults(buttonDefaultsBuilder.build())
}

private fun Appearance.Builder.applyButtonDanger(overrides: CustomThemeOverrides) {
    if (overrides.buttonDangerColorBackground == null &&
        overrides.buttonDangerColorBorder == null &&
        overrides.buttonDangerColorText == null
    ) {
        return
    }
    buttonDanger(
        Button(
            colorBackground = overrides.buttonDangerColorBackground,
            colorBorder = overrides.buttonDangerColorBorder,
            colorText = overrides.buttonDangerColorText,
        )
    )
}

private fun Appearance.Builder.applyBadgeDefaults(overrides: CustomThemeOverrides) {
    val hasBadgeLabel = overrides.badgeLabelTextTransform != null ||
        overrides.badgeLabelFontWeight != null ||
        overrides.badgeLabelFontSize != null
    val hasBadgePadding = overrides.badgePaddingX != null || overrides.badgePaddingY != null
    if (!hasBadgeLabel && !hasBadgePadding) return
    val badgeDefaultsBuilder = BadgeDefaults.Builder()
        .paddingX(overrides.badgePaddingX)
        .paddingY(overrides.badgePaddingY)
    if (hasBadgeLabel) {
        badgeDefaultsBuilder.labelTypography(
            Typography.Style(
                fontSize = overrides.badgeLabelFontSize,
                fontWeight = overrides.badgeLabelFontWeight,
                textTransform = overrides.badgeLabelTextTransform ?: TextTransform.None,
            )
        )
    }
    badgeDefaults(badgeDefaultsBuilder.build())
}

private fun Appearance.Builder.applyActions(overrides: CustomThemeOverrides) {
    overrides.actionPrimaryTextTransform?.let {
        actionPrimaryText(Action.Builder().textTransform(it).build())
    }
    overrides.actionSecondaryTextTransform?.let {
        actionSecondaryText(Action.Builder().textTransform(it).build())
    }
}

private fun Appearance.Builder.applyForm(overrides: CustomThemeOverrides) {
    if (overrides.formPlaceholderTextColor == null &&
        overrides.inputFieldPaddingX == null &&
        overrides.inputFieldPaddingY == null
    ) {
        return
    }
    val formBuilder = Form.Builder()
    overrides.formPlaceholderTextColor?.let { formBuilder.placeholderTextColor(it) }
    overrides.inputFieldPaddingX?.let { formBuilder.inputFieldPaddingX(it) }
    overrides.inputFieldPaddingY?.let { formBuilder.inputFieldPaddingY(it) }
    form(formBuilder.build())
}
