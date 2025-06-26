package com.stripe.android.paymentsheet.example.playground.settings

import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.PaymentSheet.BillingDetailsCollectionConfiguration.AddressCollectionMode
import com.stripe.android.paymentsheet.PaymentSheet.BillingDetailsCollectionConfiguration.CollectionMode

internal class BillingDetailsCollectionConfigurationBuilder(
    var name: CollectionMode = CollectionMode.Automatic,
    var phone: CollectionMode = CollectionMode.Automatic,
    var email: CollectionMode = CollectionMode.Automatic,
    var address: AddressCollectionMode = AddressCollectionMode.Automatic,
    var attachDefaultsToPaymentMethod: Boolean = false,
) {
    fun build(): PaymentSheet.BillingDetailsCollectionConfiguration {
        return PaymentSheet.BillingDetailsCollectionConfiguration(
            name = name,
            phone = phone,
            email = email,
            address = address,
            attachDefaultsToPaymentMethod = attachDefaultsToPaymentMethod,
        )
    }
}
