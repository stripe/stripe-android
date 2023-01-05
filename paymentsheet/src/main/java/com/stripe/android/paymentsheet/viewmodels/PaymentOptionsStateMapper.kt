package com.stripe.android.paymentsheet.viewmodels

import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.asFlow
import androidx.lifecycle.asLiveData
import androidx.lifecycle.distinctUntilChanged
import com.stripe.android.model.PaymentMethod
import com.stripe.android.paymentsheet.PaymentOptionsState
import com.stripe.android.paymentsheet.PaymentOptionsStateFactory
import com.stripe.android.paymentsheet.model.PaymentSelection
import com.stripe.android.paymentsheet.model.SavedSelection
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

internal class PaymentOptionsStateMapper(
    private val paymentMethods: StateFlow<List<PaymentMethod>>,
    private val isGooglePayReady: StateFlow<Boolean>,
    private val isLinkEnabled: StateFlow<Boolean>,
    private val initialSelection: StateFlow<SavedSelection>,
    private val currentSelection: StateFlow<PaymentSelection?>,
    private val isNotPaymentFlow: Boolean,
) {

    operator fun invoke(): Flow<PaymentOptionsState> {
        return MediatorLiveData<PaymentOptionsState>().apply {
            listOf(
                paymentMethods.asLiveData(),
                currentSelection.asLiveData(),
                initialSelection.asLiveData(),
                isGooglePayReady.asLiveData(),
                isLinkEnabled.asLiveData(),
            ).forEach { source ->
                addSource(source) {
                    value = createPaymentOptionsState()
                }
            }
        }.distinctUntilChanged().asFlow()
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
