package com.stripe.android.paymentsheet.example.samples.ui.customersheet.playground

import com.stripe.android.paymentsheet.PaymentSheet

data class CustomerSheetPlaygroundConfigurationState(
    val isSetupIntentEnabled: Boolean = true,
    val isGooglePayEnabled: Boolean = true,
    val isExistingCustomer: Boolean = true,
    val useDefaultBillingAddress: Boolean = true,
    val attachDefaultBillingAddress: Boolean = true,
    val achEnabled: Boolean = true,
    val billingCollectionConfiguration: PaymentSheet.BillingDetailsCollectionConfiguration =
        PaymentSheet.BillingDetailsCollectionConfiguration()
)
