package com.stripe.android.paymentmethodmessaging.view.messagingelement

import com.stripe.android.PaymentConfiguration
import com.stripe.android.core.networking.ApiRequest
import com.stripe.android.networking.StripeRepository
import javax.inject.Inject

internal interface MessagingRepository {
   suspend fun configure(configuration: PaymentMethodMessagingElement.Configuration.State)
}

internal class DefaultMessagingRepository @Inject constructor(
    private val stripeRepository: StripeRepository,
    private val paymentConfiguration: PaymentConfiguration
) : MessagingRepository {

    override suspend fun configure(configuration: PaymentMethodMessagingElement.Configuration.State) {
        val result = stripeRepository.retrievePaymentMethodMessage(
            paymentMethods = configuration.paymentMethodTypes?.map { it.code } ?: listOf(),
            amount = configuration.amount,
            currency = configuration.currency,
            locale = configuration.locale,
            country = configuration.countryCode,
            requestOptions = ApiRequest.Options(paymentConfiguration.publishableKey)
        )

        // process and return correct type
    }
}