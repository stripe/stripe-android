package com.stripe.android.payments.core.injection

import com.stripe.android.model.StripeIntent.NextActionData
import com.stripe.android.payments.core.authentication.IntentAuthenticator
import com.stripe.android.payments.core.authentication.UnsupportedAuthenticator
import dagger.Module
import dagger.Provides
import dagger.multibindings.IntoMap

/**
 * Provides [IntentAuthenticator] for [NextActionData.WeChatPayRedirect] through reflection,
 * requires "com.stripe:stripe-wechatpay:X.Y.Z" dependency.
 * Will register a [UnsupportedAuthenticator] if the dependency is not added.
 */
@Module
internal class WeChatPayAuthenticatorModule {
    @IntentAuthenticatorMap
    @Provides
    @IntoMap
    @IntentAuthenticatorKey(NextActionData.WeChatPayRedirect::class)
    internal fun provideWeChatAuthenticator(
        unsupportedAuthenticator: UnsupportedAuthenticator
    ): IntentAuthenticator {
        return try {
            Class.forName(
                "com.stripe.android.payments.wechatpay.WeChatPayAuthenticator"
            ).getConstructor()
                .newInstance() as IntentAuthenticator
        } catch (e: ClassNotFoundException) {
            unsupportedAuthenticator
        }
    }
}
