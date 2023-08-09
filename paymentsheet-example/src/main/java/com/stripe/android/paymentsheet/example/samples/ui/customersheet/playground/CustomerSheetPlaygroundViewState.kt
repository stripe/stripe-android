package com.stripe.android.paymentsheet.example.samples.ui.customersheet.playground

import com.stripe.android.customersheet.ExperimentalCustomerSheetApi
import com.stripe.android.customersheet.PaymentOptionSelection

@OptIn(ExperimentalCustomerSheetApi::class)
sealed class CustomerSheetPlaygroundViewState {
    object Loading : CustomerSheetPlaygroundViewState()
    data class FailedToLoad(val message: String) : CustomerSheetPlaygroundViewState()
    data class Data(
        val selection: PaymentOptionSelection? = null,
        val errorMessage: String? = null,
        val currentCustomer: String? = null,
    ) : CustomerSheetPlaygroundViewState()

    val currentCustomerId: String
        get() = (this as? Data)?.currentCustomer ?: "returning"
}
