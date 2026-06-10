package com.stripe.android.paymentsheet.verticalmode

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.error
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.stripe.android.checkout.Checkout
import com.stripe.android.paymentelement.CheckoutSessionPreview
import com.stripe.android.paymentsheet.R
import androidx.compose.ui.R as ComposeR
import com.stripe.android.uicore.R as StripeUiCoreR

internal const val TEST_TAG_CURRENCY_SELECTOR = "TEST_TAG_CURRENCY_SELECTOR"

internal const val TEST_TAG_CURRENCY_OPTION_PREFIX = "TEST_TAG_CURRENCY_OPTION_"

internal const val TEST_TAG_CURRENCY_SELECTOR_ERROR = "TEST_TAG_CURRENCY_SELECTOR_ERROR"

private const val DISABLED_ALPHA = 0.6f

internal data class CurrencyOption(
    val code: String,
    val displayableText: String,
    val formattedAmount: String,
)

internal data class CurrencySelectorOptions(
    val first: CurrencyOption,
    val second: CurrencyOption,
    val selectedCode: String,
    val exchangeRateText: String? = null,
)

@OptIn(CheckoutSessionPreview::class)
@Suppress("LongMethod", "CyclomaticComplexMethod")
@Composable
internal fun CurrencySelectorToggle(
    options: CurrencySelectorOptions,
    onCurrencySelected: (CurrencyOption) -> Unit,
    isEnabled: Boolean,
    errorMessage: String? = null,
    appearance: Checkout.CurrencySelectorContentAppearance.State,
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
    val selectorLabel = stringResource(R.string.stripe_paymentsheet_currency_selector_label)

    Column(modifier = modifier) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .alpha(if (isEnabled) 1f else DISABLED_ALPHA)
                .clip(shape)
                .background(trackBackground)
                .border(
                    width = borderWidth,
                    color = borderColor,
                    shape = shape,
                )
                .semantics {
                    contentDescription = selectorLabel
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
                    pillBackground = pillBackground,
                    trackBackground = trackBackground,
                    selectedTextColor = selectedText,
                    unselectedTextColor = unselectedText,
                    textStyle = bodyStyle,
                    contentVerticalPaddingDp = appearance.contentVerticalPaddingDp,
                    exchangeRateText = options.exchangeRateText,
                    errorMessage = errorMessage,
                )
                CurrencyOptionItem(
                    currency = options.second,
                    options = options,
                    onCurrencySelected = onCurrencySelected,
                    isEnabled = isEnabled,
                    pillBackground = pillBackground,
                    trackBackground = trackBackground,
                    selectedTextColor = selectedText,
                    unselectedTextColor = unselectedText,
                    textStyle = bodyStyle,
                    contentVerticalPaddingDp = appearance.contentVerticalPaddingDp,
                    exchangeRateText = options.exchangeRateText,
                    errorMessage = errorMessage,
                )
            }
        }
        if (options.exchangeRateText != null) {
            Text(
                text = options.exchangeRateText,
                style = captionStyle,
                color = captionColor,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp)
                    .semantics { liveRegion = LiveRegionMode.Polite },
            )
        }
        if (errorMessage != null) {
            Text(
                text = errorMessage,
                style = captionStyle,
                color = errorColor,
                textAlign = TextAlign.Center,
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
private fun RowScope.CurrencyOptionItem(
    currency: CurrencyOption,
    options: CurrencySelectorOptions,
    onCurrencySelected: (CurrencyOption) -> Unit,
    isEnabled: Boolean,
    pillBackground: Color,
    trackBackground: Color,
    selectedTextColor: Color,
    unselectedTextColor: Color,
    textStyle: TextStyle,
    contentVerticalPaddingDp: Float,
    exchangeRateText: String?,
    errorMessage: String?,
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
    val accessibilityLabel = currencyOptionAccessibilityLabel(
        currency = currency,
        isSelected = isSelected,
        exchangeRateText = exchangeRateText,
    )

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
                if (errorMessage != null) {
                    error(errorMessage)
                }
            }
            .padding(vertical = contentVerticalPaddingDp.dp)
            .testTag("$TEST_TAG_CURRENCY_OPTION_PREFIX${currency.code}"),
    ) {
        CurrencyOptionContent(
            currency = currency,
            isSelected = isSelected,
            textColor = textColor,
            textStyle = textStyle,
        )
    }
}

@Composable
private fun currencyOptionAccessibilityLabel(
    currency: CurrencyOption,
    isSelected: Boolean,
    exchangeRateText: String?,
): String {
    val currencyAccessibilityText = stringResource(
        R.string.stripe_paymentsheet_currency_option_accessibility,
        currency.code,
        currency.formattedAmount,
    )
    return if (isSelected && exchangeRateText != null) {
        stringResource(
            R.string.stripe_paymentsheet_currency_option_with_exchange_rate,
            currencyAccessibilityText,
            exchangeRateText,
        )
    } else {
        currencyAccessibilityText
    }
}

@Composable
private fun CurrencyOptionContent(
    currency: CurrencyOption,
    isSelected: Boolean,
    textColor: Color,
    textStyle: TextStyle,
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
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
            text = currency.displayableText,
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
