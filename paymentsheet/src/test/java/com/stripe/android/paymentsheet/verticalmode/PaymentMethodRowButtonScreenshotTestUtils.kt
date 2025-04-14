package com.stripe.android.paymentsheet.verticalmode

import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.stripe.android.paymentelement.ExperimentalEmbeddedPaymentElementApi
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.ui.PaymentMethodIconFromResource
import com.stripe.android.paymentsheet.verticalmode.UIConstants.iconHeight
import com.stripe.android.paymentsheet.verticalmode.UIConstants.iconWidth
import com.stripe.android.screenshottesting.PaparazziRule
import com.stripe.android.ui.core.R

@Composable
internal fun DefaultPaymentMethodRowIcon(
    iconRes: Int = R.drawable.stripe_ic_paymentsheet_pm_card
) {
    PaymentMethodIconFromResource(
        iconRes = iconRes,
        colorFilter = null,
        alignment = Alignment.Center,
        modifier = Modifier
            .height(iconHeight)
            .width(iconWidth)
    )
}

@OptIn(ExperimentalEmbeddedPaymentElementApi::class)
internal fun testPaymentMethodRowButton(
    isEnabled: Boolean,
    isSelected: Boolean,
    iconContent: @Composable RowScope.() -> Unit,
    rowStyle: PaymentSheet.Appearance.Embedded.RowStyle,
    trailingContent: @Composable RowScope.() -> Unit,
    title: String,
    subtitle: String?,
    promoText: String?,
    shouldShowDefaultBadge: Boolean,
    paparazziRule: PaparazziRule,
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
            style = rowStyle
        )
    }
}
