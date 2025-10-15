package com.stripe.android.paymentmethodmessaging.view.messagingelement

import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import com.stripe.android.PaymentConfiguration
import com.stripe.android.core.networking.ApiRequest
import com.stripe.android.networking.StripeRepository
import com.stripe.android.uicore.utils.collectAsState
import kotlinx.coroutines.flow.MutableStateFlow
import javax.inject.Inject

internal class MessagingContentHelper @Inject constructor(
    private val stripeRepository: StripeRepository,
    private val paymentConfiguration: PaymentConfiguration
){
    private val _state: MutableStateFlow<State?> = MutableStateFlow(null)
    suspend fun configure(
        configuration: PaymentMethodMessagingElement.Configuration
    ): PaymentMethodMessagingElement.Result {
        val state = configuration.build()
        val result = stripeRepository.retrievePaymentMethodMessage(
            paymentMethods = null,
            amount = state.amount,
            currency = state.currency,
            locale = state.locale,
            country = state.countryCode,
            requestOptions = ApiRequest.Options("pk_test_51RlchZG0ltIrPQjG7ZWZUY16t6oRDSah9oy6Hhclt3h2c3JgDEd7SljmQAgGr74dh3ECDZkDJdLhiQCDK6FDarWI00nK1B9C3c")
        )

        result.getOrNull()?.let {
            _state.value = State(
                message = it.paymentMethods
            )
        } ?: {
            _state.value = null
        }

        return PaymentMethodMessagingElement.Result.Succeeded()
    }

    @Composable
    fun Content(appearance: PaymentMethodMessagingElement.Appearance) {
        val state = _state.collectAsState().value

        if (state != null) {
            Text(state.message)
        }
    }

    private data class State(
        val message: String
    )
}