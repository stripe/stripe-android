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
    ): EmbeddedPaymentElement.Configuration? {
        val paymentElementConfiguration = configuration.paymentElementConfiguration ?: return null
        val merchantDisplayName = configuration.resolveMerchantDisplayName(checkoutSessionResponse, appName)
        return EmbeddedPaymentElement.Configuration.Builder(merchantDisplayName)
            .embeddedViewDisplaysMandateText(
                paymentElementConfiguration.embeddedViewDisplaysMandateText
            )
            .billingDetailsCollectionConfiguration(
                checkoutSessionResponse.toBillingDetailsCollectionConfiguration()
            )
            .preferredNetworks(paymentElementConfiguration.preferredNetworks)
            .paymentMethodOrder(paymentElementConfiguration.paymentMethodOrder)
            .cardBrandAcceptance(paymentElementConfiguration.cardBrandAcceptance.asPaymentSheet())
            .opensCardScannerAutomatically(
                paymentElementConfiguration.opensCardScannerAutomatically
            )
            .termsDisplay(paymentElementConfiguration.termsDisplay.asPaymentSheet())
            .appearance(paymentElementConfiguration.appearance.asPaymentSheet())
            .googlePay(configuration.toPaymentElementGooglePayConfiguration(checkoutSessionResponse))
            .link(paymentElementConfiguration.linkConfiguration.asPaymentSheet())
            .defaultBillingDetails(
                configuration.toBillingDetails(
                    checkoutSessionResponse = checkoutSessionResponse,
                    collectedEmail = collectedDetails.email,
                ),
            )
            .shippingDetails(collectedDetails.toShippingDetails())
            .allowsDelayedPaymentMethods(true)
            .allowsPaymentMethodsRequiringShippingAddress(true)
            .build()
    }
}
