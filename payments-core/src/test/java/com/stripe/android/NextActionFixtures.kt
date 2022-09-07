package com.stripe.android

import com.stripe.android.model.LuxePostConfirmActionCreator
import com.stripe.android.model.LuxePostConfirmActionRepository
import com.stripe.android.model.StripeIntent

internal val LUXE_NEXT_ACTION = LuxePostConfirmActionRepository.LuxeAction(
    postConfirmStatusToAction = mapOf(
        StripeIntent.Status.RequiresAction to
            LuxePostConfirmActionCreator.RedirectActionCreator(
                redirectPagePath = "next_action[oxxo_display_details][hosted_voucher_url]",
                returnToUrlPath = "next_action[oxxo_display_details][return_url]"
            )
    ),
    postConfirmActionIntentStatus = mapOf(
        StripeIntent.Status.Processing to StripeIntentResult.Outcome.SUCCEEDED
    )
)

internal val DEFAULT_DATA = mapOf(
    "afterpay_clearpay" to
        LuxePostConfirmActionRepository.LuxeAction(
            postConfirmStatusToAction = mapOf(
                StripeIntent.Status.RequiresAction to
                    LuxePostConfirmActionCreator.RedirectActionCreator(
                        redirectPagePath = "next_action[redirect_to_url][url]",
                        returnToUrlPath = "next_action[redirect_to_url][return_url]"
                    )
            ),
            postConfirmActionIntentStatus = mapOf(
                StripeIntent.Status.Succeeded to StripeIntentResult.Outcome.SUCCEEDED,
                StripeIntent.Status.RequiresPaymentMethod to StripeIntentResult.Outcome.FAILED,
                StripeIntent.Status.RequiresAction to StripeIntentResult.Outcome.CANCELED
            )
        ),
    "sepa_debit" to
        LuxePostConfirmActionRepository.LuxeAction(
            postConfirmStatusToAction = mapOf(
                StripeIntent.Status.Processing to
                    LuxePostConfirmActionCreator.NoActionCreator
            ),
            postConfirmActionIntentStatus = mapOf(
                StripeIntent.Status.Processing to StripeIntentResult.Outcome.SUCCEEDED
            )
        )
)
