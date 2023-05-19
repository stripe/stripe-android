package com.stripe.android.paymentsheet.example.samples.ui.wallet

import com.stripe.android.ExperimentalSavedPaymentMethodsApi
import com.stripe.android.paymentsheet.repositories.CustomerEphemeralKey

@OptIn(ExperimentalSavedPaymentMethodsApi::class)
sealed class SavedPaymentMethodsViewState {
    object Loading : SavedPaymentMethodsViewState()

    class FailedToLoad(val message: String) : SavedPaymentMethodsViewState()

    @Suppress("unused")
    class Data(
        val customerEphemeralKey: CustomerEphemeralKey
    ) : SavedPaymentMethodsViewState()
}
