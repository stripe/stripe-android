package com.stripe.android.paymentsheet.verticalmode

import androidx.compose.foundation.layout.RowScope
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.ui.PaymentMethodIconFromResource
import com.stripe.android.screenshottesting.PaparazziRule
import com.stripe.android.ui.core.R

@Composable
internal fun DefaultPaymentMethodRowIcon(
    iconRes: Int = R.drawable.stripe_ic_paymentsheet_pm_card,
    cardArtEnabled: Boolean = false,
) {
    PaymentMethodIconFromResource(
        iconRes = iconRes,
        colorFilter = null,
        alignment = Alignment.Center,
        modifier = Modifier
            .paymentMethodIconSize(cardArtEnabled)
    )
}

internal fun testPaymentMethodRowButton(
    isEnabled: Boolean,
    isSelected: Boolean,
    iconContent: @Composable RowScope.() -> Unit,
    appearance: PaymentSheet.Appearance.Embedded,
    trailingContent: @Composable RowScope.() -> Unit,
    title: String,
    subtitle: String?,
    promoText: String?,
    shouldShowDefaultBadge: Boolean,
    paparazziRule: PaparazziRule,
    cardArtEnabled: Boolean = false,
) {
    paparazziRule.snapshot {
        PaymentMethodRowButton(
            isEnabled = isEnabled,
            isSelected = isSelected,
            iconContent = iconContent,
            title = title,
            subtitle = subtitle,
            promoText = promoText,
            onClick = {},
            trailingContent = trailingContent,
            shouldShowDefaultBadge = shouldShowDefaultBadge,
            appearance = appearance,
            cardArtEnabled = cardArtEnabled,
        )
    }
}
