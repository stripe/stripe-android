package com.stripe.android.connect.webview.serialization

import androidx.annotation.ColorInt
import com.stripe.android.connect.PrivateBetaConnectSDK
import com.stripe.android.connect.appearance.Appearance
import com.stripe.android.connect.appearance.TextTransform
import kotlinx.serialization.Serializable

@Serializable
internal data class AppearanceJs(
    val variables: AppearanceVariablesJs,
)

@OptIn(PrivateBetaConnectSDK::class)
@Serializable
internal data class AppearanceVariablesJs(
    /**
     * The font family value used throughout embedded components. If an embedded component inherits a font-family value
     * from an element on your site in which it’s placed, this setting overrides that inheritance.
     */
    val fontFamily: String?,

    /**
     * The baseline font size in px set on the embedded component root. This scales the value of other font size variables.
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
     * The color used for secondary actions and links.
     */
    @ColorInt val actionSecondaryColorText: IntAsRgbHexString?,

    /**
     * The color used for text decoration of secondary actions and links.
     */
    @ColorInt val actionSecondaryTextDecorationColor: IntAsRgbHexString?,

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
    val bodyMdFontWeight: Int?,

    /**
     * The font size in px for the small body typography.
     */
    val bodySmFontSize: String?,

    /**
     * The font weight (between 0-1000) for the small body typography.
     */
    val bodySmFontWeight: Int?,

    /**
     * The font size in px for the extra large heading typography.
     */
    val headingXlFontSize: String?,

    /**
     * The font weight (between 0-1000) for the extra large heading typography.
     */
    val headingXlFontWeight: Int?,

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
    val headingLgFontWeight: Int?,

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
    val headingMdFontWeight: Int?,

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
    val headingSmFontWeight: Int?,

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
    val headingXsFontWeight: Int?,

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
    val labelMdFontWeight: Int?,

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
    val labelSmFontWeight: Int?,

    /**
     * The text transform for the small label typography. Label typography variables accept a valid text
     * transform value.
     */
    val labelSmTextTransform: TextTransform?,
)

@OptIn(PrivateBetaConnectSDK::class)
@Suppress("LongMethod")
internal fun Appearance.toJs(): AppearanceJs {
    return AppearanceJs(
        variables = AppearanceVariablesJs(
            fontFamily = typography.fontFamily,
            fontSizeBase = typography.fontSizeBase?.let { "${it.toString().removeSuffix(".0")}px" },
            spacingUnit = spacingUnit?.let { "${it.toString().removeSuffix(".0")}px" },
            borderRadius = cornerRadius.base?.let { "${it.toString().removeSuffix(".0")}px" },
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
            colorSecondaryText = colors.secondaryText,
            actionPrimaryColorText = colors.actionPrimaryText,
            actionPrimaryTextDecorationColor = null,
            actionSecondaryColorText = colors.actionSecondaryText,
            actionSecondaryTextDecorationColor = null,
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
            offsetBackgroundColor = colors.offsetBackground,
            formBackgroundColor = colors.formBackground,
            colorBorder = colors.border,
            formHighlightColorBorder = colors.formHighlightBorder,
            formAccentColor = colors.formAccent,
            buttonBorderRadius = cornerRadius.button?.let { "${it.toString().removeSuffix(".0")}px" },
            formBorderRadius = cornerRadius.form?.let { "${it.toString().removeSuffix(".0")}px" },
            badgeBorderRadius = cornerRadius.badge?.let { "${it.toString().removeSuffix(".0")}px" },
            overlayBorderRadius = cornerRadius.overlay?.let { "${it.toString().removeSuffix(".0")}px" },
            overlayBackdropColor = null,
            bodyMdFontSize = typography.bodyMd?.fontSize?.let { "${it.toString().removeSuffix(".0")}px" },
            bodyMdFontWeight = typography.bodyMd?.fontWeight,
            bodySmFontSize = typography.bodySm?.fontSize?.let { "${it.toString().removeSuffix(".0")}px" },
            bodySmFontWeight = typography.bodySm?.fontWeight,
            headingXlFontSize = typography.headingXl?.fontSize?.let { "${it.toString().removeSuffix(".0")}px" },
            headingXlFontWeight = typography.headingXl?.fontWeight,
            headingXlTextTransform = typography.headingXl?.textTransform,
            headingLgFontSize = typography.headingLg?.fontSize?.let { "${it.toString().removeSuffix(".0")}px" },
            headingLgFontWeight = typography.headingLg?.fontWeight,
            headingLgTextTransform = typography.headingLg?.textTransform,
            headingMdFontSize = typography.headingMd?.fontSize?.let { "${it.toString().removeSuffix(".0")}px" },
            headingMdFontWeight = typography.headingMd?.fontWeight,
            headingMdTextTransform = typography.headingMd?.textTransform,
            headingSmFontSize = typography.headingSm?.fontSize?.let { "${it.toString().removeSuffix(".0")}px" },
            headingSmFontWeight = typography.headingSm?.fontWeight,
            headingSmTextTransform = typography.headingSm?.textTransform,
            headingXsFontSize = typography.headingXs?.fontSize?.let { "${it.toString().removeSuffix(".0")}px" },
            headingXsFontWeight = typography.headingXs?.fontWeight,
            headingXsTextTransform = typography.headingXs?.textTransform,
            labelMdFontSize = typography.labelMd?.fontSize?.let { "${it.toString().removeSuffix(".0")}px" },
            labelMdFontWeight = typography.labelMd?.fontWeight,
            labelMdTextTransform = typography.labelMd?.textTransform,
            labelSmFontSize = typography.labelSm?.fontSize?.let { "${it.toString().removeSuffix(".0")}px" },
            labelSmFontWeight = typography.labelSm?.fontWeight,
            labelSmTextTransform = typography.labelSm?.textTransform,
        )
    )
}
