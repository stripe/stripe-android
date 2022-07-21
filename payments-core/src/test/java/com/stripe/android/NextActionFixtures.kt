package com.stripe.android

import com.stripe.android.model.LuxeNextActionRepository
import com.stripe.android.model.StripeIntent

val LUXE_NEXT_ACTION = LuxeNextActionRepository.LuxeNextAction(
    handleNextActionSpec = mapOf(
        StripeIntent.Status.RequiresAction to
            LuxeNextActionRepository.RedirectNextActionSpec(
                hostedPagePath = "next_action[oxxo_display_details][hosted_voucher_url]",
                returnToUrlPath = "next_action[oxxo_display_details][return_url]"
            )
    ),
    handlePiStatus = listOf(
        LuxeNextActionRepository.PiStatusSpec(
            associatedStatuses = listOf(StripeIntent.Status.Processing),
            outcome = StripeIntentResult.Outcome.SUCCEEDED
        )
    )
)
