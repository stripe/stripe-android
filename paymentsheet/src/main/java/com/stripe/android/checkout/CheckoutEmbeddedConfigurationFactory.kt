package com.stripe.android.checkout

import com.stripe.android.checkout.injection.MerchantDisplayName
import com.stripe.android.paymentelement.CheckoutSessionPreview
import com.stripe.android.paymentelement.EmbeddedPaymentElement
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.addresselement.AddressDetails
import com.stripe.android.paymentsheet.repositories.CheckoutSessionResponse
import javax.inject.Inject

@OptIn(CheckoutSessionPreview::class)
internal class CheckoutEmbeddedConfigurationFactory @Inject constructor(
    @MerchantDisplayName private val merchantDisplayName: String,
) {
    fun create(
        configuration: CheckoutController.Configuration.State,
        checkoutSessionResponse: CheckoutSessionResponse,
        collectedDetails: CheckoutCollectedDetails,
    ): EmbeddedPaymentElement.Configuration {
        val billingDetailsCollectionConfiguration = configuration.paymentElementConfiguration
            .billingDetailsCollectionConfiguration
            .reconcile(checkoutSessionResponse.requiresBillingAddress)
            .asPaymentSheet()
            .copy(attachDefaultsToPaymentMethod = true)

        return EmbeddedPaymentElement.Configuration.Builder(merchantDisplayName)
            .embeddedViewDisplaysMandateText(
                configuration.paymentElementConfiguration.embeddedViewDisplaysMandateText
            )
            .billingDetailsCollectionConfiguration(billingDetailsCollectionConfiguration)
            .googlePay(
                googlePay = checkoutSessionResponse.merchantCountry?.let { merchantCountry ->
                    configuration.googlePayConfiguration?.asPaymentSheet(merchantCountry)
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
}

@OptIn(CheckoutSessionPreview::class)
private fun Address.State.asPaymentSheetAddress(): PaymentSheet.Address {
    return PaymentSheet.Address(
        city = city,
        country = country,
        line1 = line1,
        line2 = line2,
        postalCode = postalCode,
        state = state,
    )
}
