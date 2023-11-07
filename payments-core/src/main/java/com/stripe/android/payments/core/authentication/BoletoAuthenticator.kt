package com.stripe.android.payments.core.authentication

import com.stripe.android.core.networking.ApiRequest
import com.stripe.android.model.StripeIntent
import com.stripe.android.model.StripeIntent.NextActionData
import com.stripe.android.view.AuthActivityStarterHost
import javax.inject.Inject
import javax.inject.Singleton

/**
 * [PaymentAuthenticator] for [NextActionData.DisplayBoletoDetails], redirects to
 * [WebIntentAuthenticator] or [NoOpIntentAuthenticator] based on whether if there is a
 * hostedVoucherUrl set.
 */
@Singleton
internal class BoletoAuthenticator @Inject constructor(
    private val webIntentAuthenticator: WebIntentAuthenticator,
    private val noOpIntentAuthenticator: NoOpIntentAuthenticator
) : PaymentAuthenticator<StripeIntent>() {
    override suspend fun performAuthentication(
        host: AuthActivityStarterHost,
        authenticatable: StripeIntent,
        requestOptions: ApiRequest.Options
    ) {
        (authenticatable.nextActionData as NextActionData.DisplayBoletoDetails).let { detailsData ->
            if (detailsData.hostedVoucherUrl == null) {
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
