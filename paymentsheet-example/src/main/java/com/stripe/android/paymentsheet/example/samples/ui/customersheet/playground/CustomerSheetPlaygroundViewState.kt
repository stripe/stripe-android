package com.stripe.android.paymentsheet.example.samples.ui.customersheet.playground

import com.stripe.android.customersheet.ExperimentalCustomerSheetApi
import com.stripe.android.customersheet.PaymentOptionSelection

@OptIn(ExperimentalCustomerSheetApi::class)
sealed class CustomerSheetPlaygroundViewState(
    open val isSetupIntentEnabled: Boolean = true,
    open val isGooglePayEnabled: Boolean = true,
    open val isExistingCustomer: Boolean = true,
) {
    object Loading : CustomerSheetPlaygroundViewState()
    data class FailedToLoad(val message: String) : CustomerSheetPlaygroundViewState()
    data class Data(
        override val isSetupIntentEnabled: Boolean = true,
        override val isGooglePayEnabled: Boolean = true,
        override val isExistingCustomer: Boolean = true,
        val selection: PaymentOptionSelection? = null,
        val errorMessage: String? = null,
    ) : CustomerSheetPlaygroundViewState(
        isSetupIntentEnabled = isSetupIntentEnabled,
        isGooglePayEnabled = isGooglePayEnabled,
        isExistingCustomer = isExistingCustomer,
    )
}
