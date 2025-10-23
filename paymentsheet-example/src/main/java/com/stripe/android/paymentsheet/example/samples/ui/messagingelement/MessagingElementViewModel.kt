package com.stripe.android.paymentsheet.example.samples.ui.messagingelement

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
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
    private val _Configure_result = MutableStateFlow<PaymentMethodMessagingElement.ConfigureResult?>(null)
    val configureResult: StateFlow<PaymentMethodMessagingElement.ConfigureResult?> = _Configure_result.asStateFlow()

    private val _config = MutableStateFlow(Config())
    val config: StateFlow<Config> = _config.asStateFlow()

    fun configurePaymentMethodMessagingElement() {
        val pmTypes = config.value.paymentMethods.mapNotNull {
            PaymentMethod.Type.fromCode(it)
        }
        viewModelScope.launch {
            _Configure_result.value = paymentMethodMessagingElement.configure(
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

    data class Config(
        val amount: Long = 0,
        val currency: String = "usd",
        val locale: String = "en",
        val countryCode: String = "US",
        val paymentMethods: List<String> = listOf("affirm", "klarna", "afterpay_clearpay")
    )
}
