package com.stripe.android.ui.core.elements.messaging

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.CreationExtras
import com.stripe.android.core.exception.StripeException
import com.stripe.android.core.networking.ApiRequest
import com.stripe.android.model.PaymentMethodMessage
import com.stripe.android.networking.StripeApiRepository
import com.stripe.android.ui.core.elements.messaging.injection.DaggerPaymentMethodMessageComponent
import com.stripe.android.ui.core.isSystemDarkTheme
import com.stripe.android.utils.requireApplication
import javax.inject.Inject

internal class PaymentMethodMessageViewModel @Inject constructor(
    private val application: Application,
    private val configuration: PaymentMethodMessageView.Configuration,
    private val stripeApiRepository: StripeApiRepository
) : ViewModel() {
    suspend fun loadMessage(): Result<PaymentMethodMessage> {
        return try {
            val message = stripeApiRepository.retrievePaymentMethodMessage(
                paymentMethods = configuration.paymentMethods.map { it.value },
                amount = configuration.amount,
                currency = configuration.currency,
                country = configuration.countryCode,
                locale = configuration.locale.toLanguageTag(),
                logoColor = configuration.imageColor?.value ?: if (application.isSystemDarkTheme()) {
                    PaymentMethodMessageView.Configuration.ImageColor.Light.value
                } else {
                    PaymentMethodMessageView.Configuration.ImageColor.Dark.value
                },
                requestOptions = ApiRequest.Options(configuration.publishableKey),
            )
            if (
                message == null ||
                message.displayHtml.isBlank() ||
                message.learnMoreUrl.isBlank()
            ) {
                Result.failure(Exception("Could not retrieve message"))
            } else {
                Result.success(message)
            }
        } catch (e: StripeException) {
            Result.failure(e)
        }
    }

    internal class Factory(
        private val configurationProvider: () -> PaymentMethodMessageView.Configuration
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T {
            val application = extras.requireApplication()
            return DaggerPaymentMethodMessageComponent.builder()
                .application(application)
                .configuration(configurationProvider())
                .build()
                .viewModel as T
        }
    }
}
