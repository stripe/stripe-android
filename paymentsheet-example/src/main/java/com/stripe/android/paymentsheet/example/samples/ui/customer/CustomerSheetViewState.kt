package com.stripe.android.paymentsheet.example.samples.ui.customer

import com.stripe.android.customersheet.CustomerEphemeralKey
import com.stripe.android.customersheet.ExperimentalCustomerSheetApi

@OptIn(ExperimentalCustomerSheetApi::class)
sealed class CustomerSheetViewState {
    object Loading : CustomerSheetViewState()

    class FailedToLoad(val message: String) : CustomerSheetViewState()

    @Suppress("unused")
    class Data(
        val customerEphemeralKey: CustomerEphemeralKey
    ) : CustomerSheetViewState()
}
