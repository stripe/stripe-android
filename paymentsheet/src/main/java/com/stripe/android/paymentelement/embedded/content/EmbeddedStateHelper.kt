package com.stripe.android.paymentelement.embedded.content

import com.stripe.android.paymentelement.EmbeddedPaymentElement
import com.stripe.android.paymentelement.embedded.EmbeddedSelectionHolder
import com.stripe.android.paymentelement.embedded.InternalRowSelectionCallback
import com.stripe.android.paymentsheet.CustomerStateHolder
import com.stripe.android.paymentsheet.parseAppearance
import javax.inject.Inject
import javax.inject.Provider

internal interface EmbeddedStateHelper {
    var state: EmbeddedPaymentElement.State?
}

internal class DefaultEmbeddedStateHelper @Inject constructor(
    private val selectionHolder: EmbeddedSelectionHolder,
    private val customerStateHolder: CustomerStateHolder,
    private val confirmationStateHolder: EmbeddedConfirmationStateHolder,
    private val embeddedContentHelper: EmbeddedContentHelper,
    private val internalRowSelectionCallback: Provider<InternalRowSelectionCallback?>,
) : EmbeddedStateHelper {
    override var state: EmbeddedPaymentElement.State?
        get() {
            return confirmationStateHolder.state?.let {
                EmbeddedPaymentElement.State(
                    confirmationState = it,
                    customer = customerStateHolder.customer.value,
                    previousNewSelections = selectionHolder.previousNewSelections,
                )
            }
        }
        set(value) {
            if (value != null) {
                handleLoadedState(value)
            } else {
                clearState()
            }
        }

    private fun handleLoadedState(
        state: EmbeddedPaymentElement.State,
    ) {
        validateRowSelectionBehaviorConfiguration(
            configuration = state.confirmationState.configuration
        )

        state.confirmationState.configuration.appearance.parseAppearance()
        confirmationStateHolder.state = state.confirmationState
        customerStateHolder.setCustomerState(state.customer)
        selectionHolder.setPreviousNewSelections(state.previousNewSelections)
        selectionHolder.set(state.confirmationState.selection)
        embeddedContentHelper.dataLoaded(
            paymentMethodMetadata = state.confirmationState.paymentMethodMetadata,
            appearance = state.confirmationState.configuration.appearance.embeddedAppearance,
            embeddedViewDisplaysMandateText = state.confirmationState.configuration.embeddedViewDisplaysMandateText,
        )
    }

    private fun validateRowSelectionBehaviorConfiguration(
        configuration: EmbeddedPaymentElement.Configuration
    ) {
        val isRowSelectionImmediateAction = internalRowSelectionCallback.get() != null
        val hasGooglePayOrCustomerConfig = configuration.googlePay != null || configuration.customer != null

        if (isRowSelectionImmediateAction &&
            configuration.formSheetAction == EmbeddedPaymentElement.FormSheetAction.Confirm &&
            hasGooglePayOrCustomerConfig
        ) {
            throw IllegalArgumentException(
                "Using RowSelectionBehavior.ImmediateAction with FormSheetAction.Confirm is not supported " +
                    "when Google Pay or a customer configuration is provided. " +
                    "Use RowSelectionBehavior.Default or disable Google Pay and saved payment methods."
            )
        }
    }

    private fun clearState() {
        embeddedContentHelper.clearEmbeddedContent()
        confirmationStateHolder.state = null
        selectionHolder.set(null)
        customerStateHolder.setCustomerState(null)
    }
}
