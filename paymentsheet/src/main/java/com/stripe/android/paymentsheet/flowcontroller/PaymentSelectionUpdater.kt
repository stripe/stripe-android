package com.stripe.android.paymentsheet.flowcontroller

import com.stripe.android.paymentsheet.model.PaymentSelection
import com.stripe.android.paymentsheet.model.getAvailablePaymentMethods
import com.stripe.android.paymentsheet.state.PaymentSheetState
import com.stripe.android.ui.core.forms.resources.LpmRepository
import javax.inject.Inject

internal class PaymentSelectionUpdater @Inject constructor(
    private val lpmRepository: LpmRepository,
) {

    operator fun invoke(
        currentSelection: PaymentSelection?,
        newState: PaymentSheetState.Full,
    ): PaymentSelection? {
        return currentSelection?.takeIf { selection ->
            canUseSelection(selection, newState)
        } ?: newState.paymentSelection
    }

    private fun canUseSelection(
        selection: PaymentSelection,
        state: PaymentSheetState.Full,
    ): Boolean {
        // The types that are allowed for this intent, as returned by the backend
        val allowedTypes = state.stripeIntent.paymentMethodTypes

        // The types that we actually do support for this intent and configuration
        val availableTypes = lpmRepository.getAvailablePaymentMethods(
            stripeIntent = state.stripeIntent,
            config = state.config,
        ).map { it.code }

        return when (selection) {
            is PaymentSelection.New -> {
                val code = selection.paymentMethodCreateParams.typeCode
                code in allowedTypes && code in availableTypes
            }
            is PaymentSelection.Saved -> {
                val paymentMethod = selection.paymentMethod
                val code = paymentMethod.type?.code
                code in allowedTypes && code in availableTypes && paymentMethod in state.customerPaymentMethods
            }
            is PaymentSelection.GooglePay -> {
                state.isGooglePayReady
            }
            is PaymentSelection.Link -> {
                state.linkState != null
            }
        }
    }
}
