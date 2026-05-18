package com.stripe.android.paymentsheet.state

import com.stripe.android.common.taptoadd.TapToAddConnectionManager
import com.stripe.android.lpmfoundations.paymentmethod.CustomerMetadata
import com.stripe.android.model.ElementsSession
import com.stripe.android.paymentelement.TapToAddPreview
import com.stripe.android.paymentelement.callbacks.PaymentElementCallbackIdentifier
import com.stripe.android.paymentelement.callbacks.PaymentElementCallbackReferences
import javax.inject.Inject

internal interface TapToAddAvailabilityFactory {
    fun isAvailable(
        elementsSession: ElementsSession,
        customerMetadata: CustomerMetadata?,
    ): Boolean
}

@OptIn(TapToAddPreview::class)
internal class DefaultTapToAddAvailabilityFactory @Inject constructor(
    private val connectionManager: TapToAddConnectionManager,
    @PaymentElementCallbackIdentifier private val paymentElementCallbackIdentifier: String,

) : TapToAddAvailabilityFactory {
    override fun isAvailable(
        elementsSession: ElementsSession,
        customerMetadata: CustomerMetadata?,
    ): Boolean {
        return connectionManager.isSupported &&
            elementsSession.isTapToAddEnabled &&
            customerMetadata != null &&
            PaymentElementCallbackReferences[paymentElementCallbackIdentifier]
                ?.createCardPresentSetupIntentCallback != null
    }
}
