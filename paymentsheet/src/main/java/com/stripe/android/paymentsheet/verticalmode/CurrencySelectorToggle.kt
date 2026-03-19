package com.stripe.android.paymentsheet.verticalmode

import androidx.annotation.RestrictTo
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.stripe.android.uicore.stripeColors
import com.stripe.android.uicore.stripeShapes

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
const val TEST_TAG_CURRENCY_SELECTOR = "TEST_TAG_CURRENCY_SELECTOR"

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
const val TEST_TAG_CURRENCY_OPTION_PREFIX = "TEST_TAG_CURRENCY_OPTION_"

internal data class CurrencyOption(
    val code: String,
    val displayName: String,
)

@Composable
internal fun CurrencySelectorToggle(
    currencies: List<CurrencyOption>,
    selectedCurrency: CurrencyOption?,
    onCurrencySelected: (CurrencyOption) -> Unit,
    isEnabled: Boolean,
    modifier: Modifier = Modifier,
) {
    val shape = MaterialTheme.stripeShapes.roundedCornerShape
    val borderColor = MaterialTheme.stripeColors.componentBorder

    Box(
        modifier = modifier
            .fillMaxWidth()
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
            currencies.forEachIndexed { index, currency ->
                val isSelected = currency == selectedCurrency
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
                        .clickable(enabled = isEnabled) { onCurrencySelected(currency) }
                        .padding(vertical = 12.dp)
                        .testTag("$TEST_TAG_CURRENCY_OPTION_PREFIX${currency.code}"),
                ) {
                    Text(
                        text = currency.displayName,
                        style = MaterialTheme.typography.subtitle1,
                        color = MaterialTheme.stripeColors.onComponent,
                        fontWeight = if (isSelected) FontWeight.Medium else null,
                    )
                }

                if (index < currencies.lastIndex) {
                    Divider(
                        color = borderColor,
                        modifier = Modifier
                            .fillMaxHeight()
                            .width(MaterialTheme.stripeShapes.borderStrokeWidth.dp),
                    )
                }
            }
        }
    }
}
