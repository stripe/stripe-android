package com.stripe.android

import com.stripe.android.model.LuxeActionCreator
import com.stripe.android.model.LuxeConfirmResponseActionRepository
import com.stripe.android.model.StripeIntent

internal val LUXE_NEXT_ACTION = LuxeConfirmResponseActionRepository.LuxeAction(
    postConfirmStatusNextStatus = mapOf(
        StripeIntent.Status.RequiresAction to
            LuxeActionCreator.RedirectActionCreator(
                redirectPagePath = "next_action[oxxo_display_details][hosted_voucher_url]",
                returnToUrlPath = "next_action[oxxo_display_details][return_url]"
            )
    ),
    postAuthorizeIntentStatus = mapOf(
        StripeIntent.Status.Processing to StripeIntentResult.Outcome.SUCCEEDED
    )
)

internal val DEFAULT_DATA = mapOf(
    "afterpay_clearpay" to
        LuxeConfirmResponseActionRepository.LuxeAction(
            postConfirmStatusNextStatus = mapOf(
                StripeIntent.Status.RequiresAction to
                    LuxeActionCreator.RedirectActionCreator(
                        redirectPagePath = "next_action[redirect_to_url][url]",
                        returnToUrlPath = "next_action[redirect_to_url][return_url]"
                    )
            ),
            postAuthorizeIntentStatus = mapOf(
                StripeIntent.Status.Succeeded to StripeIntentResult.Outcome.SUCCEEDED,
                StripeIntent.Status.RequiresPaymentMethod to StripeIntentResult.Outcome.FAILED,
                StripeIntent.Status.RequiresAction to StripeIntentResult.Outcome.CANCELED
            )
        ),
    "sepa_debit" to
        LuxeConfirmResponseActionRepository.LuxeAction(
            postConfirmStatusNextStatus = mapOf(
                StripeIntent.Status.Processing to
                    LuxeActionCreator.NoActionCreator
            ),
            postAuthorizeIntentStatus = mapOf(
                StripeIntent.Status.Processing to StripeIntentResult.Outcome.SUCCEEDED
            )
        )
)
