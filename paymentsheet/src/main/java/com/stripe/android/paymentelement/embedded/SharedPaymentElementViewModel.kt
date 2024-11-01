package com.stripe.android.paymentelement.embedded

import androidx.lifecycle.ViewModel
import com.stripe.android.paymentelement.EmbeddedPaymentElement.ConfigureResult
import com.stripe.android.paymentelement.EmbeddedPaymentElement.PaymentOptionDisplayData
import com.stripe.android.paymentelement.ExperimentalEmbeddedPaymentElementApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

@OptIn(ExperimentalEmbeddedPaymentElementApi::class)
internal class SharedPaymentElementViewModel : ViewModel() {
    private val _paymentOption: MutableStateFlow<PaymentOptionDisplayData?> = MutableStateFlow(null)
    val paymentOption: StateFlow<PaymentOptionDisplayData?> = _paymentOption.asStateFlow()

    suspend fun configure(): ConfigureResult {
        return ConfigureResult.Failed(IllegalStateException("Not implemented."))
    }
}
