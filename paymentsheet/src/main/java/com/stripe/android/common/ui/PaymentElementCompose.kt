package com.stripe.android.common.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.snapshotFlow
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.stripe.android.paymentelement.callbacks.PaymentElementCallbackReferences
import com.stripe.android.paymentelement.callbacks.PaymentElementCallbacks

@Composable
internal fun UpdateCallbacks(
    paymentElementCallbackIdentifier: String,
    paymentElementCallbacks: PaymentElementCallbacks
) {
    val lifecycleOwner = LocalLifecycleOwner.current
    val currentCallbacks by rememberUpdatedState(paymentElementCallbacks)

    LaunchedEffect(paymentElementCallbackIdentifier, lifecycleOwner.lifecycle) {
        val registration = PaymentElementCallbackReferences.register(
            key = paymentElementCallbackIdentifier,
            owner = lifecycleOwner,
            callbacks = currentCallbacks,
        )
        snapshotFlow { currentCallbacks }.collect(registration::update)
    }
}
