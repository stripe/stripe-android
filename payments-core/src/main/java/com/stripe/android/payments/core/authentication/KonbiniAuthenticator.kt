package com.stripe.android.payments.core.authentication

import android.content.Context
import com.stripe.android.core.networking.ApiRequest
import com.stripe.android.model.StripeIntent
import com.stripe.android.model.StripeIntent.NextActionData
import com.stripe.android.payments.core.analytics.ErrorReporter
import com.stripe.android.view.AuthActivityStarterHost
import javax.inject.Inject
import javax.inject.Singleton

/**
 * [PaymentAuthenticator] for [NextActionData.DisplayKonbiniDetails], redirects to
 * [WebIntentAuthenticator] or [NoOpIntentAuthenticator] based on whether if there is a
 * hostedVoucherUrl set.
 */
@Singleton
internal class KonbiniAuthenticator @Inject constructor(
    private val webIntentAuthenticator: WebIntentAuthenticator,
    private val noOpIntentAuthenticator: NoOpIntentAuthenticator,
    private val context: Context,
) : PaymentAuthenticator<StripeIntent>() {
    override suspend fun performAuthentication(
        host: AuthActivityStarterHost,
        authenticatable: StripeIntent,
        requestOptions: ApiRequest.Options
    ) {
        val detailsData = authenticatable.nextActionData as NextActionData.DisplayKonbiniDetails
        if (detailsData.hostedVoucherUrl == null) {
            ErrorReporter.createFallbackInstance(context, emptySet()).report(
                ErrorReporter.UnexpectedErrorEvent.MISSING_HOSTED_VOUCHER_URL,
                additionalNonPiiParams = mapOf("lpm" to "konbini")
            )
            noOpIntentAuthenticator.authenticate(
                host,
                authenticatable,
                requestOptions
            )
        } else {
            webIntentAuthenticator.authenticate(
                host,
                authenticatable,
                requestOptions
            )
        }
    }
}
