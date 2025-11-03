package com.stripe.android.paymentmethodmessaging.example

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.stripe.android.PaymentConfiguration
import com.stripe.android.model.PaymentMethod
import com.stripe.android.paymentmethodmessaging.element.PaymentMethodMessagingElement
import com.stripe.android.paymentmethodmessaging.element.PaymentMethodMessagingElementPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

@OptIn(PaymentMethodMessagingElementPreview::class)
internal class MessagingElementViewModel(
    application: Application,
) : AndroidViewModel(application) {

    val paymentMethodMessagingElement = PaymentMethodMessagingElement.create(getApplication())
    private val _configureResult = MutableStateFlow<PaymentMethodMessagingElement.ConfigureResult?>(null)
    val configureResult: StateFlow<PaymentMethodMessagingElement.ConfigureResult?> = _configureResult.asStateFlow()

    private val _config = MutableStateFlow(Config())
    val config: StateFlow<Config> = _config.asStateFlow()

    private val settings = Settings(getApplication())

    fun configurePaymentMethodMessagingElement() {
        val pmTypes = config.value.paymentMethods.mapNotNull {
            PaymentMethod.Type.fromCode(it)
        }

        initPaymentConfig()

        viewModelScope.launch {
            _configureResult.value = paymentMethodMessagingElement.configure(
                configuration = PaymentMethodMessagingElement.Configuration()
                    .amount(config.value.amount)
                    .currency(config.value.currency)
                    .locale(config.value.locale)
                    .countryCode(config.value.countryCode)
                    .paymentMethodTypes(pmTypes)
            )
        }
    }

    fun updateConfigState(config: Config) {
        _config.value = config
    }

    private fun initPaymentConfig() {
        val pk = _config.value.publishableKey.ifBlank {
            settings.publishableKey
        }
        val accountId = _config.value.stripeAccountId?.ifBlank {
            settings.stripeAccountId
        }
        PaymentConfiguration.init(getApplication(), pk, accountId)
    }

    data class Config(
        val amount: Long = 0,
        val currency: String = "usd",
        val locale: String = "en",
        val countryCode: String = "US",
        val paymentMethods: List<String> = listOf("affirm", "klarna", "afterpay_clearpay"),
        val publishableKey: String = "",
        val stripeAccountId: String? = ""
    )
}
