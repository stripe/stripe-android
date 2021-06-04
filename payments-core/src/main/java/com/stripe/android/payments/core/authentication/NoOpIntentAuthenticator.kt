package com.stripe.android.payments.core.authentication

import com.stripe.android.PaymentRelayStarter
import com.stripe.android.model.StripeIntent
import com.stripe.android.networking.ApiRequest
import com.stripe.android.view.AuthActivityStarter

/**
 * [IntentAuthenticator] implementation to perform no-op, just return to client's host.
 */
internal class NoOpIntentAuthenticator(
    private val paymentRelayStarterFactory: (AuthActivityStarter.Host) -> PaymentRelayStarter,
) : IntentAuthenticator {

    override suspend fun authenticate(
        host: AuthActivityStarter.Host,
        stripeIntent: StripeIntent,
        threeDs1ReturnUrl: String?,
        requestOptions: ApiRequest.Options
    ) {
        paymentRelayStarterFactory(host)
            .start(
                PaymentRelayStarter.Args.create(stripeIntent, requestOptions.stripeAccount)
            )
    }
}
