@file:OptIn(com.stripe.android.connect.PreviewConnectSDK::class)

package com.stripe.android.connect.webview.serialization

import androidx.annotation.ColorInt
import com.stripe.android.connect.appearance.Appearance
import com.stripe.android.connect.appearance.TextTransform
import kotlinx.serialization.Serializable
import kotlin.math.roundToInt

@Serializable
internal data class AppearanceJs(
    val variables: AppearanceVariablesJs,
)

@Serializable
internal data class AppearanceVariablesJs(
    /**
     * The font family value used throughout embedded components. If an embedded component inherits a font-family value
     * from an element on your site in which it’s placed, this setting overrides that inheritance.
     */
    val fontFamily: String?,
    /**
     * The baseline font size in px set on the embedded component root.
     * This scales the value of other font size variables.
     */
    val fontSizeBase: String?,
    /**
     * The base spacing unit in px that derives all spacing values. Increase or decrease this value to make your layout
     * more or less spacious.
     */
    val spacingUnit: String?,
    /**
     * The general border radius in px used in embedded components. This sets the default border radius for all
     * components.
     */
    val borderRadius: String?,
    /**
     * The primary color used throughout embedded components. Set this to your primary brand color.
     */
    @ColorInt val colorPrimary: IntAsRgbHexString?,
    /**
     * The background color for embedded components, including overlays, tooltips, and popovers.
     */
    @ColorInt val colorBackground: IntAsRgbHexString?,
    /**
     * The color used for regular text.
     */
    @ColorInt val colorText: IntAsRgbHexString?,
    /**
     * The color used to indicate errors or destructive actions.
     */
    @ColorInt val colorDanger: IntAsRgbHexString?,
    /**
     * The color used as a background for primary buttons.
     */
    @ColorInt val buttonPrimaryColorBackground: IntAsRgbHexString?,
    /**
     * The border color used for primary buttons.
     */
    @ColorInt val buttonPrimaryColorBorder: IntAsRgbHexString?,
    /**
     * The text color used for primary buttons.
     */
    @ColorInt val buttonPrimaryColorText: IntAsRgbHexString?,
    /**
     * The color used as a background for secondary buttons.
     */
    @ColorInt val buttonSecondaryColorBackground: IntAsRgbHexString?,
    /**
     * The color used as a border for secondary buttons.
     */
    @ColorInt val buttonSecondaryColorBorder: IntAsRgbHexString?,
    /**
     * The text color used for secondary buttons.
     */
    @ColorInt val buttonSecondaryColorText: IntAsRgbHexString?,
    /**
     * The background color for danger buttons to indicate destructive actions.
     */
    @ColorInt val buttonDangerColorBackground: IntAsRgbHexString?,
    /**
     * The border color for danger buttons to indicate destructive actions.
     */
    @ColorInt val buttonDangerColorBorder: IntAsRgbHexString?,
    /**
     * The text color for danger buttons to indicate destructive actions.
     */
    @ColorInt val buttonDangerColorText: IntAsRgbHexString?,
    /**
     * The vertical padding in px for buttons.
     */
    val buttonPaddingY: String?,
    /**
     * The horizontal padding in px for buttons.
     */
    val buttonPaddingX: String?,
    /**
     * The font size in px for the button label typography.
     */
    val buttonLabelFontSize: String?,
    /**
     * The font weight (between 0-1000) for the button label typography.
     */
    val buttonLabelFontWeight: String?,
    /**
     * The text transform for the button label typography.
     */
    val buttonLabelTextTransform: TextTransform?,
    /**
     * The color used for secondary text.
     */
    @ColorInt val colorSecondaryText: IntAsRgbHexString?,
    /**
     * The color used for primary actions and links.
     */
    @ColorInt val actionPrimaryColorText: IntAsRgbHexString?,
    /**
     * The color used for text decoration of primary actions and links.
     */
    @ColorInt val actionPrimaryTextDecorationColor: IntAsRgbHexString?,
    /**
     * The text transform for primary actions and links.
     */
    val actionPrimaryTextTransform: TextTransform?,
    /**
     * The color used for secondary actions and links.
     */
    @ColorInt val actionSecondaryColorText: IntAsRgbHexString?,
    /**
     * The color used for text decoration of secondary actions and links.
     */
    @ColorInt val actionSecondaryTextDecorationColor: IntAsRgbHexString?,
    /**
     * The text transform for secondary actions and links.
     */
    val actionSecondaryTextTransform: TextTransform?,
    /**
     * The background color used to represent neutral state or lack of state in status badges.
     */
    @ColorInt val badgeNeutralColorBackground: IntAsRgbHexString?,
    /**
     * The text color used to represent neutral state or lack of state in status badges.
     */
    @ColorInt val badgeNeutralColorText: IntAsRgbHexString?,
    /**
     * The border color used to represent neutral state or lack of state in status badges.
     */
    @ColorInt val badgeNeutralColorBorder: IntAsRgbHexString?,
    /**
     * The background color used to reinforce a successful outcome in status badges.
     */
    @ColorInt val badgeSuccessColorBackground: IntAsRgbHexString?,
    /**
     * The text color used to reinforce a successful outcome in status badges.
     */
    @ColorInt val badgeSuccessColorText: IntAsRgbHexString?,
    /**
     * The border color used to reinforce a successful outcome in status badges.
     */
    @ColorInt val badgeSuccessColorBorder: IntAsRgbHexString?,
    /**
     * The background color used in status badges to highlight things that might require action,
     * but are optional to resolve.
     */
    @ColorInt val badgeWarningColorBackground: IntAsRgbHexString?,
    /**
     * The text color used in status badges to highlight things that might require action,
     * but are optional to resolve.
     */
    @ColorInt val badgeWarningColorText: IntAsRgbHexString?,
    /**
     * The border color used in status badges to highlight things that might require action,
     * but are optional to resolve.
     */
    @ColorInt val badgeWarningColorBorder: IntAsRgbHexString?,
    /**
     * The background color used in status badges for high-priority, critical situations that the user must address
     * immediately, and to indicate failed or unsuccessful outcomes.
     */
    @ColorInt val badgeDangerColorBackground: IntAsRgbHexString?,
    /**
     * The text color used in status badges for high-priority, critical situations that the user must address
     * immediately, and to indicate failed or unsuccessful outcomes.
     */
    @ColorInt val badgeDangerColorText: IntAsRgbHexString?,
    /**
     * The border color used in status badges for high-priority, critical situations that the user must address
     * immediately, and to indicate failed or unsuccessful outcomes.
     */
    @ColorInt val badgeDangerColorBorder: IntAsRgbHexString?,
    /**
     * The horizontal padding in px for badges.
     */
    val badgePaddingX: String?,
    /**
     * The vertical padding in px for badges.
     */
    val badgePaddingY: String?,
    /**
     * The font size in px for the badge label typography.
     */
    val badgeLabelFontSize: String?,
    /**
     * The font weight (between 0-1000) for the badge label typography.
     */
    val badgeLabelFontWeight: String?,
    /**
     * The text transform for the badge label typography.
     */
    val badgeLabelTextTransform: TextTransform?,
    /**
     * The background color used when highlighting information, like the selected row on a table or particular piece of
     * UI.
     */
    @ColorInt val offsetBackgroundColor: IntAsRgbHexString?,
    /**
     * The background color used for form items.
     */
    @ColorInt val formBackgroundColor: IntAsRgbHexString?,
    /**
     * The color used for borders throughout the component.
     */
    @ColorInt val colorBorder: IntAsRgbHexString?,
    /**
     * The color used to highlight form items when focused.
     */
    @ColorInt val formHighlightColorBorder: IntAsRgbHexString?,
    /**
     * The color used for to fill in form items like checkboxes, radio buttons and switches.
     */
    @ColorInt val formAccentColor: IntAsRgbHexString?,
    /**
     * The color used for placeholder text in form items.
     */
    @ColorInt val formPlaceholderTextColor: IntAsRgbHexString?,
    /**
     * The horizontal padding in px for form items.
     */
    val inputFieldPaddingX: String?,
    /**
     * The vertical padding in px for form items.
     */
    val inputFieldPaddingY: String?,
    /**
     * The vertical padding in px for table rows.
     */
    val tableRowPaddingY: String?,
    /**
     * The border radius in px used for buttons.
     */
    val buttonBorderRadius: String?,
    /**
     * The border radius in px used for form elements.
     */
    val formBorderRadius: String?,
    /**
     * The border radius in px used for badges.
     */
    val badgeBorderRadius: String?,
    /**
     * The border radius in px used for overlays.
     */
    val overlayBorderRadius: String?,
    /**
     * The backdrop color when an overlay is opened.
     */
    @ColorInt val overlayBackdropColor: IntAsRgbHexString?,

    /**
     * The font size in px for the medium body typography.
     */
    val bodyMdFontSize: String?,
    /**
     * The font weight (between 0-1000) for the medium body typography.
     */
    val bodyMdFontWeight: String?,
    /**
     * The font size in px for the small body typography.
     */
    val bodySmFontSize: String?,
    /**
     * The font weight (between 0-1000) for the small body typography.
     */
    val bodySmFontWeight: String?,
    /**
     * The font size in px for the extra large heading typography.
     */
    val headingXlFontSize: String?,
    /**
     * The font weight (between 0-1000) for the extra large heading typography.
     */
    val headingXlFontWeight: String?,
    /**
     * The text transform for the extra large heading typography. Heading typography variables accept a valid text
     * transform value.
     */
    val headingXlTextTransform: TextTransform?,
    /**
     * The font size in px for the large heading typography.
     */
    val headingLgFontSize: String?,
    /**
     * The font weight (between 0-1000) for the large heading typography.
     */
    val headingLgFontWeight: String?,
    /**
     * The text transform for the large heading typography. Heading typography variables accept a valid text
     * transform value.
     */
    val headingLgTextTransform: TextTransform?,
    /**
     * The font size in px for the medium heading typography.
     */
    val headingMdFontSize: String?,
    /**
     * The font weight (between 0-1000) for the medium heading typography.
     */
    val headingMdFontWeight: String?,
    /**
     * The text transform for the medium heading typography. Heading typography variables accept a valid text
     * transform value.
     */
    val headingMdTextTransform: TextTransform?,
    /**
     * The font size in px for the small heading typography.
     */
    val headingSmFontSize: String?,
    /**
     * The font weight (between 0-1000) for the small heading typography.
     */
    val headingSmFontWeight: String?,
    /**
     * The text transform for the small heading typography. Heading typography variables accept a valid text
     * transform value.
     */
    val headingSmTextTransform: TextTransform?,
    /**
     * The font size in px for the extra small heading typography.
     */
    val headingXsFontSize: String?,
    /**
     * The font weight (between 0-1000) for the extra small heading typography.
     */
    val headingXsFontWeight: String?,
    /**
     * The text transform for the extra small heading typography. Heading typography variables accept a valid text
     * transform value.
     */
    val headingXsTextTransform: TextTransform?,
    /**
     * The font size in px for the medium label typography.
     */
    val labelMdFontSize: String?,
    /**
     * The font weight (between 0-1000) for the medium label typography.
     */
    val labelMdFontWeight: String?,
    /**
     * The text transform for the medium label typography. Label typography variables accept a valid text
     * transform value.
     */
    val labelMdTextTransform: TextTransform?,
    /**
     * The font size in px for the small label typography.
     */
    val labelSmFontSize: String?,
    /**
     * The font weight (between 0-1000) for the small label typography.
     */
    val labelSmFontWeight: String?,
    /**
     * The text transform for the small label typography. Label typography variables accept a valid text
     * transform value.
     */
    val labelSmTextTransform: TextTransform?,
)

