package com.stripe.android.checkout

import androidx.lifecycle.SavedStateHandle
import com.stripe.android.paymentelement.confirmation.ConfirmationHandler
import com.stripe.android.paymentsheet.analytics.EventReporter
import javax.inject.Inject

// TODO-codex: hook up dependency inject for this.
internal class CheckoutAnalyticsPerformer @Inject constructor(
    private val confirmationHander: ConfirmationHandler,
    private val eventReporter: EventReporter,
    private val savedStateHandle: SavedStateHandle,
) {
    // TODO-codex: save in the saved state handle: integration type (payment element vs. express checkout element)
    // TODO-codex: save in the saved state handle: the payment selection
    // TODO-codex: checkout performers should set the above values when they kick off confirmation, then they should get cleared after confirmation finishes

    suspend fun reportConfirmationResults() {
        confirmationHander.state.collect { state ->
            if (state is ConfirmationHandler.State.Complete) {
                // TODO-codex: report via eventReporter reportPaymentResult
            }
        }
    }
}