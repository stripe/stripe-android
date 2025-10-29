@file:OptIn(PaymentMethodMessagingElementPreview::class)

package com.stripe.android.paymentmethodmessaging.element

import com.stripe.android.PaymentConfiguration
import com.stripe.android.core.networking.ApiRequest
import com.stripe.android.networking.StripeRepository
import javax.inject.Inject

internal interface PaymentMethodMessagingCoordinator {

    suspend fun configure(
        configuration: PaymentMethodMessagingElement.Configuration.State
    ): PaymentMethodMessagingElement.ConfigureResult
}

internal class DefaultPaymentMethodMessagingCoordinator @Inject constructor(
    private val stripeRepository: StripeRepository,
    private val paymentConfiguration: PaymentConfiguration
) : PaymentMethodMessagingCoordinator {

    override suspend fun configure(
        configuration: PaymentMethodMessagingElement.Configuration.State
    ): PaymentMethodMessagingElement.ConfigureResult {
        val result = stripeRepository.retrievePaymentMethodMessage(
            paymentMethods = configuration.paymentMethodTypes?.map { it.code } ?: listOf(),
            amount = configuration.amount.toInt(),
            currency = configuration.currency,
            locale = configuration.locale,
            country = configuration.countryCode,
            requestOptions = ApiRequest.Options(
                apiKey = paymentConfiguration.publishableKey,
                stripeAccount = paymentConfiguration.stripeAccountId
            )
        )

        val paymentMethodMessage = result.getOrElse {
            return PaymentMethodMessagingElement.ConfigureResult.Failed(it)
        }

        return if (paymentMethodMessage.singlePartner == null && paymentMethodMessage.multiPartner == null) {
            PaymentMethodMessagingElement.ConfigureResult.NoContent()
        } else {
            PaymentMethodMessagingElement.ConfigureResult.Succeeded()
        }
    }
}
