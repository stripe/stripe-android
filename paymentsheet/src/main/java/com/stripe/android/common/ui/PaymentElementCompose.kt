package com.stripe.android.common.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner
import com.stripe.android.paymentelement.callbacks.LifecyclePaymentElementCallbackReferences
import com.stripe.android.paymentelement.callbacks.PaymentElementCallbacks

@Composable
internal fun UpdateCallbacks(
    callbackReferences: LifecyclePaymentElementCallbackReferences,
    key: String,
    paymentElementCallbacks: PaymentElementCallbacks
) {
    LaunchedEffect(callbackReferences, key, paymentElementCallbacks) {
        callbackReferences[key] = paymentElementCallbacks
    }
}

@Composable
internal fun rememberCallbackReferences(key: String): LifecyclePaymentElementCallbackReferences {
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    val owner = requireNotNull(LocalViewModelStoreOwner.current) {
        "Payment Element callbacks require a ViewModelStoreOwner."
    }
    return remember(lifecycle, owner, key) {
        LifecyclePaymentElementCallbackReferences.get(lifecycle, owner, key)
    }
}
