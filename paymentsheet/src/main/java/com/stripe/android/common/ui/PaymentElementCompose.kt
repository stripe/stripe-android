package com.stripe.android.common.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import com.stripe.android.paymentelement.callbacks.PaymentElementCallbackReferences
import com.stripe.android.paymentelement.callbacks.PaymentElementCallbacks

@Composable
internal fun UpdateCallbacks(
    paymentElementCallbackIdentifier: String,
    paymentElementCallbacks: PaymentElementCallbacks
) {
    LaunchedEffect(paymentElementCallbackIdentifier, paymentElementCallbacks) {
        PaymentElementCallbackReferences[paymentElementCallbackIdentifier] = paymentElementCallbacks
    }
}
