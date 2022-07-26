package com.stripe.android

import com.stripe.android.model.LuxeActionCreatorForStatus
import com.stripe.android.model.LuxeNextActionRepository
import com.stripe.android.model.StripeIntent

val LUXE_NEXT_ACTION = LuxeNextActionRepository.LuxeAction(
    postConfirmStatusNextStatus = LuxeActionCreatorForStatus(
        StripeIntent.Status.RequiresAction,
        LuxeActionCreatorForStatus.ActionCreator.RedirectActionCreator(
            redirectPagePath = "next_action[oxxo_display_details][hosted_voucher_url]",
            returnToUrlPath = "next_action[oxxo_display_details][return_url]"
        )
    ),
    postAuthorizeIntentStatus = mapOf(
        StripeIntent.Status.Processing to StripeIntentResult.Outcome.SUCCEEDED
    )
)
