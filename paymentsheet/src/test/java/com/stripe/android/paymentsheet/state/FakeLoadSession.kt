package com.stripe.android.paymentsheet.state

import com.stripe.android.common.model.CommonConfiguration
import com.stripe.android.paymentsheet.model.SavedSelection

internal class FakeLoadSession(
    var result: SessionResult,
) : LoadSession {

    var lastInitializationMode: PaymentElementLoader.InitializationMode? = null
        private set
    var lastConfiguration: CommonConfiguration? = null
        private set
    var lastSavedPaymentMethodSelection: SavedSelection.PaymentMethod? = null
        private set

    override suspend fun invoke(
        initializationMode: PaymentElementLoader.InitializationMode,
        configuration: CommonConfiguration,
        savedPaymentMethodSelection: SavedSelection.PaymentMethod?,
    ): SessionResult {
        lastInitializationMode = initializationMode
        lastConfiguration = configuration
        lastSavedPaymentMethodSelection = savedPaymentMethodSelection
        return result
    }
}
