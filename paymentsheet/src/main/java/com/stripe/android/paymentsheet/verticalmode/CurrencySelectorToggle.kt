package com.stripe.android.paymentsheet.verticalmode

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.InlineTextContent
import androidx.compose.foundation.text.appendInlineContent
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.error
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.Placeholder
import androidx.compose.ui.text.PlaceholderVerticalAlign
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import com.stripe.android.elements.CurrencySelectorElement
import com.stripe.android.paymentelement.CheckoutSessionPreview
import com.stripe.android.paymentsheet.R
import com.stripe.android.paymentsheet.ui.enabledStateAlpha
import androidx.compose.ui.R as ComposeR
import com.stripe.android.uicore.R as StripeUiCoreR

private const val EXPANDED_DISCLOSURE_ROTATION = 180f

internal const val TEST_TAG_CURRENCY_SELECTOR = "TEST_TAG_CURRENCY_SELECTOR"

internal const val TEST_TAG_CURRENCY_OPTION_PREFIX = "TEST_TAG_CURRENCY_OPTION_"

internal const val TEST_TAG_CURRENCY_SELECTOR_DISCLOSURE = "TEST_TAG_CURRENCY_SELECTOR_DISCLOSURE"
internal const val TEST_TAG_CURRENCY_SELECTOR_DETAILS = "TEST_TAG_CURRENCY_SELECTOR_DETAILS"

internal const val TEST_TAG_CURRENCY_SELECTOR_ERROR = "TEST_TAG_CURRENCY_SELECTOR_ERROR"

internal sealed interface FlagContent {
    data class Emoji(val emoji: String) : FlagContent
    data class Image(val bitmap: Bitmap) : FlagContent
}

internal data class CurrencyOption(
    val code: String,
    val formattedAmount: String,
    val flag: FlagContent,
)

internal data class CurrencySelectorOptions(
    val first: CurrencyOption,
    val second: CurrencyOption,
    val selectedCode: String,
    val exchangeRateText: String? = null,
    val exchangeRateDetails: String?,
)

@OptIn(CheckoutSessionPreview::class)
@Suppress("LongMethod", "CyclomaticComplexMethod")
@Composable
internal fun CurrencySelectorToggle(
    options: CurrencySelectorOptions,
    onCurrencySelected: (CurrencyOption) -> Unit,
    isEnabled: Boolean,
    showCurrencyCode: Boolean,
    errorMessage: String? = null,
    appearance: CurrencySelectorElement.Configuration.Appearance.State,
    modifier: Modifier = Modifier,
) {
    val shape = appearance.cornerRadiusDp?.let { RoundedCornerShape(it.dp) }
        ?: RoundedCornerShape(percent = 50)
    val borderColor = appearance.borderColor?.let { Color(it) }
        ?: MaterialTheme.colors.onSurface.copy(alpha = 0.12f)
    val borderWidth = appearance.borderWidthDp?.dp
        ?: 1.dp
    val trackBackground = appearance.background?.let { Color(it) }
        ?: MaterialTheme.colors.surface
    val pillBackground = appearance.selectedBackground?.let { Color(it) }
        ?: MaterialTheme.colors.primary
    val selectedText = appearance.selectedTextColor?.let { Color(it) }
        ?: MaterialTheme.colors.onPrimary
    val unselectedText = appearance.textColor?.let { Color(it) }
        ?: MaterialTheme.colors.onSurface
    val captionColor = appearance.textSecondaryColor?.let { Color(it) }
        ?: MaterialTheme.colors.onSurface.copy(alpha = 0.6f)
    val errorColor = appearance.dangerColor?.let { Color(it) }
        ?: MaterialTheme.colors.error

    val fontFamily = appearance.fontResId?.let { FontFamily(Font(it)) }
    val bodyStyle = MaterialTheme.typography.subtitle1.withAppearance(fontFamily, appearance.sizeScaleFactor)
    val captionStyle = MaterialTheme.typography.caption.withAppearance(fontFamily, appearance.sizeScaleFactor)
    Column(modifier = modifier) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .alpha(enabledStateAlpha(isEnabled))
                .clip(shape)
                .background(trackBackground)
                .border(
                    width = borderWidth,
                    color = borderColor,
                    shape = shape,
                )
                .semantics {
                    if (errorMessage != null) {
                        error(errorMessage)
                    }
                }
                .testTag(TEST_TAG_CURRENCY_SELECTOR),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(IntrinsicSize.Min)
                    .selectableGroup(),
            ) {
                CurrencyOptionItem(
                    currency = options.first,
                    options = options,
                    onCurrencySelected = onCurrencySelected,
                    isEnabled = isEnabled,
                    showCurrencyCode = showCurrencyCode,
                    pillBackground = pillBackground,
                    trackBackground = trackBackground,
                    selectedTextColor = selectedText,
                    unselectedTextColor = unselectedText,
                    textStyle = bodyStyle,
                    contentVerticalPaddingDp = appearance.contentVerticalPaddingDp,
                )
                CurrencyOptionItem(
                    currency = options.second,
                    options = options,
                    onCurrencySelected = onCurrencySelected,
                    isEnabled = isEnabled,
                    showCurrencyCode = showCurrencyCode,
                    pillBackground = pillBackground,
                    trackBackground = trackBackground,
                    selectedTextColor = selectedText,
                    unselectedTextColor = unselectedText,
                    textStyle = bodyStyle,
                    contentVerticalPaddingDp = appearance.contentVerticalPaddingDp,
                )
            }
        }
        CurrencyDisclosure(options, captionStyle, captionColor)
        if (errorMessage != null) {
            Text(
                text = errorMessage,
                style = captionStyle,
                color = errorColor,
                textAlign = TextAlign.Start,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp)
                    .semantics { liveRegion = LiveRegionMode.Assertive }
                    .testTag(TEST_TAG_CURRENCY_SELECTOR_ERROR),
            )
        }
    }
}

