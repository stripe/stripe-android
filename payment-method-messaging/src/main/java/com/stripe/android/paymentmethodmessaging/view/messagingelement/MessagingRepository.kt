package com.stripe.android.paymentmethodmessaging.view.messagingelement

import com.stripe.android.PaymentConfiguration
import com.stripe.android.core.networking.ApiRequest
import com.stripe.android.model.PaymentMethodMessage
import com.stripe.android.networking.StripeRepository
import javax.inject.Inject

internal interface MessagingRepository {
   suspend fun configure(configuration: PaymentMethodMessagingElement.Configuration.State): Result<PaymentMethodMessage>
}

// Not sure if we actually need this
internal class DefaultMessagingRepository @Inject constructor(
    private val stripeRepository: StripeRepository,
    private val paymentConfiguration: PaymentConfiguration
) : MessagingRepository {

    override suspend fun configure(
        configuration: PaymentMethodMessagingElement.Configuration.State
    ): Result<PaymentMethodMessage> {
        return stripeRepository.retrievePaymentMethodMessage(
            paymentMethods = configuration.paymentMethodTypes?.map { it.code } ?: listOf(),
            amount = configuration.amount,
            currency = configuration.currency,
            locale = configuration.locale,
            country = configuration.countryCode,
            requestOptions = ApiRequest.Options(paymentConfiguration.publishableKey)
        )
    }
}