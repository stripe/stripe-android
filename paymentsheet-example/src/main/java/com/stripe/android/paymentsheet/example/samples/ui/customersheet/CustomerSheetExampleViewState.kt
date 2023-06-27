package com.stripe.android.paymentsheet.example.samples.ui.customersheet

import com.stripe.android.customersheet.CustomerEphemeralKey
import com.stripe.android.customersheet.CustomerSheetResult
import com.stripe.android.customersheet.ExperimentalCustomerSheetApi

@OptIn(ExperimentalCustomerSheetApi::class)
sealed class CustomerSheetExampleViewState {
    object Loading : CustomerSheetExampleViewState()

    class FailedToLoad(val message: String) : CustomerSheetExampleViewState()

    @Suppress("unused")
    data class Data(
        val customerEphemeralKey: CustomerEphemeralKey,
        val result: CustomerSheetResult? = null,
    ) : CustomerSheetExampleViewState()
}
