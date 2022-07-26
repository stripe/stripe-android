package com.stripe.android.model

import androidx.annotation.RestrictTo
import androidx.annotation.VisibleForTesting
import com.stripe.android.StripeIntentResult
import org.json.JSONObject

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
class LuxeNextActionRepository {

    private val codeToNextActionSpec = mutableMapOf<String, LuxeAction>()

    internal fun update(additionalData: Map<String, LuxeAction>) {
        codeToNextActionSpec.putAll(additionalData)
    }

    @VisibleForTesting
    internal fun isPresent(code: PaymentMethodCode) = codeToNextActionSpec.contains(code)

    /**
     * Given the PaymentIntent retrieved after the returnUrl (not redirectUrl), based on
     * the Payment Method code and Status of the Intent what is the [StripeIntentResult.Outcome]
     * of the operation.
     */
    internal fun getPostAuthorizeIntentOutcome(stripeIntent: StripeIntent) =
    // This handles the case where the next action is not understood so
        // the PI is still in the requires action state.
        if (stripeIntent.requiresAction() && stripeIntent.nextActionData != null) {
            StripeIntentResult.Outcome.FAILED
        } else {
            codeToNextActionSpec[stripeIntent.paymentMethod?.code]
                ?.postAuthorizeIntentStatus?.get(stripeIntent.status)
        }

    /**
     * Given the Intent returned from the confirm call, the payment method code and status
     * will be used to lookup the "instructions" for how to pull a next action from the
     * payment intent
     *
     * Return a [Result] that indicates if there is a next action, no next action, or
     * if it is not supported by the data in this repository.
     */
    internal fun getAction(
        lpmCode: PaymentMethodCode?,
        status: StripeIntent.Status?,
        stripeIntentJson: JSONObject
    ) = getActionCreator(lpmCode, status)
        ?.actionCreator?.create(stripeIntentJson)
        ?: Result.NotSupported

    private fun getActionCreator(lpmCode: PaymentMethodCode?, status: StripeIntent.Status?) =
        codeToNextActionSpec[lpmCode]?.postConfirmStatusNextStatus.takeIf { it?.status == status }

    companion object {
        @VisibleForTesting
        internal val DEFAULT_DATA = mapOf(
            "afterpay_clearpay" to
                LuxeAction(
                    postConfirmStatusNextStatus = LuxeActionCreatorForStatus(
                        StripeIntent.Status.RequiresAction,
                        LuxeActionCreatorForStatus.ActionCreator.RedirectActionCreator(
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
//            "konbini" to
//                LuxeAction(
//                    postConfirmStatusNextStatus = LuxeActionCreatorForStatus(
//                        StripeIntent.Status.RequiresAction,
//                        LuxeActionCreatorForStatus.ActionCreator.RedirectActionCreator(
//                            redirectPagePath = "next_action[konbini_display_details][hosted_voucher_url]",
//                            returnToUrlPath = null
//                        )
//                    ),
//                    postAuthorizeIntentStatus = mapOf(
//                        StripeIntent.Status.Succeeded to StripeIntentResult.Outcome.SUCCEEDED,
//                        StripeIntent.Status.Processing to StripeIntentResult.Outcome.SUCCEEDED,
//                        // If the next action is not understood and not processed, we will also
//                        // see a status of requires_action.  WHen you close the next action status is requires_action
////                        StripeIntent.Status.RequiresAction to StripeIntentResult.Outcome.SUCCEEDED,
//                    )
//                ),

            "sepa_debit" to
                LuxeAction(
                    postConfirmStatusNextStatus = LuxeActionCreatorForStatus(
                        StripeIntent.Status.Processing,
                        LuxeActionCreatorForStatus.ActionCreator.NoActionCreator
                    ),
                    postAuthorizeIntentStatus = mapOf(
                        StripeIntent.Status.Processing to StripeIntentResult.Outcome.SUCCEEDED
                    )
                )
        )

        val Instance: LuxeNextActionRepository = LuxeNextActionRepository()
//            .apply { update(DEFAULT_DATA) }
    }

    internal data class LuxeAction(
        /**
         * This should be null to use custom next action behavior coded in the SDK
         */
        val postConfirmStatusNextStatus: LuxeActionCreatorForStatus?,

        // Int here is @StripeIntentResult.Outcome
        val postAuthorizeIntentStatus: Map<StripeIntent.Status, Int>
    )

    internal sealed class Result {
        data class Action(val nextActionData: StripeIntent.NextActionData) : Result()
        object NoAction : Result()
        object NotSupported : Result()
    }
}
