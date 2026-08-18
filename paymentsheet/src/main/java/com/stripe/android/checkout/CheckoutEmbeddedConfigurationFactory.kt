package com.stripe.android.checkout

import com.stripe.android.checkout.injection.AppName
import com.stripe.android.paymentelement.CheckoutSessionPreview
import com.stripe.android.paymentelement.EmbeddedPaymentElement
import com.stripe.android.paymentsheet.repositories.CheckoutSessionResponse
import javax.inject.Inject

@OptIn(CheckoutSessionPreview::class)
internal class CheckoutEmbeddedConfigurationFactory @Inject constructor(
    @AppName private val appName: String,
) {
    fun create(
        configuration: CheckoutController.Configuration.State,
        checkoutSessionResponse: CheckoutSessionResponse,
        collectedDetails: CheckoutCollectedDetails,
    ): EmbeddedPaymentElement.Configuration {
        val merchantDisplayName = configuration.resolveMerchantDisplayName(checkoutSessionResponse, appName)
        return EmbeddedPaymentElement.Configuration.Builder(merchantDisplayName)
            .embeddedViewDisplaysMandateText(
                configuration.paymentElementConfiguration.embeddedViewDisplaysMandateText
            )
            .billingDetailsCollectionConfiguration(
                configuration.toBillingDetailsCollectionConfiguration(checkoutSessionResponse)
            )
            .paymentMethodOrder(configuration.paymentElementConfiguration.paymentMethodOrder)
            .termsDisplay(configuration.paymentElementConfiguration.termsDisplay.asPaymentSheet())
            .appearance(configuration.paymentElementConfiguration.appearance.asPaymentSheet())
            .googlePay(configuration.toGooglePayConfiguration(checkoutSessionResponse))
            .defaultBillingDetails(collectedDetails.toBillingDetails(checkoutSessionResponse))
            .shippingDetails(collectedDetails.toShippingDetails())
            .build()
    }
}
