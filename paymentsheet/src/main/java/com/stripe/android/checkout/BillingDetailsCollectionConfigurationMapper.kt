package com.stripe.android.checkout

import com.stripe.android.paymentelement.CheckoutSessionPreview
import com.stripe.android.paymentsheet.PaymentSheet

@OptIn(CheckoutSessionPreview::class)
internal fun BillingDetailsCollectionConfiguration.State.asPaymentSheet():
    PaymentSheet.BillingDetailsCollectionConfiguration =
    PaymentSheet.BillingDetailsCollectionConfiguration(
        name = name.asPaymentSheet(),
        phone = phone.asPaymentSheet(),
        email = email.asPaymentSheet(),
        address = address.asPaymentSheet(),
        // A CheckoutSession always attaches its billing details to the payment method.
        attachDefaultsToPaymentMethod = true,
    )

@OptIn(CheckoutSessionPreview::class)
private fun BillingDetailsCollectionConfiguration.CollectionMode.asPaymentSheet():
    PaymentSheet.BillingDetailsCollectionConfiguration.CollectionMode = when (this) {
    BillingDetailsCollectionConfiguration.CollectionMode.Automatic ->
        PaymentSheet.BillingDetailsCollectionConfiguration.CollectionMode.Automatic
    BillingDetailsCollectionConfiguration.CollectionMode.Never ->
        PaymentSheet.BillingDetailsCollectionConfiguration.CollectionMode.Never
    BillingDetailsCollectionConfiguration.CollectionMode.Always ->
        PaymentSheet.BillingDetailsCollectionConfiguration.CollectionMode.Always
}

@OptIn(CheckoutSessionPreview::class)
private fun BillingDetailsCollectionConfiguration.AddressCollectionMode.asPaymentSheet():
    PaymentSheet.BillingDetailsCollectionConfiguration.AddressCollectionMode = when (this) {
    BillingDetailsCollectionConfiguration.AddressCollectionMode.Automatic ->
        PaymentSheet.BillingDetailsCollectionConfiguration.AddressCollectionMode.Automatic
    BillingDetailsCollectionConfiguration.AddressCollectionMode.Full ->
        PaymentSheet.BillingDetailsCollectionConfiguration.AddressCollectionMode.Full
}
