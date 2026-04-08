package com.stripe.android.paymentsheet.state

import com.stripe.android.common.taptoadd.TapToAddConnectionManager
import com.stripe.android.lpmfoundations.paymentmethod.CustomerMetadata
import com.stripe.android.model.ElementsSession
import javax.inject.Inject

internal interface TapToAddAvailabilityFactory {
    fun isAvailable(
        elementsSession: ElementsSession,
        customerMetadata: CustomerMetadata?,
    ): Boolean
}

internal class DefaultTapToAddAvailabilityFactory @Inject constructor(
    private val connectionManager: TapToAddConnectionManager,
) : TapToAddAvailabilityFactory {
    override fun isAvailable(
        elementsSession: ElementsSession,
        customerMetadata: CustomerMetadata?,
    ): Boolean {
        return connectionManager.isSupported &&
            elementsSession.isTapToAddEnabled &&
            customerMetadata != null
    }
}
