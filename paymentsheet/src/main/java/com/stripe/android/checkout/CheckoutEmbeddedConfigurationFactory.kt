package com.stripe.android.checkout

import com.stripe.android.checkout.injection.MerchantDisplayName
import com.stripe.android.paymentelement.CheckoutSessionPreview
import com.stripe.android.paymentelement.EmbeddedPaymentElement
import javax.inject.Inject

@OptIn(CheckoutSessionPreview::class)
internal class CheckoutEmbeddedConfigurationFactory @Inject constructor(
    @MerchantDisplayName private val merchantDisplayName: String,
) {
    fun create(
        configuration: CheckoutController.Configuration.State,
        sessionData: CheckoutSessionData,
    ): EmbeddedPaymentElement.Configuration {
        val response = sessionData.checkoutSessionResponse

        val baseConfig = EmbeddedPaymentElement.Configuration.Builder(merchantDisplayName)
            .embeddedViewDisplaysMandateText(
                configuration.paymentElementConfiguration.embeddedViewDisplaysMandateText
            )
            .billingDetailsCollectionConfiguration(
                configuration.paymentElementConfiguration.billingDetailsCollectionConfiguration
                    .reconcile(response.requiresBillingAddress)
                    .asPaymentSheet()
            )
            .googlePay(
                googlePay = response.merchantCountry?.let { merchantCountry ->
                    configuration.googlePayConfiguration?.asPaymentSheet(merchantCountry)
                }
            )
            .build()

        return CheckoutConfigurationMerger.EmbeddedConfiguration(baseConfig)
            .forCheckoutSession(sessionData)
    }
}
