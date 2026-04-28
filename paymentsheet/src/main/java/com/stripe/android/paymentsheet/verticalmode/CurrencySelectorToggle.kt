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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.stripe.android.uicore.stripeColors
import com.stripe.android.uicore.stripeShapes
import com.stripe.android.uicore.R as StripeUiCoreR

internal const val TEST_TAG_CURRENCY_SELECTOR = "TEST_TAG_CURRENCY_SELECTOR"

internal const val TEST_TAG_CURRENCY_OPTION_PREFIX = "TEST_TAG_CURRENCY_OPTION_"

internal const val TEST_TAG_CURRENCY_SELECTOR_ERROR = "TEST_TAG_CURRENCY_SELECTOR_ERROR"

private const val DISABLED_ALPHA = 0.6f

internal data class CurrencyOption(
    val code: String,
    val displayableText: String,
)

internal data class CurrencySelectorOptions(
    val first: CurrencyOption,
    val second: CurrencyOption,
    val selectedCode: String,
    val exchangeRateText: String? = null,
)

@Composable
internal fun CurrencySelectorToggle(
    options: CurrencySelectorOptions,
    onCurrencySelected: (CurrencyOption) -> Unit,
    isEnabled: Boolean,
    errorMessage: String? = null,
    modifier: Modifier = Modifier,
) {
    val shape = RoundedCornerShape(percent = 50)
    val borderColor = MaterialTheme.stripeColors.componentBorder

    Column(modifier = modifier) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .alpha(if (isEnabled) 1f else DISABLED_ALPHA)
                .clip(shape)
                .border(
                    width = MaterialTheme.stripeShapes.borderStrokeWidth.dp,
                    color = borderColor,
                    shape = shape,
                )
                .testTag(TEST_TAG_CURRENCY_SELECTOR),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(IntrinsicSize.Min),
            ) {
                CurrencyOptionItem(options.first, options, onCurrencySelected, isEnabled)
                CurrencyOptionItem(options.second, options, onCurrencySelected, isEnabled)
            }
        }
        if (options.exchangeRateText != null) {
            Text(
                text = options.exchangeRateText,
                style = MaterialTheme.typography.caption,
                color = MaterialTheme.stripeColors.subtitle,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp),
            )
        }
        if (errorMessage != null) {
            Text(
                text = errorMessage,
                style = MaterialTheme.typography.caption,
                color = MaterialTheme.colors.error,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp)
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
) {
    val isSelected = currency.code == options.selectedCode
    val backgroundColor = if (isSelected) {
        MaterialTheme.colors.primary
    } else {
        MaterialTheme.stripeColors.component
    }

    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .weight(1f)
            .background(backgroundColor)
            .selectable(
                selected = isSelected,
                enabled = isEnabled && !isSelected,
                onClick = { onCurrencySelected(currency) },
            )
            .padding(vertical = 4.dp)
            .testTag("$TEST_TAG_CURRENCY_OPTION_PREFIX${currency.code}"),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (isSelected) {
                Icon(
                    painter = painterResource(StripeUiCoreR.drawable.stripe_ic_checkmark),
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = MaterialTheme.colors.onPrimary,
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = currency.displayableText,
                    style = MaterialTheme.typography.subtitle1,
                    color = MaterialTheme.colors.onPrimary,
                    fontWeight = if (isSelected) FontWeight.Medium else null,
                )
            } else {
                Text(
                    text = currency.displayableText,
                    style = MaterialTheme.typography.subtitle1,
                    color = MaterialTheme.stripeColors.onComponent,
                    fontWeight = if (isSelected) FontWeight.Medium else null,
                )
            }
        }
    }
}
