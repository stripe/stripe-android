package com.stripe.android.checkout

import com.stripe.android.paymentelement.CheckoutSessionPreview
import com.stripe.android.paymentsheet.PaymentSheet

@OptIn(CheckoutSessionPreview::class)
internal fun PaymentElement.Configuration.BillingDetailsCollectionConfiguration.asPaymentSheet():
    PaymentSheet.BillingDetailsCollectionConfiguration =
    PaymentSheet.BillingDetailsCollectionConfiguration(
        name = name.asPaymentSheet(),
        phone = phone.asPaymentSheet(),
        email = email.asPaymentSheet(),
        address = address.asPaymentSheet(),
        attachDefaultsToPaymentMethod = attachDefaultsToPaymentMethod,
    )

@OptIn(CheckoutSessionPreview::class)
private fun PaymentElement.Configuration.BillingDetailsCollectionConfiguration.CollectionMode.asPaymentSheet():
    PaymentSheet.BillingDetailsCollectionConfiguration.CollectionMode = when (this) {
    PaymentElement.Configuration.BillingDetailsCollectionConfiguration.CollectionMode.Automatic ->
        PaymentSheet.BillingDetailsCollectionConfiguration.CollectionMode.Automatic
    PaymentElement.Configuration.BillingDetailsCollectionConfiguration.CollectionMode.Never ->
        PaymentSheet.BillingDetailsCollectionConfiguration.CollectionMode.Never
    PaymentElement.Configuration.BillingDetailsCollectionConfiguration.CollectionMode.Always ->
        PaymentSheet.BillingDetailsCollectionConfiguration.CollectionMode.Always
}

@OptIn(CheckoutSessionPreview::class)
private fun PaymentElement.Configuration.BillingDetailsCollectionConfiguration.AddressCollectionMode.asPaymentSheet():
    PaymentSheet.BillingDetailsCollectionConfiguration.AddressCollectionMode = when (this) {
    PaymentElement.Configuration.BillingDetailsCollectionConfiguration.AddressCollectionMode.Automatic ->
        PaymentSheet.BillingDetailsCollectionConfiguration.AddressCollectionMode.Automatic
    PaymentElement.Configuration.BillingDetailsCollectionConfiguration.AddressCollectionMode.Never ->
        PaymentSheet.BillingDetailsCollectionConfiguration.AddressCollectionMode.Never
    PaymentElement.Configuration.BillingDetailsCollectionConfiguration.AddressCollectionMode.Full ->
        PaymentSheet.BillingDetailsCollectionConfiguration.AddressCollectionMode.Full
}
