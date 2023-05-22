package com.stripe.android.paymentsheet.example.samples.ui.customer

import com.stripe.android.ExperimentalCustomerSheetApi
import com.stripe.android.paymentsheet.repositories.CustomerEphemeralKey

@OptIn(ExperimentalCustomerSheetApi::class)
sealed class CustomerViewState {
    object Loading : CustomerViewState()

    class FailedToLoad(val message: String) : CustomerViewState()

    @Suppress("unused")
    class Data(
        val customerEphemeralKey: CustomerEphemeralKey
    ) : CustomerViewState()
}
