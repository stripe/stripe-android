package com.stripe.android.payments.core.injection

import com.stripe.android.PaymentAuthConfig
import com.stripe.android.model.StripeIntent
import com.stripe.android.model.StripeIntent.NextActionData
import com.stripe.android.payments.core.authentication.PaymentAuthenticator
import com.stripe.android.payments.core.authentication.threeds2.Stripe3DS2Authenticator
import dagger.Module
import dagger.Provides
import dagger.multibindings.IntoMap
import javax.inject.Named

/**
 * Provides [PaymentAuthenticator] for [NextActionData.SdkData.Use3DS2].
 * Requires 3ds2 SDK.
 */
@Module
internal class Stripe3DSAuthenticatorModule {
    @IntentAuthenticatorMap
    @Provides
    @IntoMap
    @IntentAuthenticatorKey(NextActionData.SdkData.Use3DS2::class)
    internal fun provideStripe3DSAuthenticator(
        @Named(ENABLE_LOGGING) enableLogging: Boolean,
        threeDs1IntentReturnUrlMap: MutableMap<String, String>
    ): PaymentAuthenticator<StripeIntent> {
        return Stripe3DS2Authenticator(
            PaymentAuthConfig.get(),
            enableLogging,
            threeDs1IntentReturnUrlMap
        )
    }
}
