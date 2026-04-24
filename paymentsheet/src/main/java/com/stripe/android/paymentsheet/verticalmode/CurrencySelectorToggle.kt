package com.stripe.android.paymentsheet.verticalmode

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.stripe.android.uicore.stripeColors
import com.stripe.android.uicore.stripeShapes

internal const val TEST_TAG_CURRENCY_SELECTOR = "TEST_TAG_CURRENCY_SELECTOR"

internal const val TEST_TAG_CURRENCY_OPTION_PREFIX = "TEST_TAG_CURRENCY_OPTION_"

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
    modifier: Modifier = Modifier,
) {
    val shape = MaterialTheme.stripeShapes.roundedCornerShape

    Column(modifier = modifier) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .alpha(if (isEnabled) 1f else DISABLED_ALPHA)
                .clip(shape)
                .background(MaterialTheme.colors.onSurface.copy(alpha = 0.08f))
                .padding(4.dp)
                .testTag(TEST_TAG_CURRENCY_SELECTOR),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            CurrencyOptionItem(options.first, options, onCurrencySelected, isEnabled)
            CurrencyOptionItem(options.second, options, onCurrencySelected, isEnabled)
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
    val segmentShape = MaterialTheme.stripeShapes.roundedCornerShape
    val backgroundColor = if (isSelected) {
        MaterialTheme.stripeColors.component
    } else {
        Color.Transparent
    }

    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .weight(1f)
            .clip(segmentShape)
            .background(backgroundColor)
            .selectable(
                selected = isSelected,
                enabled = isEnabled && !isSelected,
                onClick = { onCurrencySelected(currency) },
            )
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
