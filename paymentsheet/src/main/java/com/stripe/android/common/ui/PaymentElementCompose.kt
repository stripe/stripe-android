package com.stripe.android.common.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import com.stripe.android.paymentelement.callbacks.PaymentElementCallbackReferences
import com.stripe.android.paymentelement.callbacks.PaymentElementCallbacks
import com.stripe.android.paymentsheet.Identifiable

@Composable
internal fun UpdateCallbacks(
    paymentElementCallbackIdentifier: Identifiable,
    paymentElementCallbacks: PaymentElementCallbacks
) {
    LaunchedEffect(paymentElementCallbackIdentifier, paymentElementCallbacks) {
        PaymentElementCallbackReferences[paymentElementCallbackIdentifier] = paymentElementCallbacks
    }
}
