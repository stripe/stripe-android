package com.stripe.android.paymentmethodmessaging.view

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.CreationExtras
import com.stripe.android.core.exception.StripeException
import com.stripe.android.core.networking.ApiRequest
import com.stripe.android.model.PaymentMethodMessage
import com.stripe.android.networking.StripeApiRepository
import com.stripe.android.paymentmethodmessaging.view.injection.DaggerPaymentMethodMessagingComponent
import com.stripe.android.utils.requireApplication
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

private typealias Mapper = (CoroutineScope, PaymentMethodMessage) -> Deferred<PaymentMethodMessagingData>

internal class PaymentMethodMessagingViewModel @Inject constructor(
    private val isSystemDarkTheme: Boolean,
    private val config: PaymentMethodMessagingView.Configuration,
    private val stripeApiRepository: StripeApiRepository,
    private val mapper: @JvmSuppressWildcards Mapper
) : ViewModel() {

    private val _messageFlow = MutableStateFlow<Result<PaymentMethodMessagingData>?>(null)
    val messageFlow: StateFlow<Result<PaymentMethodMessagingData>?> = _messageFlow

    fun loadMessage() {
        viewModelScope.launch {
            _messageFlow.update {
                try {
                    val message = stripeApiRepository.retrievePaymentMethodMessage(
                        paymentMethods = config.paymentMethods.map { it.value },
                        amount = config.amount,
                        currency = config.currency,
                        country = config.countryCode,
                        locale = config.locale.toLanguageTag(),
                        logoColor = config.imageColor?.value ?: if (isSystemDarkTheme) {
                            PaymentMethodMessagingView.Configuration.ImageColor.Light.value
                        } else {
                            PaymentMethodMessagingView.Configuration.ImageColor.Dark.value
                        },
                        requestOptions = ApiRequest.Options(config.publishableKey),
                    )
                    if (
                        message == null ||
                        message.displayHtml.isBlank() ||
                        message.learnMoreUrl.isBlank()
                    ) {
                        Result.failure(Exception("Could not retrieve message"))
                    } else {
                        Result.success(mapper(this, message).await())
                    }
                } catch (e: StripeException) {
                    Result.failure(e)
                }
            }
        }
    }

    internal class Factory(
        private val configuration: PaymentMethodMessagingView.Configuration,
        private val isSystemDarkTheme: Boolean,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T {
            val application = extras.requireApplication()

            return DaggerPaymentMethodMessagingComponent.builder()
                .application(application)
                .configuration(configuration)
                .isSystemDarkTheme(isSystemDarkTheme)
                .build()
                .viewModel as T
        }
    }
}
