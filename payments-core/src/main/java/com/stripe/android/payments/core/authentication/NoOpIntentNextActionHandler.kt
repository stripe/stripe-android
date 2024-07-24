package com.stripe.android.payments.core.authentication

import com.stripe.android.PaymentRelayStarter
import com.stripe.android.core.networking.ApiRequest
import com.stripe.android.model.StripeIntent
import com.stripe.android.view.AuthActivityStarterHost
import javax.inject.Inject
import javax.inject.Singleton

/**
 * [PaymentNextActionHandler] implementation to perform no-op, just return to client's host.
 */
@Singleton
@JvmSuppressWildcards
internal class NoOpIntentNextActionHandler @Inject constructor(
    private val paymentRelayStarterFactory: (AuthActivityStarterHost) -> PaymentRelayStarter
) : PaymentNextActionHandler<StripeIntent>() {

    override suspend fun performNextActionOnResumed(
        host: AuthActivityStarterHost,
        actionable: StripeIntent,
        requestOptions: ApiRequest.Options
    ) {
        val args = PaymentRelayStarter.Args.create(
            stripeIntent = actionable,
            stripeAccountId = requestOptions.stripeAccount,
        )
        paymentRelayStarterFactory(host).start(args)
    }
}
