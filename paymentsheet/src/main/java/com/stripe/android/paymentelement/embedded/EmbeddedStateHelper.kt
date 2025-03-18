@file:OptIn(ExperimentalEmbeddedPaymentElementApi::class)

package com.stripe.android.paymentelement.embedded

import com.stripe.android.paymentelement.EmbeddedPaymentElement
import com.stripe.android.paymentelement.ExperimentalEmbeddedPaymentElementApi
import com.stripe.android.paymentelement.embedded.content.EmbeddedConfirmationStateHolder
import com.stripe.android.paymentelement.embedded.content.EmbeddedContentHelper
import com.stripe.android.paymentsheet.CustomerStateHolder
import com.stripe.android.paymentsheet.parseAppearance
import javax.inject.Inject

internal interface EmbeddedStateHelper {
    var state: EmbeddedPaymentElement.State?
}

internal class DefaultEmbeddedStateHelper @Inject constructor(
    private val confirmationStateHolder: EmbeddedConfirmationStateHolder,
    private val customerStateHolder: CustomerStateHolder,
    private val selectionHolder: EmbeddedSelectionHolder,
    private val embeddedContentHelper: EmbeddedContentHelper,
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
        selectionHolder.set(state.confirmationState.selection)
        embeddedContentHelper.dataLoaded(
            paymentMethodMetadata = state.confirmationState.paymentMethodMetadata,
            rowStyle = state.confirmationState.configuration.appearance.embeddedAppearance.style,
            embeddedViewDisplaysMandateText = state.confirmationState.configuration.embeddedViewDisplaysMandateText,
        )
    }

    private fun clearState() {
        embeddedContentHelper.clearEmbeddedContent()
        confirmationStateHolder.state = null
        selectionHolder.set(null)
    }
}
