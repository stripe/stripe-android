package com.stripe.android.paymentsheet.example.playground.settings

import com.stripe.android.elements.BillingDetailsCollectionConfiguration
import com.stripe.android.elements.BillingDetailsCollectionConfiguration.AddressCollectionMode
import com.stripe.android.elements.BillingDetailsCollectionConfiguration.CollectionMode

internal class BillingDetailsCollectionConfigurationBuilder(
    var name: CollectionMode = CollectionMode.Automatic,
    var phone: CollectionMode = CollectionMode.Automatic,
    var email: CollectionMode = CollectionMode.Automatic,
    var address: AddressCollectionMode = AddressCollectionMode.Automatic,
    var attachDefaultsToPaymentMethod: Boolean = false,
) {
    fun build(): BillingDetailsCollectionConfiguration {
        return BillingDetailsCollectionConfiguration(
            name = name,
            phone = phone,
            email = email,
            address = address,
            attachDefaultsToPaymentMethod = attachDefaultsToPaymentMethod,
        )
    }
}
