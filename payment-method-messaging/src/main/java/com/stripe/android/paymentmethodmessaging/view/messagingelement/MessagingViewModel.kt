package com.stripe.android.paymentmethodmessaging.view.messagingelement

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewmodel.CreationExtras
import com.stripe.android.PaymentConfiguration
import com.stripe.android.core.networking.ApiRequest
import com.stripe.android.core.utils.requireApplication
import com.stripe.android.networking.StripeRepository
import com.stripe.android.paymentmethodmessaging.view.injection.DaggerPaymentMethodMessagingViewModelComponent
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

internal class MessagingViewModel @Inject constructor(
    private val stripeRepository: StripeRepository,
    private val paymentConfiguration: PaymentConfiguration
) : ViewModel() {
    private val _state: MutableStateFlow<State?> = MutableStateFlow(null)
    val state: StateFlow<State?> = _state
    suspend fun configure(
        configuration: PaymentMethodMessagingElement.Configuration.State
    ): PaymentMethodMessagingElement.Result {
        val result = stripeRepository.retrievePaymentMethodMessage(
            paymentMethods = null,//state.paymentMethodTypes?.map { it.code },
            amount = configuration.amount,
            currency = configuration.currency,
            locale = configuration.locale,
            country = configuration.countryCode,
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

    class Factory : ViewModelProvider.Factory {

        override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T {
            val component = DaggerPaymentMethodMessagingViewModelComponent.factory()
                .build(
                    savedStateHandle = extras.createSavedStateHandle(),
                    application = extras.requireApplication()
                )
            @Suppress("UNCHECKED_CAST")
            return component.viewModel as T
        }
    }

    internal data class State(
        val message: String
    )
}