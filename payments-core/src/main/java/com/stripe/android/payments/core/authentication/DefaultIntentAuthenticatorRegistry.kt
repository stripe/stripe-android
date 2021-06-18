package com.stripe.android.payments.core.authentication

import android.content.Context
import com.stripe.android.PaymentBrowserAuthStarter
import com.stripe.android.PaymentRelayStarter
import com.stripe.android.model.StripeIntent
import com.stripe.android.networking.AnalyticsRequestExecutor
import com.stripe.android.networking.AnalyticsRequestFactory
import com.stripe.android.networking.StripeRepository
import com.stripe.android.payments.Stripe3ds2CompletionStarter
import com.stripe.android.payments.core.injection.DaggerAuthenticationComponent
import com.stripe.android.payments.core.injection.IntentAuthenticatorMap
import com.stripe.android.view.AuthActivityStarterHost
import javax.inject.Inject
import kotlin.coroutines.CoroutineContext

/**
 * Default registry to provide look ups for [IntentAuthenticator].
 * Should be only accessed through [DefaultIntentAuthenticatorRegistry.createInstance].
 */
internal class DefaultIntentAuthenticatorRegistry @Inject internal constructor(
    private val noOpIntentAuthenticator: NoOpIntentAuthenticator,
    @IntentAuthenticatorMap
    private val intentAuthenticatorMap:
        Map<Class<out StripeIntent.NextActionData>, @JvmSuppressWildcards IntentAuthenticator>
) : IntentAuthenticatorRegistry {

    override fun getAuthenticator(stripeIntent: StripeIntent): IntentAuthenticator {
        if (!stripeIntent.requiresAction()) {
            return noOpIntentAuthenticator
        }

        return stripeIntent.nextActionData?.let {
            intentAuthenticatorMap
                .getOrElse(it::class.java) { noOpIntentAuthenticator }
        } ?: run {
            noOpIntentAuthenticator
        }
    }

    companion object {
        /**
         * Create an instance of [IntentAuthenticatorRegistry] with dagger.
         */
        fun createInstance(
            context: Context,
            stripeRepository: StripeRepository,
            paymentRelayStarterFactory: (AuthActivityStarterHost) -> PaymentRelayStarter,
            paymentBrowserAuthStarterFactory: (AuthActivityStarterHost) -> PaymentBrowserAuthStarter,
            stripe3ds2ChallengeLauncherFactory: (AuthActivityStarterHost, Int) -> Stripe3ds2CompletionStarter,
            analyticsRequestExecutor: AnalyticsRequestExecutor,
            analyticsRequestFactory: AnalyticsRequestFactory,
            enableLogging: Boolean,
            workContext: CoroutineContext,
            uiContext: CoroutineContext,
        ) = DaggerAuthenticationComponent.builder()
            .context(context)
            .stripeRepository(stripeRepository)
            .paymentRelayStarterFactory(paymentRelayStarterFactory)
            .paymentBrowserAuthStarterFactory(paymentBrowserAuthStarterFactory)
            .stripe3ds2ChallengeLauncherFactory(stripe3ds2ChallengeLauncherFactory)
            .analyticsRequestExecutor(analyticsRequestExecutor)
            .analyticsRequestFactory(analyticsRequestFactory)
            .enableLogging(enableLogging)
            .workContext(workContext)
            .uiContext(uiContext)
            .build()
            .registry
    }
}
