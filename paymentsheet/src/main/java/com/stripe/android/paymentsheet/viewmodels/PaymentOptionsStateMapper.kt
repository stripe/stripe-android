package com.stripe.android.paymentsheet.viewmodels

import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.distinctUntilChanged
import com.stripe.android.model.PaymentMethod
import com.stripe.android.paymentsheet.PaymentOptionsState
import com.stripe.android.paymentsheet.PaymentOptionsStateFactory
import com.stripe.android.paymentsheet.model.PaymentSelection
import com.stripe.android.paymentsheet.model.SavedSelection
import com.stripe.android.paymentsheet.state.GooglePayState

internal class PaymentOptionsStateMapper(
    private val paymentMethods: LiveData<List<PaymentMethod>?>,
    private val googlePayState: LiveData<GooglePayState>,
    private val isLinkEnabled: LiveData<Boolean>,
    private val initialSelection: LiveData<SavedSelection?>,
    private val currentSelection: LiveData<PaymentSelection?>,
    private val isNotPaymentFlow: Boolean,
) {

    operator fun invoke(): LiveData<PaymentOptionsState> {
        return MediatorLiveData<PaymentOptionsState>().apply {
            listOf(
                paymentMethods,
                currentSelection,
                initialSelection,
                googlePayState,
                isLinkEnabled,
            ).forEach { source ->
                addSource(source) {
                    val newState = createPaymentOptionsState()
                    if (newState != null) {
                        value = newState
                    }
                }
            }
        }.distinctUntilChanged()
    }

    @Suppress("ReturnCount")
    private fun createPaymentOptionsState(): PaymentOptionsState? {
        val paymentMethods = paymentMethods.value ?: return null
        val initialSelection = initialSelection.value ?: return null
        val googlePayState = googlePayState.value ?: return null
        val isLinkEnabled = isLinkEnabled.value ?: return null

        val currentSelection = currentSelection.value

        return PaymentOptionsStateFactory.create(
            paymentMethods = paymentMethods,
            showGooglePay = (googlePayState is GooglePayState.Available) && isNotPaymentFlow,
            showLink = isLinkEnabled && isNotPaymentFlow,
            initialSelection = initialSelection,
            currentSelection = currentSelection,
        )
    }
}
