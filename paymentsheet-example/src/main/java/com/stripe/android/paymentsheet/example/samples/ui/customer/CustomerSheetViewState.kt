package com.stripe.android.paymentsheet.example.samples.ui.customer

import com.stripe.android.customersheet.ExperimentalCustomerSheetApi
import com.stripe.android.customersheet.repositories.CustomerEphemeralKey

@OptIn(ExperimentalCustomerSheetApi::class)
sealed class CustomerSheetViewState {
    object Loading : CustomerSheetViewState()

    class FailedToLoad(val message: String) : CustomerSheetViewState()

    @Suppress("unused")
    class Data(
        val customerEphemeralKey: CustomerEphemeralKey
    ) : CustomerSheetViewState()
}
