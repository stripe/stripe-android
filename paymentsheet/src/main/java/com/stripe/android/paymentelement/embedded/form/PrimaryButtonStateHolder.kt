package com.stripe.android.paymentelement.embedded.form

import com.stripe.android.core.injection.ViewModelScope
import com.stripe.android.core.strings.ResolvableString
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadata
import com.stripe.android.model.PaymentIntent
import com.stripe.android.model.StripeIntent
import com.stripe.android.paymentelement.EmbeddedPaymentElement
import com.stripe.android.paymentelement.ExperimentalEmbeddedPaymentElementApi
import com.stripe.android.paymentelement.embedded.EmbeddedSelectionHolder
import com.stripe.android.paymentsheet.model.amount
import com.stripe.android.paymentsheet.model.currency
import com.stripe.android.paymentsheet.ui.PrimaryButtonProcessingState
import com.stripe.android.paymentsheet.utils.buyButtonLabel
import com.stripe.android.ui.core.Amount
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@OptIn(ExperimentalEmbeddedPaymentElementApi::class)
internal class PrimaryButtonStateHolder @Inject constructor(
    paymentMethodMetadata: PaymentMethodMetadata,
    selectionHolder: EmbeddedSelectionHolder,
    @ViewModelScope coroutineScope: CoroutineScope,
    configuration: EmbeddedPaymentElement.Configuration,
) {
    private val _state = MutableStateFlow(
        State(
            label = primaryButtonLabel(paymentMethodMetadata.stripeIntent, configuration),
            isEnabled = false,
            processingState = PrimaryButtonProcessingState.Idle(null)
        )
    )
    val state: StateFlow<State> = _state

    init {
        coroutineScope.launch {
            selectionHolder.selection.collectLatest { selection ->
                _state.update { state ->
                    state.copy(
                        isEnabled = selection != null
                    )
                }
            }
        }
    }

    private fun primaryButtonLabel(
        stripeIntent: StripeIntent,
        configuration: EmbeddedPaymentElement.Configuration
    ): ResolvableString {
        val amount = amount(stripeIntent.amount, stripeIntent.currency)
        val label = configuration.primaryButtonLabel
        val isForPaymentIntent = stripeIntent is PaymentIntent
        return buyButtonLabel(amount, label, isForPaymentIntent)
    }

    private fun amount(amount: Long?, currency: String?): Amount? {
        return if (amount != null && currency != null) Amount(amount, currency) else null
    }

    internal data class State(
        val label: ResolvableString,
        val isEnabled: Boolean,
        val processingState: PrimaryButtonProcessingState
    )
}
