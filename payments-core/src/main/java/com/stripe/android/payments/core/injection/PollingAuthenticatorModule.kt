package com.stripe.android.payments.core.injection

import com.stripe.android.model.StripeIntent
import com.stripe.android.payments.core.authentication.PaymentAuthenticator
import com.stripe.android.payments.core.authentication.UnsupportedAuthenticator
import dagger.Module
import dagger.Provides
import dagger.multibindings.IntoMap

@Module
internal class PollingAuthenticatorModule {

    @IntentAuthenticatorMap
    @Provides
    @IntoMap
    @IntentAuthenticatorKey(StripeIntent.NextActionData.UpiAwaitNotification::class)
    internal fun provideUpiAuthenticator(
        unsupportedAuthenticator: UnsupportedAuthenticator
    ): PaymentAuthenticator<StripeIntent> {
        return runCatching {
            val name = "com.stripe.android.paymentsheet.paymentdatacollection.polling.PollingAuthenticator"
            val constructor = Class.forName(name).getConstructor()
            @Suppress("UNCHECKED_CAST")
            constructor.newInstance() as PaymentAuthenticator<StripeIntent>
        }.getOrDefault(unsupportedAuthenticator)
    }
}
