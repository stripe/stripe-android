package com.stripe.android.payments.core.injection

import android.content.Context
import androidx.annotation.RestrictTo
import com.stripe.android.ApiConfiguration
import com.stripe.android.PaymentConfiguration
import com.stripe.android.core.networking.ApiRequest
import dagger.Module
import dagger.Provides
import javax.inject.Provider

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@Module
class PaymentConfigurationModule {
    @Provides
    fun providePaymentConfiguration(context: Context): PaymentConfiguration {
        return PaymentConfiguration.getInstance(context)
    }

    @Provides
    fun provideApiConfigurationState(
        paymentConfiguration: Provider<PaymentConfiguration>
    ): ApiConfiguration.State {
        val config = paymentConfiguration.get()
        return ApiConfiguration.State(
            publishableKey = config.publishableKey,
            stripeAccountId = config.stripeAccountId,
        )
    }

    @Provides
    fun provideApiRequestOptions(
        paymentConfiguration: Provider<PaymentConfiguration>
    ): ApiRequest.Options = ApiRequest.Options(
        apiKey = paymentConfiguration.get().publishableKey,
        stripeAccount = paymentConfiguration.get().stripeAccountId,
    )
}
