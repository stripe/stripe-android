package com.stripe.android.paymentelement.embedded.linkinline

import androidx.annotation.DrawableRes
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Card
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.ContentAlpha
import androidx.compose.material.Divider
import androidx.compose.material.DropdownMenu
import androidx.compose.material.DropdownMenuItem
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.max
import androidx.compose.ui.unit.sp
import com.stripe.android.link.ui.LinkLogoStyle
import com.stripe.android.link.ui.logoRes
import com.stripe.android.model.LinkBrand
import com.stripe.android.paymentsheet.PaymentSheet.Appearance.Embedded.RowStyle
import com.stripe.android.paymentsheet.R
import com.stripe.android.paymentsheet.ui.ErrorMessage
import com.stripe.android.paymentsheet.verticalmode.separatorColor
import com.stripe.android.uicore.LocalColors
import com.stripe.android.uicore.darken
import com.stripe.android.uicore.elements.OTPElement
import com.stripe.android.uicore.elements.OTPElementColors
import com.stripe.android.uicore.elements.OTPElementUI
import com.stripe.android.uicore.getBorderStroke
import com.stripe.android.uicore.getBorderStrokeColor
import com.stripe.android.uicore.lighten
import com.stripe.android.uicore.shouldUseDarkDynamicColor
import com.stripe.android.uicore.strings.resolve
import com.stripe.android.uicore.stripeColors
import com.stripe.android.uicore.stripeShapes
import com.stripe.android.uicore.stripeThemeIsDark
import com.stripe.android.uicore.stripeTypography

/**
 * The card a returning Link customer verifies in, shown in place of the Link row in the Embedded Payment Element.
 *
 * It only renders [state] and reports taps. Its owner decides where a tapped code is entered.
 */
@Composable
internal fun EmbeddedLinkInlineVerificationCard(
    state: EmbeddedLinkInlineCardState,
    otpElement: OTPElement,
    linkBrand: LinkBrand,
    rowStyle: RowStyle,
    onCodeTapped: () -> Unit,
    onClose: () -> Unit,
    onSwitchAccount: () -> Unit,
    onAction: (EmbeddedLinkInlineCardState.Action) -> Unit,
    modifier: Modifier = Modifier,
) {
    val textStyles = EmbeddedLinkInlineCardTextStyles.create()
    CardContainer(
        rowStyle = rowStyle,
        modifier = modifier.testTag(EMBEDDED_LINK_INLINE_CARD_TEST_TAG),
    ) {
        Column(modifier = Modifier.padding(start = 16.dp, top = 12.dp, end = 16.dp, bottom = 16.dp)) {
            Header(
                email = state.email,
                textStyles = textStyles,
                onSwitchAccount = onSwitchAccount,
                onClose = onClose,
            )
            Spacer(modifier = Modifier.height(10.dp))
            Hairline(rowStyle = rowStyle)
            Spacer(modifier = Modifier.height(16.dp))
            FocusRegion(
                state = state,
                otpElement = otpElement,
                textStyles = textStyles,
                onCodeTapped = onCodeTapped,
                onAction = onAction,
            )
            Spacer(modifier = Modifier.height(16.dp))
            Hairline(rowStyle = rowStyle)
            Spacer(modifier = Modifier.height(16.dp))
            Image(
                painter = painterResource(linkBrand.logoRes(LinkLogoStyle.Primary)),
                contentDescription = linkBrand.brandName(),
                // The logo scales with the title.
                modifier = Modifier.height(with(LocalDensity.current) { textStyles.title.fontSize.toDp() }),
            )
        }
    }
}

@Composable
private fun CardContainer(
    rowStyle: RowStyle,
    modifier: Modifier,
    content: @Composable () -> Unit,
) {
    val shape = MaterialTheme.shapes.medium
    if (rowStyle is RowStyle.FloatingButton) {
        Card(
            modifier = modifier.fillMaxWidth(),
            shape = shape,
            backgroundColor = MaterialTheme.stripeColors.component,
            border = MaterialTheme.getBorderStroke(isSelected = false),
            elevation = 0.dp,
            content = content,
        )
    } else {
        val borderWidth = max(MaterialTheme.stripeShapes.borderStrokeWidth.dp, 1.dp)
        Box(
            modifier = modifier
                .fillMaxWidth()
                .border(BorderStroke(borderWidth, MaterialTheme.stripeColors.componentBorder), shape),
        ) {
            content()
        }
    }
}

