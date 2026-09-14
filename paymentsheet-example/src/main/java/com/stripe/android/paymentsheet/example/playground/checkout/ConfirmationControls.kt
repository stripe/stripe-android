@file:OptIn(com.stripe.android.paymentelement.CheckoutSessionPreview::class)

package com.stripe.android.paymentsheet.example.playground.checkout

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.material.Button
import androidx.compose.material.MaterialTheme
import androidx.compose.material.OutlinedButton
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.stripe.android.checkout.CheckoutController.Session.PaymentOptionDisplayData

@Composable
internal fun ConfirmationControls(
    paymentOption: PaymentOptionDisplayData?,
    confirmationMessage: String?,
    isUpdating: Boolean,
    displayMandate: Boolean,
    onClearPaymentMethod: () -> Unit,
    onSelectPaymentMethod: () -> Unit,
    onSetShippingAddress: () -> Unit,
    onConfirm: () -> Unit,
) {
    PaymentOptionRow(paymentOption)
    if (displayMandate) {
        paymentOption?.mandateText?.let { Text(text = it, style = MaterialTheme.typography.caption) }
    }
    confirmationMessage?.let { message ->
        Text(text = message, color = MaterialTheme.colors.error)
    }
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedButton(
            onClick = onClearPaymentMethod,
            enabled = !isUpdating && paymentOption != null,
            modifier = Modifier.weight(1f),
        ) { Text("Clear") }
        Button(
            onClick = onSelectPaymentMethod,
            enabled = !isUpdating,
            modifier = Modifier.weight(1f),
        ) { Text("Select") }
    }
    Button(
        onClick = onSetShippingAddress,
        modifier = Modifier.fillMaxWidth(),
    ) { Text("Set shipping address (WIP)") }
    Button(
        onClick = onConfirm,
        enabled = !isUpdating,
        modifier = Modifier.fillMaxWidth(),
    ) { Text("Confirm") }
}

@Composable
private fun PaymentOptionRow(paymentOption: PaymentOptionDisplayData?) {
    if (paymentOption == null) {
        Text("No payment method selected", style = MaterialTheme.typography.body2, color = Color.Gray)
        return
    }
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Image(painter = paymentOption.iconPainter, contentDescription = null, modifier = Modifier.size(32.dp))
        Text(paymentOption.label)
    }
}
