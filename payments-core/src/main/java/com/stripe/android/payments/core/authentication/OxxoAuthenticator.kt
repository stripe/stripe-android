package com.stripe.android.payments.core.authentication

import com.stripe.android.model.StripeIntent
import com.stripe.android.model.StripeIntent.NextActionData
import com.stripe.android.networking.ApiRequest
import com.stripe.android.view.AuthActivityStarterHost
import javax.inject.Inject
import javax.inject.Singleton

/**
 * [PaymentAuthenticator] for [NextActionData.DisplayOxxoDetails], redirects to
 * [WebIntentAuthenticator] or [NoOpIntentAuthenticator] based on whether if there is a
 * hostedVoucherUrl set.
 */
@Singleton
internal class OxxoAuthenticator @Inject constructor(
    private val webIntentAuthenticator: WebIntentAuthenticator,
    private val noOpIntentAuthenticator: NoOpIntentAuthenticator
) : PaymentAuthenticator<StripeIntent> {
    override suspend fun authenticate(
        host: AuthActivityStarterHost,
        authenticatable: StripeIntent,
        requestOptions: ApiRequest.Options
    ) {
        (authenticatable.nextActionData as NextActionData.DisplayOxxoDetails).let { oxxoDetailsData ->
            if (oxxoDetailsData.hostedVoucherUrl == null) {
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
}
