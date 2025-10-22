package com.stripe.android.paymentmethodmessaging.view.messagingelement

import com.stripe.android.PaymentConfiguration
import com.stripe.android.core.networking.ApiRequest
import com.stripe.android.networking.StripeRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

internal interface MessagingCoordinator {
    val messagingContent: StateFlow<MessagingContent?>
    suspend fun configure(
        configuration: PaymentMethodMessagingElement.Configuration
    ) : PaymentMethodMessagingElement.Result
}

internal class DefaultMessagingCoordinator @Inject constructor(
    private val stripeRepository: StripeRepository,
    private val paymentConfiguration: PaymentConfiguration
): MessagingCoordinator {

    private val _messagingContent = MutableStateFlow<MessagingContent?>(null)
    override val messagingContent: StateFlow<MessagingContent?> = _messagingContent.asStateFlow()

    override suspend fun configure(
        configuration: PaymentMethodMessagingElement.Configuration
    ): PaymentMethodMessagingElement.Result {
        val state = configuration.build()
        val result = stripeRepository.retrievePaymentMethodMessage(
            paymentMethods = state.paymentMethodTypes?.map { it.code } ?: listOf(),
            amount = state.amount,
            currency = state.currency,
            locale = state.locale,
            country = state.countryCode,
            requestOptions = ApiRequest.Options(
                apiKey = paymentConfiguration.publishableKey,
                stripeAccount = paymentConfiguration.stripeAccountId
            )
        )

        val paymentMethodMessage = result.getOrElse {
            _messagingContent.value = null
            return PaymentMethodMessagingElement.Result.Failed(it)
        }

        val message = MessageTransformer.transformPaymentMethodMessage(paymentMethodMessage)
        _messagingContent.value = MessagingContent(message)

        return if (message is Message.Empty) {
            PaymentMethodMessagingElement.Result.NoContent
        } else {
            PaymentMethodMessagingElement.Result.Succeeded
        }
    }
}