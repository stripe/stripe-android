package com.stripe.android.common.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.stripe.android.paymentelement.callbacks.LifecyclePaymentElementCallbackReferences
import com.stripe.android.paymentelement.callbacks.PaymentElementCallbacks

@Composable
internal fun UpdateCallbacks(
    paymentElementCallbackIdentifier: String,
    paymentElementCallbacks: PaymentElementCallbacks
) {
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    val callbackReferences = remember(lifecycle) {
        LifecyclePaymentElementCallbackReferences(lifecycle)
    }
    LaunchedEffect(callbackReferences, paymentElementCallbackIdentifier, paymentElementCallbacks) {
        callbackReferences[paymentElementCallbackIdentifier] = paymentElementCallbacks
    }
}
