package com.stripe.android.payments.core.authentication

import com.stripe.android.PaymentRelayStarter
import com.stripe.android.model.StripeIntent
import com.stripe.android.networking.ApiRequest
import com.stripe.android.view.AuthActivityStarterHost
import javax.inject.Inject
import javax.inject.Singleton

// Need relayLauncher
/**
 * [PaymentAuthenticator] implementation to perform no-op, just return to client's host.
 */
@Singleton
@JvmSuppressWildcards
internal class NoOpIntentAuthenticator @Inject constructor(
    private val paymentRelayStarterFactory: (AuthActivityStarterHost) -> PaymentRelayStarter,
) : PaymentAuthenticator<StripeIntent> {

    override suspend fun authenticate(
        host: AuthActivityStarterHost,
        authenticatable: StripeIntent,
        requestOptions: ApiRequest.Options
    ) {
        paymentRelayStarterFactory(host)
            .start(
                PaymentRelayStarter.Args.create(
                    authenticatable,
                    requestOptions.stripeAccount
                )
            )
    }
}
