@file:RestrictTo(RestrictTo.Scope.LIBRARY)

package com.stripe.android.paymentsheet.utils

import androidx.annotation.RestrictTo
import com.stripe.android.paymentsheet.model.PaymentSelection
import com.stripe.android.paymentsheet.state.PaymentElementLoader

internal fun PaymentSelection.New.canSave(
    initializationMode: PaymentElementLoader.InitializationMode
): Boolean {
    val requestedToSave = customerRequestedSave == PaymentSelection.CustomerRequestedSave.RequestReuse

    return when (initializationMode) {
        is PaymentElementLoader.InitializationMode.PaymentIntent -> requestedToSave
        is PaymentElementLoader.InitializationMode.SetupIntent -> true
        is PaymentElementLoader.InitializationMode.DeferredIntent -> {
            initializationMode.intentConfiguration.mode.setupFutureUse != null || requestedToSave
        }
    }
}