@Composable
private fun Header(
    email: String,
    textStyles: EmbeddedLinkInlineCardTextStyles,
    onSwitchAccount: () -> Unit,
    onClose: () -> Unit,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Box(modifier = Modifier.weight(1f)) {
            EmailPill(email = email, textStyle = textStyles.email, onSwitchAccount = onSwitchAccount)
        }
        CloseButton(onClose = onClose)
    }
}

@Composable
private fun EmailPill(
    email: String,
    textStyle: TextStyle,
    onSwitchAccount: () -> Unit,
) {
    var isMenuExpanded by remember { mutableStateOf(false) }
    val switchAccountsLabel = stringResource(R.string.stripe_link_inline_verification_switch_accounts)
    Box {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .clip(EmailPillShape)
                .background(shadedComponentColor())
                .clickable(role = Role.DropdownList, onClickLabel = switchAccountsLabel) { isMenuExpanded = true }
                .padding(horizontal = 8.dp, vertical = 4.dp)
                .testTag(EMBEDDED_LINK_INLINE_CARD_EMAIL_TEST_TAG),
        ) {
            Text(
                text = email,
                style = textStyle,
                maxLines = 1,
                overflow = TextOverflow.MiddleEllipsis,
                modifier = Modifier.weight(1f, fill = false),
            )
            Spacer(modifier = Modifier.width(4.dp))
            Icon(
                painter = painterResource(R.drawable.stripe_ic_paymentsheet_ctil_chevron_down),
                contentDescription = null,
                tint = textStyle.color,
                modifier = Modifier.size(8.dp),
            )
        }
        // A DropdownMenu opens below its anchor, so the email stays visible.
        DropdownMenu(
            expanded = isMenuExpanded,
            onDismissRequest = { isMenuExpanded = false },
        ) {
            DropdownMenuItem(
                onClick = {
                    isMenuExpanded = false
                    onSwitchAccount()
                },
                modifier = Modifier.testTag(EMBEDDED_LINK_INLINE_CARD_SWITCH_ACCOUNTS_TEST_TAG),
            ) {
                Text(text = switchAccountsLabel, style = textStyle)
            }
        }
    }
}

@Composable
private fun CloseButton(onClose: () -> Unit) {
    IconButton(
        onClick = onClose,
        modifier = Modifier.testTag(EMBEDDED_LINK_INLINE_CARD_CLOSE_TEST_TAG),
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(CloseButtonSize)
                .clip(CircleShape)
                .background(MaterialTheme.stripeColors.component)
                .border(BorderStroke(1.dp, MaterialTheme.stripeColors.componentBorder), CircleShape),
        ) {
            Icon(
                painter = painterResource(R.drawable.stripe_ic_paymentsheet_close),
                contentDescription = stringResource(R.string.stripe_paymentsheet_close),
                tint = MaterialTheme.stripeColors.appBarIcon,
                modifier = Modifier.size(CloseIconSize),
            )
        }
    }
}

@Composable
private fun Hairline(rowStyle: RowStyle) {
    val color = if (rowStyle.hasSeparators()) {
        Color(rowStyle.separatorColor(MaterialTheme.stripeThemeIsDark))
    } else {
        MaterialTheme.stripeColors.componentBorder
    }
    Divider(color = color, thickness = Dp.Hairline)
}