@Composable
private fun CurrencyDisclosure(options: CurrencySelectorOptions, style: TextStyle, color: Color) {
    val isIntegrationCurrency = options.selectedCode == options.second.code
    val caption = options.exchangeRateText ?: if (isIntegrationCurrency) {
        stringResource(R.string.stripe_currency_selector_bank_fees_disclaimer)
    } else {
        null
    }
    val details = options.exchangeRateDetails?.takeUnless { isIntegrationCurrency }
    var expanded by rememberSaveable(options.selectedCode, details) { mutableStateOf(false) }
    if (caption != null) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 4.dp)
                .then(
                    if (details != null) {
                        Modifier.clickable(role = Role.Button) { expanded = !expanded }
                    } else {
                        Modifier
                    }
                )
                .testTag(TEST_TAG_CURRENCY_SELECTOR_DISCLOSURE),
        ) {
            Text(text = caption, style = style, color = color, modifier = Modifier.weight(1f))
            if (details != null) {
                Icon(
                    painter = painterResource(StripeUiCoreR.drawable.stripe_ic_chevron_down),
                    contentDescription = stringResource(
                        if (expanded) {
                            R.string.stripe_currency_selector_hide_details
                        } else {
                            R.string.stripe_currency_selector_show_details
                        }
                    ),
                    tint = color,
                    modifier = Modifier.padding(start = 4.dp).size(16.dp)
                        .rotate(if (expanded) EXPANDED_DISCLOSURE_ROTATION else 0f),
                )
            }
        }
    }
    if (expanded && details != null) {
        Text(
            text = details,
            style = style,
            color = color,
            modifier = Modifier.padding(top = 4.dp).testTag(TEST_TAG_CURRENCY_SELECTOR_DETAILS),
        )
    }
}

@Composable
private fun RowScope.CurrencyOptionItem(
    currency: CurrencyOption,
    options: CurrencySelectorOptions,
    onCurrencySelected: (CurrencyOption) -> Unit,
    isEnabled: Boolean,
    showCurrencyCode: Boolean,
    pillBackground: Color,
    trackBackground: Color,
    selectedTextColor: Color,
    unselectedTextColor: Color,
    textStyle: TextStyle,
    contentVerticalPaddingDp: Float,
) {
    val isSelected = currency.code == options.selectedCode
    val backgroundColor = if (isSelected) pillBackground else trackBackground
    val textColor = if (isSelected) selectedTextColor else unselectedTextColor
    val accessibilityDescription = stringResource(
        if (isSelected) {
            ComposeR.string.selected
        } else {
            ComposeR.string.not_selected
        }
    )
    val accessibilityLabel = if (showCurrencyCode) currency.code else currency.formattedAmount

    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .weight(1f)
            .background(backgroundColor)
            .selectable(
                selected = isSelected,
                enabled = isEnabled,
                role = Role.RadioButton,
                onClick = {
                    if (!isSelected) {
                        onCurrencySelected(currency)
                    }
                },
            )
            .semantics {
                contentDescription = accessibilityLabel
                stateDescription = accessibilityDescription
            }
            .padding(vertical = contentVerticalPaddingDp.dp)
            .testTag("$TEST_TAG_CURRENCY_OPTION_PREFIX${currency.code}"),
    ) {
        CurrencyOptionContent(
            currency = currency,
            isSelected = isSelected,
            showCurrencyCode = showCurrencyCode,
            textColor = textColor,
            textStyle = textStyle,
        )
    }
}

@Composable
private fun CurrencyOptionContent(
    currency: CurrencyOption,
    isSelected: Boolean,
    showCurrencyCode: Boolean,
    textColor: Color,
    textStyle: TextStyle,
) {
    val displayText = if (showCurrencyCode) currency.code else currency.formattedAmount
    val annotatedText = buildAnnotatedString {
        when (val flag = currency.flag) {
            is FlagContent.Image -> {
                appendInlineContent("flag", "[flag]")
                append(" ")
            }
            is FlagContent.Emoji -> {
                append(flag.emoji)
                append(" ")
            }
        }
        append(displayText)
    }
    val inlineContent = when (val flag = currency.flag) {
        is FlagContent.Image -> mapOf(
            "flag" to InlineTextContent(
                Placeholder(
                    width = 1.2.em,
                    height = 1.2.em,
                    placeholderVerticalAlign = PlaceholderVerticalAlign.TextCenter,
                )
            ) {
                Image(
                    bitmap = flag.bitmap.asImageBitmap(),
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                )
            }
        )
        is FlagContent.Emoji -> emptyMap()
    }
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.clearAndSetSemantics {},
    ) {
        if (isSelected) {
            Icon(
                painter = painterResource(StripeUiCoreR.drawable.stripe_ic_checkmark),
                contentDescription = null,
                modifier = Modifier.size(16.dp),
                tint = textColor,
            )
            Spacer(modifier = Modifier.width(6.dp))
        }
        Text(
            text = annotatedText,
            inlineContent = inlineContent,
            style = textStyle,
            color = textColor,
            fontWeight = if (isSelected) FontWeight.Medium else null,
        )
    }
}

private fun TextStyle.withAppearance(fontFamily: FontFamily?, sizeScaleFactor: Float): TextStyle {
    return copy(
        fontFamily = fontFamily ?: this.fontFamily,
        fontSize = fontSize * sizeScaleFactor,
    )
}