private fun Float?.toPx(): String? {
    return this?.let { "${roundToInt()}px" }
}

/**
 * We need to send the user's customization options to ConnectJS which is only aware of web.
 * Unscaled floats are sent without conversion since browsers will appropriately
 * scale `px` values.
 */
@Suppress("LongMethod")
internal fun Appearance.toJs(): AppearanceJs {
    return AppearanceJs(
        variables = AppearanceVariablesJs(
            fontFamily = typography.fontFamily,
            fontSizeBase = typography.fontSizeBase.toPx(),
            spacingUnit = spacingUnit.toPx(),
            borderRadius = cornerRadius.base.toPx(),
            colorPrimary = colors.primary,
            colorBackground = colors.background,
            colorText = colors.text,
            colorDanger = colors.danger,
            buttonPrimaryColorBackground = buttonPrimary.colorBackground,
            buttonPrimaryColorBorder = buttonPrimary.colorBorder,
            buttonPrimaryColorText = buttonPrimary.colorText,
            buttonSecondaryColorBackground = buttonSecondary.colorBackground,
            buttonSecondaryColorBorder = buttonSecondary.colorBorder,
            buttonSecondaryColorText = buttonSecondary.colorText,
            buttonDangerColorBackground = buttonDanger.colorBackground,
            buttonDangerColorBorder = buttonDanger.colorBorder,
            buttonDangerColorText = buttonDanger.colorText,
            buttonPaddingX = buttonDefaults.paddingX.toPx(),
            buttonPaddingY = buttonDefaults.paddingY.toPx(),
            buttonLabelFontSize = buttonDefaults.labelTypography?.fontSize.toPx(),
            buttonLabelFontWeight = buttonDefaults.labelTypography?.fontWeight?.toString(),
            buttonLabelTextTransform = buttonDefaults.labelTypography?.textTransform,
            colorSecondaryText = colors.secondaryText,
            actionPrimaryColorText = actionPrimaryText.colorText ?: colors.actionPrimaryText,
            actionPrimaryTextDecorationColor = null,
            actionPrimaryTextTransform = actionPrimaryText.textTransform,
            actionSecondaryColorText = actionSecondaryText.colorText ?: colors.actionSecondaryText,
            actionSecondaryTextDecorationColor = null,
            actionSecondaryTextTransform = actionSecondaryText.textTransform,
            badgeNeutralColorBackground = badgeNeutral.colorBackground,
            badgeNeutralColorText = badgeNeutral.colorText,
            badgeNeutralColorBorder = badgeNeutral.colorBorder,
            badgeSuccessColorBackground = badgeSuccess.colorBackground,
            badgeSuccessColorText = badgeSuccess.colorText,
            badgeSuccessColorBorder = badgeSuccess.colorBorder,
            badgeWarningColorBackground = badgeWarning.colorBackground,
            badgeWarningColorText = badgeWarning.colorText,
            badgeWarningColorBorder = badgeWarning.colorBorder,
            badgeDangerColorBackground = badgeDanger.colorBackground,
            badgeDangerColorText = badgeDanger.colorText,
            badgeDangerColorBorder = badgeDanger.colorBorder,
            badgePaddingX = badgeDefaults.paddingX.toPx(),
            badgePaddingY = badgeDefaults.paddingY.toPx(),
            badgeLabelFontSize = badgeDefaults.labelTypography?.fontSize.toPx(),
            badgeLabelFontWeight = badgeDefaults.labelTypography?.fontWeight?.toString(),
            badgeLabelTextTransform = badgeDefaults.labelTypography?.textTransform,
            offsetBackgroundColor = colors.offsetBackground,
            formBackgroundColor = form.colorBackground ?: colors.formBackground,
            colorBorder = colors.border,
            formHighlightColorBorder = form.highlightBorder ?: colors.formHighlightBorder,
            formAccentColor = form.accent ?: colors.formAccent,
            formPlaceholderTextColor = form.placeholderTextColor,
            inputFieldPaddingX = form.inputFieldPaddingX.toPx(),
            inputFieldPaddingY = form.inputFieldPaddingY.toPx(),
            tableRowPaddingY = tableRowPaddingY.toPx(),
            buttonBorderRadius = cornerRadius.button.toPx(),
            formBorderRadius = cornerRadius.form.toPx(),
            badgeBorderRadius = cornerRadius.badge.toPx(),
            overlayBorderRadius = cornerRadius.overlay.toPx(),
            overlayBackdropColor = null,
            bodyMdFontSize = typography.bodyMd?.fontSize.toPx(),
            bodyMdFontWeight = typography.bodyMd?.fontWeight?.toString(),
            bodySmFontSize = typography.bodySm?.fontSize.toPx(),
            bodySmFontWeight = typography.bodySm?.fontWeight?.toString(),
            headingXlFontSize = typography.headingXl?.fontSize.toPx(),
            headingXlFontWeight = typography.headingXl?.fontWeight?.toString(),
            headingXlTextTransform = typography.headingXl?.textTransform,
            headingLgFontSize = typography.headingLg?.fontSize.toPx(),
            headingLgFontWeight = typography.headingLg?.fontWeight?.toString(),
            headingLgTextTransform = typography.headingLg?.textTransform,
            headingMdFontSize = typography.headingMd?.fontSize.toPx(),
            headingMdFontWeight = typography.headingMd?.fontWeight?.toString(),
            headingMdTextTransform = typography.headingMd?.textTransform,
            headingSmFontSize = typography.headingSm?.fontSize.toPx(),
            headingSmFontWeight = typography.headingSm?.fontWeight?.toString(),
            headingSmTextTransform = typography.headingSm?.textTransform,
            headingXsFontSize = typography.headingXs?.fontSize.toPx(),
            headingXsFontWeight = typography.headingXs?.fontWeight?.toString(),
            headingXsTextTransform = typography.headingXs?.textTransform,
            labelMdFontSize = typography.labelMd?.fontSize.toPx(),
            labelMdFontWeight = typography.labelMd?.fontWeight?.toString(),
            labelMdTextTransform = typography.labelMd?.textTransform,
            labelSmFontSize = typography.labelSm?.fontSize.toPx(),
            labelSmFontWeight = typography.labelSm?.fontWeight?.toString(),
            labelSmTextTransform = typography.labelSm?.textTransform,
        )
    )
}
