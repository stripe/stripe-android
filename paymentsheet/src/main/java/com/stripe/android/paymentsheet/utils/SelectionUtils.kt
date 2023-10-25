@file:RestrictTo(RestrictTo.Scope.LIBRARY)

package com.stripe.android.paymentsheet.utils

import androidx.annotation.RestrictTo
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.model.PaymentSelection

internal fun PaymentSelection.New.canSave(
    initializationMode: PaymentSheet.InitializationMode
): Boolean {
    val requestedToSave = customerRequestedSave == PaymentSelection.CustomerRequestedSave.RequestReuse

    return when (initializationMode) {
        is PaymentSheet.InitializationMode.PaymentIntent -> requestedToSave
        is PaymentSheet.InitializationMode.SetupIntent -> true
        is PaymentSheet.InitializationMode.DeferredIntent -> {
            initializationMode.intentConfiguration.mode.setupFutureUse != null || requestedToSave
        }
    }
}
