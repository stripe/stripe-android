package com.stripe.android.elements.ece

import com.stripe.android.elements.ExpressCheckoutElement.Configuration.BillingDetailsCollectionConfiguration.AddressCollectionMode
import com.stripe.android.elements.ExpressCheckoutElement.Configuration.BillingDetailsCollectionConfiguration.CollectionMode
import com.stripe.android.elements.ExpressCheckoutElement.Configuration.BillingDetailsCollectionConfiguration.State
import com.stripe.android.paymentelement.CheckoutSessionPreview
import com.stripe.android.paymentsheet.PaymentSheet.BillingDetailsCollectionConfiguration as PaymentSheetBillingDetails

@OptIn(CheckoutSessionPreview::class)
internal fun State.asPaymentSheet(
    requiresBillingAddress: Boolean,
): PaymentSheetBillingDetails =
    PaymentSheetBillingDetails(
        name = name.asPaymentSheet(),
        phone = PaymentSheetBillingDetails.CollectionMode.Automatic,
        email = email.asPaymentSheet(),
        address = address.asPaymentSheet(requiresBillingAddress),
        attachDefaultsToPaymentMethod = true,
    )

@OptIn(CheckoutSessionPreview::class)
private fun CollectionMode.asPaymentSheet():
    PaymentSheetBillingDetails.CollectionMode = when (this) {
    CollectionMode.Automatic ->
        PaymentSheetBillingDetails.CollectionMode.Automatic
    CollectionMode.Never ->
        PaymentSheetBillingDetails.CollectionMode.Never
    CollectionMode.Always ->
        PaymentSheetBillingDetails.CollectionMode.Always
}

@OptIn(CheckoutSessionPreview::class)
private fun AddressCollectionMode.asPaymentSheet(
    requiresBillingAddress: Boolean,
): PaymentSheetBillingDetails.AddressCollectionMode = when (this) {
    AddressCollectionMode.Automatic ->
        if (requiresBillingAddress) {
            PaymentSheetBillingDetails.AddressCollectionMode.Full
        } else {
            PaymentSheetBillingDetails.AddressCollectionMode.Automatic
        }
    AddressCollectionMode.Full ->
        PaymentSheetBillingDetails.AddressCollectionMode.Full
}
