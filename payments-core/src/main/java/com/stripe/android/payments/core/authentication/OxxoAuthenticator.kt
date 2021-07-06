package com.stripe.android.payments.core.authentication

import com.stripe.android.model.StripeIntent
import com.stripe.android.model.StripeIntent.NextActionData
import com.stripe.android.networking.ApiRequest
import com.stripe.android.view.AuthActivityStarterHost
import javax.inject.Inject
import javax.inject.Singleton

/**
 * [IntentAuthenticator] for [NextActionData.DisplayOxxoDetails], redirects to
 * [WebIntentAuthenticator] or [NoOpIntentAuthenticator] based on whether if there is a
 * hostedVoucherUrl set.
 */
@Singleton
internal class OxxoAuthenticator @Inject constructor(
    private val webIntentAuthenticator: WebIntentAuthenticator,
    private val noOpIntentAuthenticator: NoOpIntentAuthenticator
) : IntentAuthenticator {
    override suspend fun authenticate(
        host: AuthActivityStarterHost,
        stripeIntent: StripeIntent,
        requestOptions: ApiRequest.Options
    ) {
        (stripeIntent.nextActionData as NextActionData.DisplayOxxoDetails).let { oxxoDetailsData ->
            if (oxxoDetailsData.hostedVoucherUrl == null) {
                noOpIntentAuthenticator.authenticate(
                    host,
                    stripeIntent,
                    requestOptions
                )
            } else {
                webIntentAuthenticator.authenticate(
                    host,
                    stripeIntent,
                    requestOptions
                )
            }
        }
    }
}
