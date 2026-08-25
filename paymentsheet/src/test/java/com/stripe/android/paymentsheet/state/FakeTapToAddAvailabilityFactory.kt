package com.stripe.android.paymentsheet.state

import com.stripe.android.lpmfoundations.paymentmethod.CustomerMetadata
import com.stripe.android.model.ElementsSession

internal class FakeTapToAddAvailabilityFactory(
    private val isAvailableResult: Boolean,
) : TapToAddAvailabilityFactory {
    override fun isAvailable(
        elementsSession: ElementsSession,
        customerMetadata: CustomerMetadata?,
    ): Boolean = isAvailableResult
}
