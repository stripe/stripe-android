package com.stripe.android.checkout

import com.stripe.android.paymentelement.CheckoutSessionPreview
import com.stripe.android.paymentelement.EmbeddedPaymentElement
import com.stripe.android.paymentsheet.repositories.CheckoutSessionResponse
import javax.inject.Inject

@OptIn(CheckoutSessionPreview::class)
internal class CheckoutEmbeddedConfigurationFactory @Inject constructor() {
    fun create(
        configuration: CheckoutController.Configuration.State,
        checkoutSessionResponse: CheckoutSessionResponse,
        collectedDetails: CheckoutCollectedDetails,
    ): EmbeddedPaymentElement.Configuration {
        val merchantDisplayName = configuration.resolveMerchantDisplayName(checkoutSessionResponse)
        return EmbeddedPaymentElement.Configuration.Builder(merchantDisplayName)
            .embeddedViewDisplaysMandateText(
                configuration.paymentElementConfiguration.embeddedViewDisplaysMandateText
            )
            .billingDetailsCollectionConfiguration(
                configuration.toBillingDetailsCollectionConfiguration(checkoutSessionResponse)
            )
            .googlePay(configuration.toGooglePayConfiguration(checkoutSessionResponse))
            .defaultBillingDetails(collectedDetails.toBillingDetails(checkoutSessionResponse))
            .shippingDetails(collectedDetails.toShippingDetails())
            .build()
    }
}
