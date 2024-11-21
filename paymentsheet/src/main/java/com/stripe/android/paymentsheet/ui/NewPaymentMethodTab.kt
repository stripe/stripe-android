package com.stripe.android.paymentsheet.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.stripe.android.uicore.image.StripeImageLoader
import com.stripe.android.uicore.stripeColors

private object PaymentMethodUISpacing {
    val cardPadding = 12.dp
    val iconSize = 16.dp
}

@Composable
internal fun NewPaymentMethodTab(
    minViewWidth: Dp,
    iconRes: Int,
    iconUrl: String?,
    imageLoader: StripeImageLoader,
    title: String,
    isSelected: Boolean,
    isEnabled: Boolean,
    iconRequiresTinting: Boolean,
    promoBadge: String?,
    modifier: Modifier = Modifier,
    onItemSelectedListener: () -> Unit
) {
    val minContentWidth = minViewWidth - (PaymentMethodUISpacing.cardPadding * 2)

    RowButton(
        isEnabled = isEnabled,
        isSelected = isSelected,
        onClick = onItemSelectedListener,
        contentPaddingValues = PaddingValues(
            start = PaymentMethodUISpacing.cardPadding,
            end = PaymentMethodUISpacing.cardPadding,
            top = PaymentMethodUISpacing.cardPadding,
        ),
        verticalArrangement = Arrangement.Top,
        modifier = modifier.height(60.dp),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.widthIn(
                min = minContentWidth,
            ),
        ) {
            PaymentMethodIcon(
                iconRes = iconRes,
                iconUrl = iconUrl,
                imageLoader = imageLoader,
                iconRequiresTinting = iconRequiresTinting,
                contentAlignment = Alignment.CenterStart,
                modifier = Modifier
                    .height(PaymentMethodUISpacing.iconSize)
                    .widthIn(max = 36.dp),
            )

            promoBadge?.let {
                PromoBadge(text = it, tinyMode = true)
            }
        }

        LpmSelectorText(
            text = title,
            isEnabled = isEnabled,
            textColor = MaterialTheme.stripeColors.onComponent,
            modifier = Modifier.padding(top = 6.dp),
        )
    }
}
