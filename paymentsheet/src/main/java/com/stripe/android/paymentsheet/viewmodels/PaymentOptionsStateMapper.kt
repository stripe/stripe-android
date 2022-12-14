package com.stripe.android.paymentsheet.viewmodels

import com.stripe.android.model.PaymentMethod
import com.stripe.android.paymentsheet.PaymentOptionsState
import com.stripe.android.paymentsheet.PaymentOptionsStateFactory
import com.stripe.android.paymentsheet.model.PaymentSelection
import com.stripe.android.paymentsheet.model.SavedSelection
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged

internal class PaymentOptionsStateMapper(
    private val paymentMethods: StateFlow<List<PaymentMethod>>,
    private val isGooglePayReady: StateFlow<Boolean>,
    private val isLinkEnabled: StateFlow<Boolean>,
    private val initialSelection: StateFlow<SavedSelection>,
    private val currentSelection: StateFlow<PaymentSelection?>,
    private val isNotPaymentFlow: Boolean,
) {

    operator fun invoke(): Flow<PaymentOptionsState> {
        return combine(
            combine(
                paymentMethods,
                currentSelection,
                initialSelection,
                ::Triple
            ),
            combine(
                isGooglePayReady,
                isLinkEnabled,
                ::Pair
            )
        ) { _, _ ->
            createPaymentOptionsState()
        }.distinctUntilChanged()
    }

    @Suppress("ReturnCount")
    private fun createPaymentOptionsState(): PaymentOptionsState {
        val paymentMethods = paymentMethods.value
        val initialSelection = initialSelection.value
        val isGooglePayReady = isGooglePayReady.value
        val isLinkEnabled = isLinkEnabled.value
        val currentSelection = currentSelection.value

        return PaymentOptionsStateFactory.create(
            paymentMethods = paymentMethods,
            showGooglePay = isGooglePayReady && isNotPaymentFlow,
            showLink = isLinkEnabled && isNotPaymentFlow,
            initialSelection = initialSelection,
            currentSelection = currentSelection,
        )
    }
}