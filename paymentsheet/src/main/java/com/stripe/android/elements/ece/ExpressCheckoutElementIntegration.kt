package com.stripe.android.elements.ece

import com.stripe.android.elements.ExpressCheckoutElement
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadata
import kotlinx.coroutines.flow.StateFlow

/** Supplies integration state and handles the actions behind the shared element. */
internal interface ExpressCheckoutElementIntegration {
    val state: StateFlow<State>

    fun onDisplayed()

    fun confirm(expressButton: ExpressButton)

    data class State(
        val content: Content?,
        val enabled: Boolean,
    )

    data class Content(
        val configuration: ExpressCheckoutElement.Configuration.State,
        val paymentMethodMetadata: PaymentMethodMetadata,
        val availableExpressButtonTypes: List<ExpressButtonType>,
        val requiresShippingAddress: Boolean,
    )
}
