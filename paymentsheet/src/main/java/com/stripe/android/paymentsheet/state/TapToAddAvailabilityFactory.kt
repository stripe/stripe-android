package com.stripe.android.paymentsheet.state

import com.stripe.android.common.taptoadd.TapToAddConnectionManager
import com.stripe.android.lpmfoundations.paymentmethod.CustomerMetadata
import com.stripe.android.model.ElementsSession
import com.stripe.android.paymentelement.TapToAddPreview
import javax.inject.Inject

internal interface TapToAddAvailabilityFactory {
    fun isAvailable(
        elementsSession: ElementsSession,
        customerMetadata: CustomerMetadata?,
        publishableKey: String,
        isLiveMode: Boolean,
    ): Boolean
}

@OptIn(TapToAddPreview::class)
internal class DefaultTapToAddAvailabilityFactory @Inject constructor(
    private val connectionManager: TapToAddConnectionManager,
) : TapToAddAvailabilityFactory {
    override fun isAvailable(
        elementsSession: ElementsSession,
        customerMetadata: CustomerMetadata?,
        publishableKey: String,
        isLiveMode: Boolean,
    ): Boolean {
        return connectionManager.isSupported(publishableKey, isLiveMode) &&
            elementsSession.isTapToAddEnabled &&
            customerMetadata != null
    }
}

internal class TapToAddAvailabilityFactoryForCustomerSheet @Inject constructor() : TapToAddAvailabilityFactory {
    override fun isAvailable(
        elementsSession: ElementsSession,
        customerMetadata: CustomerMetadata?,
        publishableKey: String,
        isLiveMode: Boolean,
    ) = false
}
