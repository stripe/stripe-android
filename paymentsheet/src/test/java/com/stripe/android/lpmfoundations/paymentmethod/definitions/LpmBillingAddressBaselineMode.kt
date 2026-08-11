package com.stripe.android.lpmfoundations.paymentmethod.definitions

import com.stripe.android.paymentsheet.PaymentSheet

internal enum class LpmBillingAddressBaselineMode {
    Never,
    AutomaticWithoutTax,
    Full,
    ;

    fun billingDetailsCollectionConfiguration(): PaymentSheet.BillingDetailsCollectionConfiguration {
        return when (this) {
            Never -> PaymentSheet.BillingDetailsCollectionConfiguration(
                name = PaymentSheet.BillingDetailsCollectionConfiguration.CollectionMode.Never,
                phone = PaymentSheet.BillingDetailsCollectionConfiguration.CollectionMode.Never,
                email = PaymentSheet.BillingDetailsCollectionConfiguration.CollectionMode.Never,
                address = PaymentSheet.BillingDetailsCollectionConfiguration.AddressCollectionMode.Never,
            )
            AutomaticWithoutTax -> PaymentSheet.BillingDetailsCollectionConfiguration()
            Full -> PaymentSheet.BillingDetailsCollectionConfiguration(
                address = PaymentSheet.BillingDetailsCollectionConfiguration.AddressCollectionMode.Full,
            )
        }
    }
}
