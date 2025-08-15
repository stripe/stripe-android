@file:RestrictTo(RestrictTo.Scope.LIBRARY)

package com.stripe.android.paymentsheet.utils

import androidx.annotation.RestrictTo
import com.stripe.android.model.PaymentMethodExtraParams
import com.stripe.android.paymentsheet.model.PaymentSelection
import com.stripe.android.paymentsheet.state.PaymentElementLoader

internal fun PaymentSelection.New.canSave(
    initializationMode: PaymentElementLoader.InitializationMode,
    forceSave: Boolean = false,
): Boolean {
    val requestedToSave = customerRequestedSave == PaymentSelection.CustomerRequestedSave.RequestReuse

    return when (initializationMode) {
        is PaymentElementLoader.InitializationMode.PaymentIntent -> requestedToSave || forceSave
        is PaymentElementLoader.InitializationMode.SetupIntent -> true
        is PaymentElementLoader.InitializationMode.DeferredIntent -> {
            initializationMode.intentConfiguration.mode.setupFutureUse != null || requestedToSave || forceSave
        }
    }
}

internal fun PaymentSelection.getSetAsDefaultPaymentMethodFromPaymentSelection(): Boolean? {
    return when (this) {
        is PaymentSelection.New.Card -> {
            (this.paymentMethodExtraParams as? PaymentMethodExtraParams.Card)?.setAsDefault
        }
        is PaymentSelection.New.USBankAccount -> {
            (this.paymentMethodExtraParams as? PaymentMethodExtraParams.USBankAccount)?.setAsDefault
        }
        else -> {
            null
        }
    }
}
