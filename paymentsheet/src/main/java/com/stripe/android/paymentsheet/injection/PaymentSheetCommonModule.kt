package com.stripe.android.paymentsheet.injection

import android.content.Context
import android.content.res.Resources
import androidx.core.os.LocaleListCompat
import com.stripe.android.PaymentConfiguration
import com.stripe.android.payments.core.injection.ENABLE_LOGGING
import com.stripe.android.payments.core.injection.PUBLISHABLE_KEY
import com.stripe.android.payments.core.injection.STRIPE_ACCOUNT_ID
import com.stripe.android.paymentsheet.BuildConfig
import com.stripe.android.paymentsheet.analytics.DefaultDeviceIdRepository
import com.stripe.android.paymentsheet.analytics.DefaultEventReporter
import com.stripe.android.paymentsheet.analytics.DeviceIdRepository
import com.stripe.android.paymentsheet.analytics.EventReporter
import com.stripe.android.paymentsheet.forms.resources.AsyncResourceRepository
import com.stripe.android.paymentsheet.forms.resources.ResourceRepository
import com.stripe.android.paymentsheet.repositories.CustomerApiRepository
import com.stripe.android.paymentsheet.repositories.CustomerRepository
import com.stripe.android.paymentsheet.repositories.StripeIntentRepository
import dagger.Binds
import dagger.Lazy
import dagger.Module
import dagger.Provides
import javax.inject.Named
import javax.inject.Provider
import javax.inject.Singleton

@Module
internal abstract class PaymentSheetCommonModule {

    @Binds
    abstract fun bindsEventReporter(eventReporter: DefaultEventReporter): EventReporter

    @Binds
    abstract fun bindsDeviceIdRepository(repository: DefaultDeviceIdRepository): DeviceIdRepository

    @Binds
    abstract fun bindsCustomerRepository(repository: CustomerApiRepository): CustomerRepository

    @Binds
    abstract fun provideResourceRepository(asyncResourceRepository: AsyncResourceRepository): ResourceRepository

    @Binds
    abstract fun bindsStripeIntentRepository(
        repository: StripeIntentRepository.Api
    ): StripeIntentRepository

    companion object {
        /**
         * Provides a non-singleton PaymentConfiguration.
         *
         * Should be fetched only when it's needed, to allow client to set the publishableKey and
         * stripeAccountId in PaymentConfiguration any time before configuring the FlowController
         * or presenting Payment Sheet.
         *
         * Should always be injected with [Lazy] or [Provider].
         */
        @Provides
        fun providePaymentConfiguration(appContext: Context): PaymentConfiguration {
            return PaymentConfiguration.getInstance(appContext)
        }

        @Provides
        @Singleton
        fun provideResources(context: Context): Resources {
            return context.resources
        }

        @Provides
        @Named(PUBLISHABLE_KEY)
        fun providePublishableKey(paymentConfiguration: Lazy<PaymentConfiguration>): () -> String =
            { paymentConfiguration.get().publishableKey }

        @Provides
        @Named(STRIPE_ACCOUNT_ID)
        fun provideStripeAccountId(paymentConfiguration: Lazy<PaymentConfiguration>):
            () -> String? = { paymentConfiguration.get().stripeAccountId }

        @Provides
        @Singleton
        @Named(ENABLE_LOGGING)
        fun provideEnabledLogging(): Boolean = BuildConfig.DEBUG

        @Provides
        @Singleton
        fun provideLocale() =
            LocaleListCompat.getAdjustedDefault().takeUnless { it.isEmpty }?.get(0)
    }
}
