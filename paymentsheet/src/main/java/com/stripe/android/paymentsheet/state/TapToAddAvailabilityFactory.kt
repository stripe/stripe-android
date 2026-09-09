package com.stripe.android.paymentsheet.state

import com.stripe.android.common.taptoadd.TapToAddConnectionManager
import com.stripe.android.core.ApiConfiguration
import com.stripe.android.lpmfoundations.paymentmethod.CustomerMetadata
import com.stripe.android.model.ElementsSession
import com.stripe.android.paymentelement.TapToAddPreview
import javax.inject.Inject

internal interface TapToAddAvailabilityFactory {
    fun isAvailable(
        elementsSession: ElementsSession,
        customerMetadata: CustomerMetadata?,
        apiConfiguration: ApiConfiguration.State,
    ): Boolean
}

@OptIn(TapToAddPreview::class)
internal class DefaultTapToAddAvailabilityFactory @Inject constructor(
    private val connectionManager: TapToAddConnectionManager,
) : TapToAddAvailabilityFactory {
    override fun isAvailable(
        elementsSession: ElementsSession,
        customerMetadata: CustomerMetadata?,
        apiConfiguration: ApiConfiguration.State,
    ): Boolean {
        return connectionManager.isSupported(apiConfiguration) &&
            elementsSession.isTapToAddEnabled &&
            customerMetadata != null
    }
}

internal class TapToAddAvailabilityFactoryForCustomerSheet @Inject constructor() : TapToAddAvailabilityFactory {
    override fun isAvailable(
        elementsSession: ElementsSession,
        customerMetadata: CustomerMetadata?,
        apiConfiguration: ApiConfiguration.State,
    ) = false
}
