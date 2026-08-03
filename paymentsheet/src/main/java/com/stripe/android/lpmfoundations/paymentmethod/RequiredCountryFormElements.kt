package com.stripe.android.lpmfoundations.paymentmethod

import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.ui.core.elements.BillingAddressCollectionMode
import com.stripe.android.ui.core.elements.CountrySpec
import com.stripe.android.uicore.elements.FormElement

internal fun CountrySpec.transformToFormElements(
    arguments: UiDefinitionFactory.Arguments,
    initialCountry: String?,
): List<FormElement> {
    val initialValues = arguments.initialValues.toMutableMap().apply {
        initialCountry?.let { put(apiPath, it) }
    }
    val addressCollectionMode = when (arguments.billingDetailsCollectionConfiguration.address) {
        PaymentSheet.BillingDetailsCollectionConfiguration.AddressCollectionMode.Full -> {
            BillingAddressCollectionMode.Full
        }
        PaymentSheet.BillingDetailsCollectionConfiguration.AddressCollectionMode.Automatic,
        PaymentSheet.BillingDetailsCollectionConfiguration.AddressCollectionMode.Never -> {
            BillingAddressCollectionMode.Country(emptyMap())
        }
    }

    return transform(
        initialValues = initialValues,
        shippingValues = arguments.shippingValues,
        autocompleteAddressInteractorFactory = arguments.autocompleteAddressInteractorFactory,
        addressCollectionMode = addressCollectionMode,
    )
}
