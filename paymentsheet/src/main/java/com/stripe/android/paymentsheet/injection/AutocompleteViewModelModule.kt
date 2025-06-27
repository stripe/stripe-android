package com.stripe.android.paymentsheet.injection

import android.app.Application
import android.content.Context
import com.stripe.android.BuildConfig
import com.stripe.android.PaymentConfiguration
import com.stripe.android.core.injection.ENABLE_LOGGING
import com.stripe.android.core.injection.PUBLISHABLE_KEY
import com.stripe.android.core.networking.AnalyticsRequestFactory
import com.stripe.android.networking.PaymentAnalyticsRequestFactory
import com.stripe.android.payments.core.analytics.ErrorReporter
import com.stripe.android.payments.core.injection.PRODUCT_USAGE
import com.stripe.android.paymentsheet.addresselement.AutocompleteContract
import com.stripe.android.paymentsheet.addresselement.AutocompleteViewModel
import com.stripe.android.paymentsheet.addresselement.analytics.AddressLauncherEventReporter
import com.stripe.android.paymentsheet.addresselement.analytics.DefaultAddressLauncherEventReporter
import com.stripe.android.paymentsheet.analytics.EventReporter
import com.stripe.android.ui.core.elements.autocomplete.PlacesClientProxy
import dagger.Binds
import dagger.Module
import dagger.Provides
import javax.inject.Named
import javax.inject.Singleton

@Module
internal interface AutocompleteViewModelModule {
    @Binds
    fun bindsAnalyticsRequestFactory(
        paymentAnalyticsRequestFactory: PaymentAnalyticsRequestFactory
    ): AnalyticsRequestFactory

    companion object {
        @Provides
        @Singleton
        fun providesContext(application: Application): Context = application.applicationContext

        @Provides
        @Singleton
        fun provideEventReporterMode(): EventReporter.Mode = EventReporter.Mode.Custom

        @Provides
        @Named(PRODUCT_USAGE)
        @Singleton
        fun providesProductUsage() = setOf("PaymentElement.Autocomplete")

        @Provides
        @Singleton
        fun providesAutocompleteViewModelArgs(
            args: AutocompleteContract.Args
        ): AutocompleteViewModel.Args = AutocompleteViewModel.Args(args.country)

        @Provides
        @Named(ENABLE_LOGGING)
        fun providesEnableLogging(): Boolean = BuildConfig.DEBUG

        @Provides
        @Named(PUBLISHABLE_KEY)
        @Singleton
        fun providesPublishableKey(
            context: Context
        ): () -> String = { PaymentConfiguration.getInstance(context).publishableKey }

        @Provides
        @Singleton
        internal fun provideGooglePlacesClient(
            context: Context,
            args: AutocompleteContract.Args
        ): PlacesClientProxy = PlacesClientProxy.create(
            context = context,
            googlePlacesApiKey = args.googlePlacesApiKey,
            errorReporter = ErrorReporter.createFallbackInstance(context),
        )

        @Provides
        @Singleton
        fun provideEventReporter(
            defaultAddressLauncherEventReporter: DefaultAddressLauncherEventReporter
        ): AddressLauncherEventReporter = defaultAddressLauncherEventReporter
    }
}
