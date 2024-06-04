package com.stripe.android.paymentsheet.example.samples.ui.customersheet

import com.stripe.android.customersheet.CustomerEphemeralKey
import com.stripe.android.customersheet.ExperimentalCustomerSheetApi
import com.stripe.android.customersheet.PaymentOptionSelection

@OptIn(ExperimentalCustomerSheetApi::class)
sealed class CustomerSheetExampleViewState {
    object Loading : CustomerSheetExampleViewState()

    class FailedToLoad(val message: String) : CustomerSheetExampleViewState()

    @Suppress("unused")
    data class Data(
        val customerEphemeralKey: CustomerEphemeralKey,
        val selection: PaymentOptionSelection? = null,
        val errorMessage: String? = null
    ) : CustomerSheetExampleViewState()
}
