package com.stripe.android.paymentsheet.viewmodels

import com.stripe.android.paymentsheet.DisplayableSavedPaymentMethod
import com.stripe.android.paymentsheet.PaymentOptionsState
import com.stripe.android.paymentsheet.PaymentOptionsStateFactory
import com.stripe.android.paymentsheet.model.PaymentSelection
import com.stripe.android.paymentsheet.state.GooglePayState
import com.stripe.android.uicore.utils.combineAsStateFlow
import kotlinx.coroutines.flow.StateFlow

internal class PaymentOptionsStateMapper(
    private val paymentMethods: StateFlow<List<DisplayableSavedPaymentMethod>?>,
    private val googlePayState: StateFlow<GooglePayState>,
    private val isLinkEnabled: StateFlow<Boolean?>,
    private val currentSelection: StateFlow<PaymentSelection?>,
    private val isNotPaymentFlow: Boolean,
) {

    operator fun invoke(): StateFlow<PaymentOptionsState> {
        return combineAsStateFlow(
            paymentMethods,
            currentSelection,
            isLinkEnabled,
            googlePayState,
        ) { paymentMethods, currentSelection, isLinkEnabled, googlePayState ->
            createPaymentOptionsState(
                paymentMethods = paymentMethods,
                currentSelection = currentSelection,
                isLinkEnabled = isLinkEnabled,
                googlePayState = googlePayState,
            ) ?: PaymentOptionsState()
        }
    }

    @Suppress("ReturnCount")
    private fun createPaymentOptionsState(
        paymentMethods: List<DisplayableSavedPaymentMethod>?,
        currentSelection: PaymentSelection?,
        isLinkEnabled: Boolean?,
        googlePayState: GooglePayState,
    ): PaymentOptionsState? {
        if (paymentMethods == null) return null
        if (isLinkEnabled == null) return null

        return PaymentOptionsStateFactory.create(
            paymentMethods = paymentMethods,
            showGooglePay = (googlePayState is GooglePayState.Available) && isNotPaymentFlow,
            showLink = isLinkEnabled && isNotPaymentFlow,
            currentSelection = currentSelection,
        )
    }
}
