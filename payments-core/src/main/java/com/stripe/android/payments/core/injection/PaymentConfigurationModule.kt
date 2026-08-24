package com.stripe.android.payments.core.injection

import android.content.Context
import androidx.annotation.RestrictTo
import com.stripe.android.PaymentConfiguration
import dagger.Module
import dagger.Provides

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@Module
class PaymentConfigurationModule {
    @Provides
    fun providePaymentConfiguration(context: Context): PaymentConfiguration {
        return PaymentConfiguration.getInstance(context)
    }
}
