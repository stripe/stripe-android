package com.stripe.android.paymentsheet.verticalmode

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.Divider
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.stripe.android.uicore.stripeColors
import com.stripe.android.uicore.stripeShapes

internal const val TEST_TAG_CURRENCY_SELECTOR = "TEST_TAG_CURRENCY_SELECTOR"

internal const val TEST_TAG_CURRENCY_OPTION_PREFIX = "TEST_TAG_CURRENCY_OPTION_"

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
    modifier: Modifier = Modifier,
) {
    val shape = MaterialTheme.stripeShapes.roundedCornerShape
    val borderColor = MaterialTheme.stripeColors.componentBorder

    Column(modifier = modifier) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .alpha(if (isEnabled) 1.0f else 0.6f)
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
                Divider(
                    color = borderColor,
                    modifier = Modifier
                        .fillMaxHeight()
                        .width(MaterialTheme.stripeShapes.borderStrokeWidth.dp),
                )
                CurrencyOptionItem(options.second, options, onCurrencySelected, isEnabled)
            }
        }
        if (options.exchangeRateText != null) {
            Text(
                text = options.exchangeRateText,
                style = MaterialTheme.typography.caption,
                color = MaterialTheme.stripeColors.subtitle,
                modifier = Modifier.padding(top = 4.dp),
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
        MaterialTheme.colors.primary.copy(alpha = 0.12f)
    } else {
        MaterialTheme.stripeColors.component
    }

    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .weight(1f)
            .background(backgroundColor)
            .clickable(enabled = isEnabled && !isSelected) { onCurrencySelected(currency) }
            .padding(vertical = 12.dp)
            .testTag("$TEST_TAG_CURRENCY_OPTION_PREFIX${currency.code}"),
    ) {
        Text(
            text = currency.displayableText,
            style = MaterialTheme.typography.subtitle1,
            color = MaterialTheme.stripeColors.onComponent,
            fontWeight = if (isSelected) FontWeight.Medium else null,
        )
    }
}
