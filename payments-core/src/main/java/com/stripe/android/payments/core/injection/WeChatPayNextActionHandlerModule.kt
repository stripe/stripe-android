package com.stripe.android.payments.core.injection

import com.stripe.android.model.StripeIntent
import com.stripe.android.model.StripeIntent.NextActionData
import com.stripe.android.payments.core.authentication.PaymentNextActionHandler
import com.stripe.android.payments.core.authentication.UnsupportedNextActionHandler
import dagger.Module
import dagger.Provides
import dagger.multibindings.IntoMap

/**
 * Provides [PaymentNextActionHandler] for [NextActionData.WeChatPayRedirect] through reflection,
 * requires "com.stripe:stripe-wechatpay:[StripeSdkVersion.VERSION_NAME]" dependency.
 * Will register a [UnsupportedNextActionHandler] if the dependency is not added.
 */
@Module
internal class WeChatPayNextActionHandlerModule {
    @IntentAuthenticatorMap
    @Provides
    @IntoMap
    @IntentAuthenticatorKey(NextActionData.WeChatPayRedirect::class)
    internal fun provideWeChatNextActionHandler(
        unsupportedNextActionHandler: UnsupportedNextActionHandler
    ): PaymentNextActionHandler<StripeIntent> {
        return runCatching {
            @Suppress("UNCHECKED_CAST")
            Class.forName(
                "com.stripe.android.payments.wechatpay.WeChatPayNextActionHandler"
            ).getConstructor()
                .newInstance() as PaymentNextActionHandler<StripeIntent>
        }.getOrDefault(unsupportedNextActionHandler)
    }
}