@Composable
private fun FocusRegion(
    state: EmbeddedLinkInlineCardState,
    otpElement: OTPElement,
    textStyles: EmbeddedLinkInlineCardTextStyles,
    onCodeTapped: () -> Unit,
    onAction: (EmbeddedLinkInlineCardState.Action) -> Unit,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .fillMaxWidth()
            .testTag(EMBEDDED_LINK_INLINE_CARD_FOCUS_REGION_TEST_TAG),
    ) {
        Text(
            text = stringResource(R.string.stripe_link_inline_verification_title),
            style = textStyles.title,
            textAlign = TextAlign.Center,
            modifier = Modifier.semantics { heading() },
        )
        Spacer(modifier = Modifier.height(4.dp))
        val subtitle = subtitle(recipient = state.recipient, textStyles = textStyles)
        Text(
            text = subtitle,
            style = textStyles.subtitle,
            textAlign = TextAlign.Center,
        )
        Spacer(modifier = Modifier.height(16.dp))
        CodeBoxes(
            isEnabled = state.isCodeEntryEnabled,
            otpElement = otpElement,
            digitStyle = textStyles.digit,
            accessibilityLabel = subtitle.text,
            onCodeTapped = onCodeTapped,
        )
        state.errorMessage?.let { errorMessage ->
            Spacer(modifier = Modifier.height(12.dp))
            ErrorMessage(
                error = errorMessage.resolve(),
                textAlign = TextAlign.Center,
                modifier = Modifier.testTag(EMBEDDED_LINK_INLINE_CARD_ERROR_TEST_TAG),
            )
        }
        if (state.actions != EmbeddedLinkInlineCardState.Actions.None) {
            Spacer(modifier = Modifier.height(12.dp))
            ActionButton(
                actions = state.actions,
                isLoading = state.isActionLoading,
                textStyle = textStyles.action,
                onAction = onAction,
            )
        }
    }
}

@Composable
private fun subtitle(recipient: String, textStyles: EmbeddedLinkInlineCardTextStyles): AnnotatedString {
    // Keeps the masked number in order in right-to-left languages.
    val isolatedRecipient = "\u2068$recipient\u2069"
    val text = stringResource(R.string.stripe_link_verification_message_short, isolatedRecipient)
    val recipientStart = text.indexOf(isolatedRecipient)
    return buildAnnotatedString {
        append(text)
        if (recipientStart >= 0) {
            addStyle(
                style = SpanStyle(color = textStyles.title.color, fontWeight = textStyles.action.fontWeight),
                start = recipientStart,
                end = recipientStart + isolatedRecipient.length,
            )
        }
    }
}

@Composable
private fun CodeBoxes(
    isEnabled: Boolean,
    otpElement: OTPElement,
    digitStyle: TextStyle,
    accessibilityLabel: String,
    onCodeTapped: () -> Unit,
) {
    val stripeColors = MaterialTheme.stripeColors
    val shadedColor = shadedComponentColor()
    val colors = OTPElementColors(
        selectedBorder = MaterialTheme.getBorderStrokeColor(isSelected = true),
        placeholder = stripeColors.placeholderText,
        background = shadedColor,
        selectedBackground = stripeColors.component,
        unselectedBorder = Color.Transparent,
    )
    CompositionLocalProvider(
        // Digits are always laid out left to right, even in right-to-left languages.
        LocalLayoutDirection provides LayoutDirection.Ltr,
        // Each box draws the component color around its digit, which would ring the shaded boxes.
        LocalColors provides stripeColors.copy(component = shadedColor),
    ) {
        Box(modifier = Modifier.widthIn(max = CodeMaxWidth)) {
            OTPElementUI(
                enabled = isEnabled,
                element = otpElement,
                boxShape = RoundedCornerShape(CodeBoxCornerRadius),
                boxTextStyle = digitStyle,
                boxSpacing = CodeBoxSpacing,
                middleSpacing = CodeBoxSpacing,
                otpInputPlaceholder = "",
                colors = colors,
                selectedStrokeWidth = CodeBoxFocusRingWidth,
                boxHeight = CodeBoxHeight,
            )
            // Takes taps so the field never focuses itself. The card's owner decides where the code is entered.
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .clickable(
                        enabled = isEnabled,
                        role = Role.Button,
                        onClickLabel = accessibilityLabel,
                        onClick = onCodeTapped,
                    )
                    .testTag(EMBEDDED_LINK_INLINE_CARD_CODE_TEST_TAG),
            )
        }
    }
}

@Composable
private fun ActionButton(
    actions: EmbeddedLinkInlineCardState.Actions,
    isLoading: Boolean,
    textStyle: TextStyle,
    onAction: (EmbeddedLinkInlineCardState.Action) -> Unit,
) {
    when (actions) {
        is EmbeddedLinkInlineCardState.Actions.None -> Unit
        is EmbeddedLinkInlineCardState.Actions.Resend -> {
            TextActionButton(
                label = actions.label.resolve(),
                isEnabled = actions.isEnabled && !isLoading,
                isLoading = isLoading,
                textStyle = textStyle,
                onClick = { onAction(EmbeddedLinkInlineCardState.Action.Resend) },
            )
        }
        is EmbeddedLinkInlineCardState.Actions.Menu -> {
            ActionsMenu(
                menu = actions,
                isLoading = isLoading,
                textStyle = textStyle,
                onAction = onAction,
            )
        }
    }
}

