package com.stripe.android.payments.core.injection

import com.stripe.android.model.StripeIntent
import com.stripe.android.model.StripeIntent.NextActionData
import com.stripe.android.payments.core.authentication.PaymentAuthenticator
import com.stripe.android.payments.core.authentication.UnsupportedAuthenticator
import dagger.Module
import dagger.Provides
import dagger.multibindings.IntoMap

/**
 * Provides [PaymentAuthenticator] for [NextActionData.WeChatPayRedirect] through reflection,
 * requires "com.stripe:stripe-wechatpay:[StripeSdkVersion.VERSION_NAME]" dependency.
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
    ): PaymentAuthenticator<StripeIntent> {
        return runCatching {
            @Suppress("UNCHECKED_CAST")
            Class.forName(
                "com.stripe.android.payments.wechatpay.WeChatPayAuthenticator"
            ).getConstructor()
                .newInstance() as PaymentAuthenticator<StripeIntent>
        }.getOrDefault(unsupportedAuthenticator)
    }
}
