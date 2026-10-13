package com.stripe.android.payments.core.authentication

import android.content.Context
import com.stripe.android.core.ApiConfiguration
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
    override suspend fun performNextActionOnResumed(
        host: AuthActivityStarterHost,
        actionable: StripeIntent,
        apiConfiguration: ApiConfiguration.State
    ) {
        val detailsData = actionable.nextActionData as NextActionData.DisplayVoucherDetails
        if (detailsData.hostedVoucherUrl == null) {
            ErrorReporter.createFallbackInstance(
                context,
                apiConfigurationProvider = { apiConfiguration },
            ).report(
                ErrorReporter.UnexpectedErrorEvent.MISSING_HOSTED_VOUCHER_URL,
                additionalNonPiiParams = mapOf("next_action_type" to (actionable.nextActionType?.code ?: ""))
            )
            noOpIntentAuthenticator.performNextAction(
                host,
                actionable,
                apiConfiguration
            )
        } else {
            webIntentAuthenticator.performNextAction(
                host,
                actionable,
                apiConfiguration
            )
        }
    }
}