@Composable
private fun ActionsMenu(
    menu: EmbeddedLinkInlineCardState.Actions.Menu,
    isLoading: Boolean,
    textStyle: TextStyle,
    onAction: (EmbeddedLinkInlineCardState.Action) -> Unit,
) {
    var isMenuExpanded by remember { mutableStateOf(false) }
    Box {
        TextActionButton(
            label = stringResource(R.string.stripe_link_auth_more_options),
            isEnabled = menu.isEnabled && !isLoading,
            isLoading = isLoading,
            textStyle = textStyle,
            onClick = { isMenuExpanded = true },
        )
        DropdownMenu(
            expanded = isMenuExpanded,
            onDismissRequest = { isMenuExpanded = false },
        ) {
            menu.items.forEach { item ->
                DropdownMenuItem(
                    enabled = item.isEnabled,
                    onClick = {
                        isMenuExpanded = false
                        onAction(item.action)
                    },
                    modifier = Modifier.testTag(item.action.testTag),
                ) {
                    val alpha = if (item.isEnabled) ContentAlpha.high else ContentAlpha.disabled
                    Icon(
                        painter = painterResource(item.action.iconRes),
                        contentDescription = null,
                        tint = MaterialTheme.stripeColors.appBarIcon,
                        modifier = Modifier
                            .size(16.dp)
                            .alpha(alpha),
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = item.label.resolve(),
                        style = textStyle.copy(fontWeight = FontWeight.Normal),
                        modifier = Modifier.alpha(alpha),
                    )
                }
            }
        }
    }
}

@Composable
private fun TextActionButton(
    label: String,
    isEnabled: Boolean,
    isLoading: Boolean,
    textStyle: TextStyle,
    onClick: () -> Unit,
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .clip(MaterialTheme.shapes.small)
            .clickable(enabled = isEnabled, role = Role.Button, onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 4.dp)
            .testTag(EMBEDDED_LINK_INLINE_CARD_ACTION_TEST_TAG),
    ) {
        Text(
            text = label,
            style = textStyle,
            textAlign = TextAlign.Center,
            modifier = Modifier.alpha(
                when {
                    isLoading -> 0f
                    isEnabled -> ContentAlpha.high
                    else -> ContentAlpha.disabled
                }
            ),
        )
        if (isLoading) {
            CircularProgressIndicator(
                color = textStyle.color,
                strokeWidth = 2.dp,
                modifier = Modifier.size(16.dp),
            )
        }
    }
}

private val EmbeddedLinkInlineCardState.Action.iconRes: Int
    @DrawableRes
    get() = when (this) {
        EmbeddedLinkInlineCardState.Action.Resend -> R.drawable.stripe_link_refresh
        EmbeddedLinkInlineCardState.Action.Email -> R.drawable.stripe_link_email
    }

private val EmbeddedLinkInlineCardState.Action.testTag: String
    get() = when (this) {
        EmbeddedLinkInlineCardState.Action.Resend -> EMBEDDED_LINK_INLINE_CARD_MENU_RESEND_TEST_TAG
        EmbeddedLinkInlineCardState.Action.Email -> EMBEDDED_LINK_INLINE_CARD_MENU_EMAIL_TEST_TAG
    }

/**
 * A shade of the component background, for the email and the unfocused digits.
 */
@Composable
@ReadOnlyComposable
private fun shadedComponentColor(): Color {
    val component = MaterialTheme.stripeColors.component
    return if (component.shouldUseDarkDynamicColor()) {
        component.darken(SHADE_DARKEN_AMOUNT)
    } else {
        component.lighten(SHADE_LIGHTEN_AMOUNT)
    }
}

/**
 * The card's text, in the merchant's typography. Sizes follow the font size setting up to a maximum, so the card
 * stays usable at the largest settings.
 */
