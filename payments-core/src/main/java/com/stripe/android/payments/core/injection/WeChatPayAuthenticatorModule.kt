package com.stripe.android.payments.core.injection

import com.stripe.android.model.StripeIntent
import com.stripe.android.model.StripeIntent.NextActionData
import com.stripe.android.payments.core.nextactionhandler.PaymentNextActionHandler
import com.stripe.android.payments.core.nextactionhandler.UnsupportedNextActionHandler
import dagger.Module
import dagger.Provides
import dagger.multibindings.IntoMap

/**
 * Provides [PaymentNextActionHandler] for [NextActionData.WeChatPayRedirect] through reflection,
 * requires "com.stripe:stripe-wechatpay:[StripeSdkVersion.VERSION_NAME]" dependency.
 * Will register a [UnsupportedNextActionHandler] if the dependency is not added.
 */
@Module
internal class WeChatPayAuthenticatorModule {
    @IntentNextActionHandlerMap
    @Provides
    @IntoMap
    @IntentNextActionHandlerKey(NextActionData.WeChatPayRedirect::class)
    internal fun provideWeChatAuthenticator(
        unsupportedAuthenticator: UnsupportedNextActionHandler
    ): PaymentNextActionHandler<StripeIntent> {
        return runCatching {
            @Suppress("UNCHECKED_CAST")
            Class.forName(
                "com.stripe.android.payments.wechatpay.WeChatPayAuthenticator"
            ).getConstructor()
                .newInstance() as PaymentNextActionHandler<StripeIntent>
        }.getOrDefault(unsupportedAuthenticator)
    }
}
