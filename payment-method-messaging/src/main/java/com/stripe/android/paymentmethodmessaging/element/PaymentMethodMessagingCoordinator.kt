@file:OptIn(PaymentMethodMessagingElementPreview::class)

package com.stripe.android.paymentmethodmessaging.element

import com.stripe.android.PaymentConfiguration
import com.stripe.android.core.networking.ApiRequest
import com.stripe.android.networking.StripeRepository
import com.stripe.android.paymentmethodmessaging.element.analytics.PaymentMethodMessagingEventReporter
import com.stripe.android.paymentmethodmessaging.element.analytics.paymentMethods
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject
import javax.inject.Provider

internal interface PaymentMethodMessagingCoordinator {
    val messagingContent: StateFlow<PaymentMethodMessagingContent?>
    suspend fun configure(
        configuration: PaymentMethodMessagingElement.Configuration.State
    ): PaymentMethodMessagingElement.ConfigureResult
}

internal class DefaultPaymentMethodMessagingCoordinator @Inject constructor(
    private val stripeRepository: StripeRepository,
    private val paymentConfiguration: Provider<PaymentConfiguration>,
    private val eventReporter: PaymentMethodMessagingEventReporter
) : PaymentMethodMessagingCoordinator {

    private val _messagingContent = MutableStateFlow<PaymentMethodMessagingContent?>(null)
    override val messagingContent: StateFlow<PaymentMethodMessagingContent?> = _messagingContent

    override suspend fun configure(
        configuration: PaymentMethodMessagingElement.Configuration.State
    ): PaymentMethodMessagingElement.ConfigureResult {
        eventReporter.onLoadStarted(configuration)
        val result = stripeRepository.retrievePaymentMethodMessage(
            paymentMethods = configuration.paymentMethodTypes?.map { it.code } ?: listOf(),
            amount = configuration.amount.toInt(),
            currency = configuration.currency,
            locale = configuration.locale,
            country = configuration.countryCode,
            requestOptions = ApiRequest.Options(
                apiKey = paymentConfiguration.get().publishableKey,
                stripeAccount = paymentConfiguration.get().stripeAccountId
            )
        )

        val paymentMethodMessage = result.getOrElse {
            _messagingContent.value = null
            eventReporter.onLoadFailed(it)
            return PaymentMethodMessagingElement.ConfigureResult.Failed(it)
        }

        val content = PaymentMethodMessagingContent.get(paymentMethodMessage, eventReporter::onElementTapped)
        _messagingContent.value = content
        eventReporter.onLoadSucceeded(paymentMethodMessage.paymentMethods(), content)

        return when (content) {
            is PaymentMethodMessagingContent.SinglePartner,
            is PaymentMethodMessagingContent.MultiPartner -> PaymentMethodMessagingElement.ConfigureResult.Succeeded()
            is PaymentMethodMessagingContent.NoContent -> PaymentMethodMessagingElement.ConfigureResult.NoContent()
        }
    }
}
