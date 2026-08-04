package com.stripe.android.checkout

import com.stripe.android.checkout.CheckoutController.Address
import com.stripe.android.paymentelement.CheckoutSessionPreview
import com.stripe.android.paymentelement.EmbeddedPaymentElement
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.addresselement.AddressDetails
import com.stripe.android.paymentsheet.repositories.CheckoutSessionResponse

@OptIn(CheckoutSessionPreview::class)
internal fun Address.State.asPaymentSheetAddress(): PaymentSheet.Address {
    return PaymentSheet.Address(
        city = city,
        country = country,
        line1 = line1,
        line2 = line2,
        postalCode = postalCode,
        state = state,
    )
}

@OptIn(CheckoutSessionPreview::class)
internal fun CheckoutController.Configuration.State.asEmbeddedPaymentElementConfiguration(
    merchantDisplayName: String,
    checkoutSessionResponse: CheckoutSessionResponse,
    collectedDetails: CheckoutCollectedDetails,
): EmbeddedPaymentElement.Configuration {
    val billingDetailsCollectionConfiguration = paymentElementConfiguration
        .billingDetailsCollectionConfiguration
        .reconcile(checkoutSessionResponse.requiresBillingAddress)
        .asPaymentSheet()

    return EmbeddedPaymentElement.Configuration.Builder(merchantDisplayName)
        .embeddedViewDisplaysMandateText(paymentElementConfiguration.embeddedViewDisplaysMandateText)
        .billingDetailsCollectionConfiguration(billingDetailsCollectionConfiguration)
        .googlePay(
            googlePay = checkoutSessionResponse.merchantCountry?.let { merchantCountry ->
                googlePayConfiguration?.asPaymentSheet(merchantCountry)
            }
        )
        .defaultBillingDetails(
            PaymentSheet.BillingDetails(
                address = collectedDetails.billingAddress?.asPaymentSheetAddress(),
                email = checkoutSessionResponse.customerEmail,
                name = collectedDetails.billingName,
                phone = collectedDetails.billingPhoneNumber,
            )
        )
        .shippingDetails(
            AddressDetails(
                name = collectedDetails.shippingName,
                address = collectedDetails.shippingAddress?.asPaymentSheetAddress(),
                phoneNumber = collectedDetails.shippingPhoneNumber,
            )
        )
        .build()
}
