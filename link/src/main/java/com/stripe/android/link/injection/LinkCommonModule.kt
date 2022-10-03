package com.stripe.android.link.injection

import com.stripe.android.link.LinkPaymentLauncher
import com.stripe.android.link.analytics.DefaultLinkEventsReporter
import com.stripe.android.link.analytics.LinkEventsReporter
import com.stripe.android.link.repositories.LinkApiRepository
import com.stripe.android.link.repositories.LinkRepository
import com.stripe.android.model.StripeIntent
import dagger.Binds
import dagger.Module
import dagger.Provides
import javax.inject.Named
import javax.inject.Singleton

@Module
internal interface LinkCommonModule {
    @Binds
    @Singleton
    fun bindLinkRepository(linkApiRepository: LinkApiRepository): LinkRepository

    @Binds
    @Singleton
    fun bindLinkEventsReporter(linkEventsReporter: DefaultLinkEventsReporter): LinkEventsReporter

    companion object {
        @Provides
        @Singleton
        @Named(MERCHANT_NAME)
        fun merchantName(configuration: LinkPaymentLauncher.Configuration): String =
            configuration.merchantName

        @Provides
        @Singleton
        @Named(CUSTOMER_EMAIL)
        fun customerEmail(configuration: LinkPaymentLauncher.Configuration): String? =
            configuration.customerEmail

        @Provides
        @Singleton
        @Named(CUSTOMER_PHONE)
        fun customerPhone(configuration: LinkPaymentLauncher.Configuration): String? =
            configuration.customerPhone

        @Provides
        @Singleton
        @Named(CUSTOMER_NAME)
        fun customerName(configuration: LinkPaymentLauncher.Configuration): String? =
            configuration.customerName

        @Provides
        @Singleton
        @Named(LINK_INTENT)
        fun stripeIntent(configuration: LinkPaymentLauncher.Configuration): StripeIntent =
            configuration.stripeIntent
    }
}
