package com.stripe.android.checkout

import com.stripe.android.checkout.injection.AppName
import com.stripe.android.elements.PaymentElement
import com.stripe.android.paymentelement.CheckoutSessionPreview
import com.stripe.android.paymentelement.EmbeddedPaymentElement
import com.stripe.android.paymentsheet.repositories.CheckoutSessionResponse
import javax.inject.Inject

@OptIn(CheckoutSessionPreview::class)
internal class CheckoutEmbeddedConfigurationFactory @Inject constructor(
    @AppName private val appName: String,
) {
    fun create(
        paymentElementConfiguration: PaymentElement.Configuration.State,
        merchantDisplayName: String?,
        defaults: CheckoutController.Configuration.Defaults.State,
        checkoutSessionResponse: CheckoutSessionResponse,
        collectedDetails: CheckoutCollectedDetails,
    ): EmbeddedPaymentElement.Configuration {
        val resolvedMerchantDisplayName = resolveMerchantDisplayName(
            merchantDisplayName = merchantDisplayName,
            checkoutSessionResponse = checkoutSessionResponse,
            appName = appName,
        )
        return EmbeddedPaymentElement.Configuration.Builder(resolvedMerchantDisplayName)
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
            .googlePay(paymentElementConfiguration.toPaymentElementGooglePayConfiguration(checkoutSessionResponse))
            .link(paymentElementConfiguration.linkConfiguration.asPaymentSheet())
            .defaultBillingDetails(
                defaults.toBillingDetails(
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
