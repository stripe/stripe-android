@file:OptIn(ExperimentalEmbeddedPaymentElementApi::class)

package com.stripe.android.paymentelement.embedded.content

import com.stripe.android.paymentelement.EmbeddedPaymentElement
import com.stripe.android.paymentelement.ExperimentalEmbeddedPaymentElementApi
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
        state.confirmationState.configuration.appearance.parseAppearance()
        confirmationStateHolder.state = state.confirmationState
        customerStateHolder.setCustomerState(state.customer)
        selectionHolder.set(
            if (shouldClearPaymentOptionForImmediateRowSelection(state)) {
                null
            } else {
                state.confirmationState.selection
            }
        )
        embeddedContentHelper.dataLoaded(
            paymentMethodMetadata = state.confirmationState.paymentMethodMetadata,
            rowStyle = state.confirmationState.configuration.appearance.embeddedAppearance.style,
            embeddedViewDisplaysMandateText = state.confirmationState.configuration.embeddedViewDisplaysMandateText,
        )
    }

    private fun shouldClearPaymentOptionForImmediateRowSelection(state: EmbeddedPaymentElement.State): Boolean {
        return internalRowSelectionCallback.get() != null &&
            state.confirmationState.configuration.formSheetAction == EmbeddedPaymentElement.FormSheetAction.Confirm
    }

    private fun clearState() {
        embeddedContentHelper.clearEmbeddedContent()
        confirmationStateHolder.state = null
        selectionHolder.set(null)
        customerStateHolder.setCustomerState(null)
    }
}
