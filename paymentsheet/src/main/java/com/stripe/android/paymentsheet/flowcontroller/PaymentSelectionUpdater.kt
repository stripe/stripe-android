package com.stripe.android.paymentsheet.flowcontroller

import com.stripe.android.paymentsheet.model.PaymentSelection
import com.stripe.android.paymentsheet.model.getPMsToAdd
import com.stripe.android.paymentsheet.state.PaymentSheetState
import com.stripe.android.ui.core.forms.resources.LpmRepository
import javax.inject.Inject

internal fun interface PaymentSelectionUpdater {
    operator fun invoke(
        currentSelection: PaymentSelection?,
        newState: PaymentSheetState.Full,
    ): PaymentSelection?
}

internal class DefaultPaymentSelectionUpdater @Inject constructor(
    private val lpmRepository: LpmRepository,
) : PaymentSelectionUpdater {

    override operator fun invoke(
        currentSelection: PaymentSelection?,
        newState: PaymentSheetState.Full,
    ): PaymentSelection? {
        // TODO Null it out if some config properties have changed
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
        val availableTypes = getPMsToAdd(
            stripeIntent = state.stripeIntent,
            config = state.config,
            lpmRepository = lpmRepository,
        ).map { it.code }

        return when (selection) {
            is PaymentSelection.New -> {
                val code = selection.paymentMethodCreateParams.typeCode
                val requiresNoNewMandate = !shouldAskForMandate(selection)
                code in allowedTypes && code in availableTypes && requiresNoNewMandate
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

    private fun shouldAskForMandate(
        currentSelection: PaymentSelection.New,
    ): Boolean {
        val code = currentSelection.paymentMethodCreateParams.typeCode

        // This is the SupportedPaymentMethod based on the most recently fetched StripeIntent
        val paymentMethodRequiresMandate = lpmRepository.fromCode(code)!!.requiresMandate

        return if (paymentMethodRequiresMandate) {
            !currentSelection.customerAcknowledgedMandate
        } else {
            false
        }
    }
}

private val PaymentSelection.New.code: String
    get() = paymentMethodCreateParams.typeCode


private val PaymentSelection.New.customerAcknowledgedMandate: Boolean
    get() = paymentMethodCreateParams.requiresMandate()
