package com.stripe.android.payments.core.injection

import androidx.annotation.RestrictTo
import com.stripe.android.ApiConfiguration
import com.stripe.android.PaymentConfiguration
import dagger.Module
import dagger.Provides
import javax.inject.Provider

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@Module(includes = [PaymentConfigurationModule::class])
class ApiConfigurationModule {
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
}
