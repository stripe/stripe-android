package com.stripe.android.paymentmethodmessaging.view

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.CreationExtras
import com.stripe.android.core.exception.APIException
import com.stripe.android.core.networking.ApiRequest
import com.stripe.android.model.PaymentMethodMessage
import com.stripe.android.networking.StripeRepository
import com.stripe.android.paymentmethodmessaging.view.injection.DaggerPaymentMethodMessagingComponent
import com.stripe.android.utils.requireApplication
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

private typealias Mapper = (CoroutineScope, PaymentMethodMessage) ->
Deferred<PaymentMethodMessagingData>

internal class PaymentMethodMessagingViewModel @Inject constructor(
    private val isSystemDarkThemeProvider: () -> Boolean,
    private val config: PaymentMethodMessagingView.Configuration,
    private val stripeRepository: StripeRepository,
    private val mapper: @JvmSuppressWildcards Mapper
) : ViewModel() {

    private val _messageFlow = MutableStateFlow<Result<PaymentMethodMessagingData>?>(null)
    val messageFlow: StateFlow<Result<PaymentMethodMessagingData>?> = _messageFlow

    fun loadMessage() {
        viewModelScope.launch {
            _messageFlow.update {
                retrievePaymentMethodMessage().mapCatching { message ->
                    if (message.displayHtml.isBlank() || message.learnMoreUrl.isBlank()) {
                        throw APIException(message = "Could not retrieve message")
                    } else {
                        mapper(this, message).await()
                    }
                }
            }
        }
    }

    private suspend fun retrievePaymentMethodMessage(): Result<PaymentMethodMessage> {
        return stripeRepository.retrievePaymentMethodMessage(
            paymentMethods = config.paymentMethods.map { it.value },
            amount = config.amount,
            currency = config.currency,
            country = config.countryCode,
            locale = config.locale.toLanguageTag(),
            logoColor = config.imageColor?.value ?: if (isSystemDarkThemeProvider()) {
                PaymentMethodMessagingView.Configuration.ImageColor.Light.value
            } else {
                PaymentMethodMessagingView.Configuration.ImageColor.Dark.value
            },
            requestOptions = ApiRequest.Options(config.publishableKey),
        )
    }

    internal class Factory(
        private val configuration: PaymentMethodMessagingView.Configuration
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T {
            val application = extras.requireApplication()

            return DaggerPaymentMethodMessagingComponent.builder()
                .application(application)
                .configuration(configuration)
                .build()
                .viewModel as T
        }
    }
}