private class EmbeddedLinkInlineCardTextStyles(
    val email: TextStyle,
    val title: TextStyle,
    val subtitle: TextStyle,
    val digit: TextStyle,
    val action: TextStyle,
) {
    companion object {
        @Composable
        fun create(): EmbeddedLinkInlineCardTextStyles {
            val typography = MaterialTheme.stripeTypography
            val base = MaterialTheme.typography.body1
            val regular = FontWeight(typography.fontWeightNormal)
            val medium = FontWeight(typography.fontWeightMedium)
            val textColor = MaterialTheme.stripeColors.onComponent
            val smallSize = (typography.mediumFontSize * typography.fontSizeMultiplier).cappedAt(SmallMaxFontSize)
            return EmbeddedLinkInlineCardTextStyles(
                email = base.copy(fontSize = smallSize, fontWeight = regular, color = textColor),
                title = base.copy(
                    fontSize = (typography.largeFontSize * typography.fontSizeMultiplier).cappedAt(TitleMaxFontSize),
                    fontWeight = medium,
                    color = textColor,
                ),
                subtitle = base.copy(
                    fontSize = smallSize,
                    fontWeight = regular,
                    color = MaterialTheme.stripeColors.subtitle,
                ),
                digit = base.copy(
                    fontSize = (typography.xLargeFontSize * typography.fontSizeMultiplier).cappedAt(DigitMaxFontSize),
                    fontWeight = medium,
                    color = textColor,
                    textAlign = TextAlign.Center,
                ),
                action = base.copy(fontSize = smallSize, fontWeight = medium, color = textColor),
            )
        }

        /**
         * Limits how large text gets at large font size settings.
         *
         * @param maximum The largest size, as it would render with the default font size setting.
         */
        @Composable
        private fun TextUnit.cappedAt(maximum: TextUnit): TextUnit {
            return with(LocalDensity.current) {
                val maximumPx = maximum.value.dp.toPx()
                if (toPx() > maximumPx) maximumPx.toSp() else this@cappedAt
            }
        }
    }
}

private val EmailPillShape = RoundedCornerShape(6.dp)
private val CloseButtonSize = 22.dp
private val CloseIconSize = 8.dp

/**
 * Link's own verification screen, scaled down: 56dp-tall digits with a 12dp radius.
 */
private val CodeBoxHeight = 44.dp
private val CodeBoxMaxWidth = CodeBoxHeight / 1.16f
private val CodeBoxCornerRadius = CodeBoxHeight * 12 / 56
private val CodeBoxSpacing = 8.dp
private val CodeBoxFocusRingWidth = 1.5.dp
private const val CODE_LENGTH = 6
private val CodeMaxWidth = CodeBoxMaxWidth * CODE_LENGTH + CodeBoxSpacing * (CODE_LENGTH - 1)

private val SmallMaxFontSize = 25.sp
private val TitleMaxFontSize = 30.sp
private val DigitMaxFontSize = 28.sp

private const val SHADE_DARKEN_AMOUNT = 0.04f
private const val SHADE_LIGHTEN_AMOUNT = 0.08f

internal const val EMBEDDED_LINK_INLINE_CARD_TEST_TAG = "embedded_link_inline_verification"
internal const val EMBEDDED_LINK_INLINE_CARD_EMAIL_TEST_TAG = "embedded_link_inline_verification_email"
internal const val EMBEDDED_LINK_INLINE_CARD_SWITCH_ACCOUNTS_TEST_TAG =
    "embedded_link_inline_verification_switch_accounts"
internal const val EMBEDDED_LINK_INLINE_CARD_CLOSE_TEST_TAG = "embedded_link_inline_verification_close"
internal const val EMBEDDED_LINK_INLINE_CARD_FOCUS_REGION_TEST_TAG = "embedded_link_inline_verification_focus_region"
internal const val EMBEDDED_LINK_INLINE_CARD_CODE_TEST_TAG = "embedded_link_inline_verification_code"
internal const val EMBEDDED_LINK_INLINE_CARD_ERROR_TEST_TAG = "embedded_link_inline_verification_error"
internal const val EMBEDDED_LINK_INLINE_CARD_ACTION_TEST_TAG = "embedded_link_inline_verification_action"
internal const val EMBEDDED_LINK_INLINE_CARD_MENU_RESEND_TEST_TAG = "embedded_link_inline_verification_menu_resend"
internal const val EMBEDDED_LINK_INLINE_CARD_MENU_EMAIL_TEST_TAG = "embedded_link_inline_verification_menu_email"
