package com.stripe.android.payments.core.injection

import com.stripe.android.model.StripeIntent
import com.stripe.android.model.StripeIntent.NextActionData
import com.stripe.android.payments.core.authentication.OxxoAuthenticator
import com.stripe.android.payments.core.authentication.PaymentAuthenticator
import com.stripe.android.payments.core.authentication.WebIntentAuthenticator
import dagger.Binds
import dagger.Module
import dagger.multibindings.IntoMap

/**
 * Provides mappings between [NextActionData] and [PaymentAuthenticator] provided by payment SDK.
 */
@Module
internal interface AuthenticationModule {
    @IntentAuthenticatorMap
    @Binds
    @IntoMap
    @IntentAuthenticatorKey(NextActionData.SdkData.Use3DS1::class)
    fun binds3DS1Authenticator(webIntentAuthenticator: WebIntentAuthenticator): PaymentAuthenticator<StripeIntent>

    @IntentAuthenticatorMap
    @Binds
    @IntoMap
    @IntentAuthenticatorKey(NextActionData.RedirectToUrl::class)
    fun bindsRedirectAuthenticator(webIntentAuthenticator: WebIntentAuthenticator): PaymentAuthenticator<StripeIntent>

    @IntentAuthenticatorMap
    @Binds
    @IntoMap
    @IntentAuthenticatorKey(NextActionData.AlipayRedirect::class)
    fun bindsAlipayRedirectAuthenticator(webIntentAuthenticator: WebIntentAuthenticator): PaymentAuthenticator<StripeIntent>

    @IntentAuthenticatorMap
    @Binds
    @IntoMap
    @IntentAuthenticatorKey(NextActionData.DisplayOxxoDetails::class)
    fun bindsOxxoAuthenticator(oxxoAuthenticator: OxxoAuthenticator): PaymentAuthenticator<StripeIntent>
}
