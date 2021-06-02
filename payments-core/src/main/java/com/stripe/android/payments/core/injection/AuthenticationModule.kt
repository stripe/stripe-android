package com.stripe.android.payments.core.injection

import androidx.activity.result.ActivityResultLauncher
import com.stripe.android.Logger
import com.stripe.android.PaymentAuthConfig
import com.stripe.android.PaymentBrowserAuthStarter
import com.stripe.android.PaymentRelayStarter
import com.stripe.android.StripePaymentController
import com.stripe.android.networking.AnalyticsRequestExecutor
import com.stripe.android.networking.AnalyticsRequestFactory
import com.stripe.android.networking.StripeRepository
import com.stripe.android.payments.PaymentFlowResult
import com.stripe.android.payments.core.authentication.NoOpIntentAuthenticator
import com.stripe.android.payments.core.authentication.Stripe3DS2Authenticator
import com.stripe.android.payments.core.authentication.WebIntentAuthenticator
import com.stripe.android.stripe3ds2.service.StripeThreeDs2Service
import com.stripe.android.stripe3ds2.transaction.MessageVersionRegistry
import com.stripe.android.view.AuthActivityStarter
import dagger.Module
import dagger.Provides
import javax.inject.Singleton
import kotlin.coroutines.CoroutineContext

/**
 * [Module] for com.stripe.android.payments.core.authentication.
 *
 * All non-daggerized dependencies are passed in as constructor parameters for now, a parameter
 * will be removed when it's daggerized and introduced to the dagger graph.
 */
@Module
internal class AuthenticationModule(
    private val stripeRepository: StripeRepository,
    private val paymentRelayStarterFactory: (AuthActivityStarter.Host) -> PaymentRelayStarter,
    private val paymentBrowserAuthStarterFactory: (AuthActivityStarter.Host) -> PaymentBrowserAuthStarter,
    private val analyticsRequestExecutor: AnalyticsRequestExecutor,
    private val analyticsRequestFactory: AnalyticsRequestFactory,
    private val logger: Logger,
    private val enableLogging: Boolean,
    private val workContext: CoroutineContext,
    private val uiContext: CoroutineContext,
    private val threeDs2Service: StripeThreeDs2Service,
    private val messageVersionRegistry: MessageVersionRegistry,
    private val challengeProgressActivityStarter: StripePaymentController.ChallengeProgressActivityStarter,
    private val stripe3ds2Config: PaymentAuthConfig.Stripe3ds2Config,
    private val stripe3ds2ChallengeLauncher: ActivityResultLauncher<PaymentFlowResult.Unvalidated>?,
) {
    @Singleton
    @Provides
    internal fun provideNoOpAuthenticator(): NoOpIntentAuthenticator {
        return NoOpIntentAuthenticator(paymentRelayStarterFactory)
    }

    @Singleton
    @Provides
    internal fun provideWebAuthenticator(): WebIntentAuthenticator {
        return WebIntentAuthenticator(
            paymentBrowserAuthStarterFactory,
            analyticsRequestExecutor,
            analyticsRequestFactory,
            logger,
            enableLogging,
            uiContext,
        )
    }

    @Singleton
    @Provides
    internal fun provideStripe3DSAuthenticator(
        webIntentAuthenticator: WebIntentAuthenticator
    ): Stripe3DS2Authenticator {
        return Stripe3DS2Authenticator(
            stripeRepository,
            webIntentAuthenticator,
            paymentRelayStarterFactory,
            analyticsRequestExecutor,
            analyticsRequestFactory,
            threeDs2Service,
            messageVersionRegistry,
            challengeProgressActivityStarter,
            stripe3ds2Config,
            stripe3ds2ChallengeLauncher,
            workContext,
            uiContext
        )
    }
}
