package com.stripe.android.payments.core.injection

import android.content.Context
import com.stripe.android.PaymentAuthConfig
import com.stripe.android.PaymentRelayStarter
import com.stripe.android.model.StripeIntent.NextActionData
import com.stripe.android.networking.AnalyticsRequestExecutor
import com.stripe.android.networking.AnalyticsRequestFactory
import com.stripe.android.networking.StripeRepository
import com.stripe.android.payments.Stripe3ds2CompletionStarter
import com.stripe.android.payments.core.authentication.IntentAuthenticator
import com.stripe.android.payments.core.authentication.Stripe3DS2Authenticator
import com.stripe.android.payments.core.authentication.WebIntentAuthenticator
import com.stripe.android.stripe3ds2.service.StripeThreeDs2ServiceImpl
import com.stripe.android.stripe3ds2.transaction.MessageVersionRegistry
import com.stripe.android.view.AuthActivityStarterHost
import dagger.Module
import dagger.Provides
import dagger.multibindings.IntoMap
import javax.inject.Named
import kotlin.coroutines.CoroutineContext

/**
 * Provides [IntentAuthenticator] for [NextActionData.SdkData.Use3DS2].
 * Requires 3ds2 SDK.
 */
@Module
internal class Stripe3DSAuthenticatorModule {
    @IntentAuthenticatorMap
    @Provides
    @IntoMap
    @IntentAuthenticatorKey(NextActionData.SdkData.Use3DS2::class)
    internal fun provideStripe3DSAuthenticator(
        context: Context,
        @Named(AuthenticationComponent.ENABLE_LOGGING) enableLogging: Boolean,
        stripeRepository: StripeRepository,
        webIntentAuthenticator: WebIntentAuthenticator,
        paymentRelayStarterFactory: (AuthActivityStarterHost) -> PaymentRelayStarter,
        analyticsRequestExecutor: AnalyticsRequestExecutor,
        analyticsRequestFactory: AnalyticsRequestFactory,
        stripe3ds2CompletionStarterFactory: (AuthActivityStarterHost, Int) -> Stripe3ds2CompletionStarter,
        @IOContext workContext: CoroutineContext,
        @UIContext uiContext: CoroutineContext
    ): IntentAuthenticator {
        return Stripe3DS2Authenticator(
            PaymentAuthConfig.get(),
            stripeRepository,
            webIntentAuthenticator,
            paymentRelayStarterFactory,
            analyticsRequestExecutor,
            analyticsRequestFactory,
            stripe3ds2CompletionStarterFactory,
            workContext,
            uiContext,
            StripeThreeDs2ServiceImpl(context, enableLogging),
            MessageVersionRegistry(),
            Stripe3DS2Authenticator.DefaultChallengeProgressActivityStarter()
        )
    }
}
