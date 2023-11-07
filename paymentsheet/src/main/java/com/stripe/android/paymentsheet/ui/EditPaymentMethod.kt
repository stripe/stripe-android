package com.stripe.android.paymentsheet.ui

import androidx.compose.foundation.layout.padding
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.stripe.android.model.PaymentMethod

@Composable
internal fun EditPaymentMethod(
    paymentMethod: PaymentMethod,
    modifier: Modifier = Modifier,
) {
    Text(
        text = "Editing ${paymentMethod.getLabel(LocalContext.current.resources)}",
        modifier = modifier.padding(16.dp),
    )
}
