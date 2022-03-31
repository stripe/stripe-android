package com.stripe.android.paymentsheet.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.stripe.android.paymentsheet.R
import com.stripe.android.ui.core.PaymentsTheme

@Composable
internal fun GooglePayDividerUi(
    text: String = stringResource(R.string.stripe_paymentsheet_or_pay_with_card)
) {
    Box(
        Modifier
            .background(PaymentsTheme.colors.material.surface)
            .padding(horizontal = 8.dp)
            .padding(top = 16.dp)
    ) {
        Text(
            text = text,
            style = PaymentsTheme.typography.body1,
            color = PaymentsTheme.colors.subtitle
        )
    }
}
