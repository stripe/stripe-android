@file:RestrictTo(RestrictTo.Scope.LIBRARY)

package com.stripe.android.paymentsheet.utils

import androidx.annotation.RestrictTo
import com.stripe.android.model.PaymentMethod
import com.stripe.android.model.PaymentMethodCreateParams
import com.stripe.android.model.PaymentMethodExtraParams
import com.stripe.android.paymentelement.PaymentMethodOptionsSetupFutureUsagePreview
import com.stripe.android.paymentsheet.PaymentSheet
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
            requestedToSave || canSaveIfNotRequested(initializationMode.intentConfiguration)
        }
    }
}

private fun PaymentSelection.New.canSaveIfNotRequested(
    intentConfiguration: PaymentSheet.IntentConfiguration
): Boolean {
    return intentConfiguration.mode.setupFutureUse.isSavable() || intentConfiguration.run {
        when (mode) {
            is PaymentSheet.IntentConfiguration.Mode.Payment -> mode.canSave(paymentMethodCreateParams)
            is PaymentSheet.IntentConfiguration.Mode.Setup -> false
        }
    }
}

@OptIn(PaymentMethodOptionsSetupFutureUsagePreview::class)
private fun PaymentSheet.IntentConfiguration.Mode.Payment.canSave(
    createParams: PaymentMethodCreateParams
): Boolean {
    return paymentMethodOptions?.setupFutureUsageValues?.let { setupFutureUsageValues ->
        PaymentMethod.Type.fromCode(createParams.typeCode)?.let {
            setupFutureUsageValues[it].isSavable()
        } ?: false
    } ?: false
}

private fun PaymentSheet.IntentConfiguration.SetupFutureUse?.isSavable() = when (this) {
    PaymentSheet.IntentConfiguration.SetupFutureUse.OffSession,
    PaymentSheet.IntentConfiguration.SetupFutureUse.OnSession -> true
    PaymentSheet.IntentConfiguration.SetupFutureUse.None,
    null -> false
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
