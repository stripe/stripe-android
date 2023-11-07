package com.stripe.android.paymentsheet.viewmodels

import com.stripe.android.model.PaymentMethod
import com.stripe.android.model.PaymentMethodCode
import com.stripe.android.paymentsheet.PaymentOptionsState
import com.stripe.android.paymentsheet.PaymentOptionsStateFactory
import com.stripe.android.paymentsheet.model.PaymentSelection
import com.stripe.android.paymentsheet.state.GooglePayState
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine

internal class PaymentOptionsStateMapper(
    private val paymentMethods: StateFlow<List<PaymentMethod>?>,
    private val googlePayState: StateFlow<GooglePayState>,
    private val isLinkEnabled: StateFlow<Boolean?>,
    private val currentSelection: StateFlow<PaymentSelection?>,
    private val nameProvider: (PaymentMethodCode?) -> String,
    private val isNotPaymentFlow: Boolean,
    private val isCbcEligible: () -> Boolean
) {

    operator fun invoke(): Flow<PaymentOptionsState?> {
        return combine(
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
            )
        }
    }

    @Suppress("ReturnCount")
    private fun createPaymentOptionsState(
        paymentMethods: List<PaymentMethod>?,
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
            nameProvider = nameProvider,
            isCbcEligible = isCbcEligible()
        )
    }
}
