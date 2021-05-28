package com.stripe.android.payments.core.authentication

import androidx.activity.result.ActivityResultLauncher
import com.stripe.android.Logger
import com.stripe.android.PaymentAuthConfig
import com.stripe.android.PaymentBrowserAuthStarter
import com.stripe.android.PaymentRelayStarter
import com.stripe.android.StripePaymentController
import com.stripe.android.model.StripeIntent
import com.stripe.android.networking.AnalyticsRequestExecutor
import com.stripe.android.networking.AnalyticsRequestFactory
import com.stripe.android.networking.StripeRepository
import com.stripe.android.payments.PaymentFlowResult
import com.stripe.android.payments.core.injection.AuthenticationModule
import com.stripe.android.payments.core.injection.DaggerAuthenticationComponent
import com.stripe.android.stripe3ds2.service.StripeThreeDs2Service
import com.stripe.android.stripe3ds2.transaction.MessageVersionRegistry
import com.stripe.android.view.AuthActivityStarter
import javax.inject.Inject
import kotlin.coroutines.CoroutineContext

/**
 * Default registry to provide look ups for [IntentAuthenticator].
 * Should be only accessed through [DefaultIntentAuthenticatorRegistry.getInstance].
 */
internal class DefaultIntentAuthenticatorRegistry @Inject internal constructor() :
    IntentAuthenticatorRegistry {

    // default authenticators always packaged with the SDK
    @Inject
    lateinit var webIntentAuthenticator: WebIntentAuthenticator

    @Inject
    lateinit var noOpIntentAuthenticator: NoOpIntentAuthenticator

    // authenticators requiring a 3p SDK
    // TODO(ccen) move them to a dedicated module
    @Inject
    lateinit var stripe3DS2Authenticator: Stripe3DS2Authenticator

    override fun lookUp(stripeIntent: StripeIntent): IntentAuthenticator {
        if (!stripeIntent.requiresAction()) {
            return noOpIntentAuthenticator
        }

        when (val nextActionData = stripeIntent.nextActionData) {
            is StripeIntent.NextActionData.SdkData.Use3DS2 -> {
                return stripe3DS2Authenticator
            }
            is StripeIntent.NextActionData.SdkData.Use3DS1 -> {
                // can only triggered when `use_stripe_sdk=true`
                return webIntentAuthenticator
            }
            is StripeIntent.NextActionData.RedirectToUrl -> {
                // can only triggered when `use_stripe_sdk=false`
                return webIntentAuthenticator
            }
            is StripeIntent.NextActionData.AlipayRedirect -> {
                return webIntentAuthenticator
            }
            is StripeIntent.NextActionData.DisplayOxxoDetails -> {
                return webIntentAuthenticator.takeIf { nextActionData.hostedVoucherUrl != null }
                    ?: noOpIntentAuthenticator
            }
            else -> return noOpIntentAuthenticator
        }
    }

    companion object {
        // Holding this single instance would in turn holds DaggerAuthenticationComponent instance,
        // which keeps dagger injection graph live.
        private var instance: IntentAuthenticatorRegistry? = null

        /**
         * Return the singleton instance of [IntentAuthenticatorRegistry].
         */
        fun getInstance(
            stripeRepository: StripeRepository,
            paymentRelayStarterFactory: (AuthActivityStarter.Host) -> PaymentRelayStarter,
            paymentBrowserAuthStarterFactory: (AuthActivityStarter.Host) -> PaymentBrowserAuthStarter,
            analyticsRequestExecutor: AnalyticsRequestExecutor,
            analyticsRequestFactory: AnalyticsRequestFactory,
            logger: Logger,
            enableLogging: Boolean,
            workContext: CoroutineContext,
            uiContext: CoroutineContext,
            threeDs2Service: StripeThreeDs2Service,
            messageVersionRegistry: MessageVersionRegistry,
            challengeProgressActivityStarter: StripePaymentController.ChallengeProgressActivityStarter,
            stripe3ds2Config: PaymentAuthConfig.Stripe3ds2Config,
            stripe3ds2ChallengeLauncher: ActivityResultLauncher<PaymentFlowResult.Unvalidated>?
        ): IntentAuthenticatorRegistry {
            if (instance == null) {
                instance = DaggerAuthenticationComponent.builder().authenticationModule(
                    AuthenticationModule(
                        stripeRepository,
                        paymentRelayStarterFactory,
                        paymentBrowserAuthStarterFactory,
                        analyticsRequestExecutor,
                        analyticsRequestFactory,
                        logger,
                        enableLogging,
                        workContext,
                        uiContext,
                        threeDs2Service,
                        messageVersionRegistry,
                        challengeProgressActivityStarter,
                        stripe3ds2Config,
                        stripe3ds2ChallengeLauncher
                    )
                ).build().registry
            }
            return instance!!
        }
    }
}
