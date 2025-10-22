package com.stripe.android.paymentsheet.example.samples.ui.messagingelement

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.github.kittinunf.fuel.Fuel
import com.github.kittinunf.fuel.core.extensions.jsonBody
import com.github.kittinunf.fuel.core.requests.suspendable
import com.github.kittinunf.result.Result
import com.stripe.android.PaymentConfiguration
import com.stripe.android.model.PaymentMethod
import com.stripe.android.paymentmethodmessaging.view.messagingelement.PaymentMethodMessagingElement
import com.stripe.android.paymentsheet.PaymentSheetResult
import com.stripe.android.paymentsheet.example.samples.model.CartState
import com.stripe.android.paymentsheet.example.samples.networking.ExampleCheckoutRequest
import com.stripe.android.paymentsheet.example.samples.networking.ExampleCheckoutResponse
import com.stripe.android.paymentsheet.example.samples.networking.awaitModel
import com.stripe.android.paymentsheet.example.samples.networking.toCheckoutRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.flow.updateAndGet
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json

internal class MessagingElementViewModel(
    application: Application,
) : AndroidViewModel(application) {

    val paymentMethodMessagingElement = PaymentMethodMessagingElement.create(getApplication())
    private val _result = MutableStateFlow<PaymentMethodMessagingElement.Result?>(null)
    val result: StateFlow<PaymentMethodMessagingElement.Result?> = _result.asStateFlow()

    fun configurePaymentMethodMessagingElement(
        amount: Long,
        currency: String,
        locale: String,
        countryCode: String,
        paymentMethods: List<String>
    ) {
        val pmTypes = paymentMethods.mapNotNull {
            PaymentMethod.Type.fromCode(it)
        }
        viewModelScope.launch {
            _result.value = paymentMethodMessagingElement.configure(
                configuration = PaymentMethodMessagingElement.Configuration()
                    .amount(amount)
                    .currency(currency)
                    .locale(locale)
                    .countryCode(countryCode)
                    .paymentMethodTypes(pmTypes)
            )
        }
    }
}
