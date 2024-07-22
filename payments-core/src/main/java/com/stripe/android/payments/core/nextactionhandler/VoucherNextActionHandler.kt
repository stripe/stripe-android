package com.stripe.android.payments.core.nextactionhandler

import android.content.Context
import com.stripe.android.core.networking.ApiRequest
import com.stripe.android.model.StripeIntent
import com.stripe.android.model.StripeIntent.NextActionData
import com.stripe.android.payments.core.analytics.ErrorReporter
import com.stripe.android.view.AuthActivityStarterHost
import javax.inject.Inject
import javax.inject.Singleton

/**
 * [PaymentNextActionHandler] for [NextActionData.DisplayVoucherDetails], redirects to
 * [WebIntentNextActionHandler] or [NoOpIntentNextActionHandler] based on whether if there is a
 * hostedVoucherUrl set.
 */
@Singleton
internal class VoucherNextActionHandler @Inject constructor(
    private val webIntentAuthenticator: WebIntentNextActionHandler,
    private val noOpIntentAuthenticator: NoOpIntentNextActionHandler,
    private val context: Context,
) : PaymentNextActionHandler<StripeIntent>() {
    override suspend fun performNextAction(
        host: AuthActivityStarterHost,
        actionable: StripeIntent,
        requestOptions: ApiRequest.Options
    ) {
        val detailsData = actionable.nextActionData as NextActionData.DisplayVoucherDetails
        if (detailsData.hostedVoucherUrl == null) {
            ErrorReporter.createFallbackInstance(context).report(
                ErrorReporter.UnexpectedErrorEvent.MISSING_HOSTED_VOUCHER_URL,
                additionalNonPiiParams = mapOf("next_action_type" to (actionable.nextActionType?.code ?: ""))
            )
            noOpIntentAuthenticator.nextAction(
                host,
                actionable,
                requestOptions
            )
        } else {
            webIntentAuthenticator.nextAction(
                host,
                actionable,
                requestOptions
            )
        }
    }
}
