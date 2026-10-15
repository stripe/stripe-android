@file:OptIn(CheckoutSessionPreview::class)

package com.stripe.android.checkout

import androidx.lifecycle.SavedStateHandle
import com.stripe.android.checkout.injection.CheckoutIsUpdating
import com.stripe.android.elements.ece.ExpressButton
import com.stripe.android.elements.ece.ExpressCheckoutElementConfirmationPerformer
import com.stripe.android.elements.ece.ExpressCheckoutElementEventReporter
import com.stripe.android.elements.ece.ExpressCheckoutElementIntegration
import com.stripe.android.paymentelement.CheckoutSessionPreview
import com.stripe.android.uicore.utils.combineAsStateFlow
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

internal class CheckoutExpressCheckoutElementIntegration @Inject constructor(
    stateHolder: CheckoutControllerStateHolder,
    private val savedStateHandle: SavedStateHandle,
    private val eventReporter: ExpressCheckoutElementEventReporter,
    private val confirmationPerformer: ExpressCheckoutElementConfirmationPerformer,
    @CheckoutIsUpdating isUpdating: StateFlow<Boolean>,
) : ExpressCheckoutElementIntegration {
    override val state: StateFlow<ExpressCheckoutElementIntegration.State> = combineAsStateFlow(
        stateHolder.stateFlow,
        stateHolder.session,
        isUpdating,
    ) { state, session, isUpdating ->
        val configuration = state?.configuration?.expressCheckoutElementConfiguration
        val paymentMethodMetadata = state?.expressCheckoutElementPaymentMethodMetadata
        ExpressCheckoutElementIntegration.State(
            content = if (configuration != null && paymentMethodMetadata != null && session != null) {
                ExpressCheckoutElementIntegration.Content(
                    configuration = configuration,
                    paymentMethodMetadata = paymentMethodMetadata,
                    availableExpressButtonTypes = session.availableExpressButtonTypes,
                    requiresShippingAddress = state.checkoutSessionResponse.requiresShippingAddress,
                )
            } else {
                null
            },
            enabled = !isUpdating,
        )
    }

    override fun onDisplayed() {
        if (savedStateHandle.get<Boolean>(KEY_ECE_DISPLAYED) != true) {
            savedStateHandle[KEY_ECE_DISPLAYED] = true
            eventReporter.onEceDisplayed(state.value.content)
        }
    }

    override fun confirm(expressButton: ExpressButton) {
        confirmationPerformer.confirm(expressButton)
    }

    private companion object {
        const val KEY_ECE_DISPLAYED = "express_checkout_element_displayed"
    }
}
