package com.stripe.android.paymentsheet.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.stripe.android.paymentsheet.model.PaymentMethodIncentive
import com.stripe.android.paymentsheet.R
import com.stripe.android.uicore.StripeTheme
import com.stripe.android.uicore.image.StripeImageLoader
import com.stripe.android.uicore.stripeColors

private object PaymentMethodUISpacing {
    val cardPadding = 12.dp
    val iconSize = 16.dp
}

@Composable
internal fun NewPaymentMethodTab(
    minWidth: Dp,
    iconRes: Int,
    iconUrl: String?,
    imageLoader: StripeImageLoader,
    title: String,
    isSelected: Boolean,
    isEnabled: Boolean,
    iconRequiresTinting: Boolean,
    modifier: Modifier = Modifier,
    incentive: PaymentMethodIncentive?,
    onItemSelectedListener: () -> Unit,
) {
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
            modifier = Modifier
                // This is an annoying workaround to make sure the incentive badge is aligned correctly
                .widthIn(min = minWidth - PaymentMethodUISpacing.cardPadding * 2)
                .fillMaxWidth(),
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

            incentive?.Content(tinyMode = true)
        }

        LpmSelectorText(
            text = title,
            isEnabled = isEnabled,
            textColor = MaterialTheme.stripeColors.onComponent,
            modifier = Modifier.padding(top = 6.dp),
        )
    }
}

@Preview
@Composable
private fun NewPaymentMethodTabPreview() {
    StripeTheme {
        NewPaymentMethodTab(
            minWidth = 100.dp,
            iconRes = R.drawable.stripe_ic_paymentsheet_bank,
            iconUrl = null,
            imageLoader = StripeImageLoader(
                context = LocalContext.current,
            ),
            title = "Bank",
            isSelected = false,
            isEnabled = true,
            iconRequiresTinting = false,
            modifier = Modifier,
            incentive = null,
            onItemSelectedListener = {},
        )
    }
}
