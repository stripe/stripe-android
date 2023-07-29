package com.stripe.android.paymentsheet.injection

import android.content.Context
import com.stripe.android.core.injection.PUBLISHABLE_KEY
import com.stripe.android.core.networking.AnalyticsRequestFactory
import com.stripe.android.core.networking.NetworkTypeDetector
import com.stripe.android.core.utils.ContextUtils.packageInfo
import com.stripe.android.paymentsheet.addresselement.AddressElementActivityContract
import com.stripe.android.paymentsheet.addresselement.analytics.AddressLauncherEventReporter
import com.stripe.android.paymentsheet.addresselement.analytics.DefaultAddressLauncherEventReporter
import com.stripe.android.paymentsheet.analytics.EventReporter
import com.stripe.android.ui.core.elements.autocomplete.PlacesClientProxy
import com.stripe.android.ui.core.injection.FormControllerSubcomponent
import dagger.Module
import dagger.Provides
import javax.inject.Named
import javax.inject.Singleton

@Module(
    subcomponents = [
        AddressElementViewModelSubcomponent::class,
        InputAddressViewModelSubcomponent::class,
        AutocompleteViewModelSubcomponent::class,
        FormControllerSubcomponent::class
    ]
)
internal class AddressElementViewModelModule {
    @Provides
    @Singleton
    fun provideEventReporterMode(): EventReporter.Mode = EventReporter.Mode.Custom

    @Provides
    @Named(PUBLISHABLE_KEY)
    @Singleton
    fun providesPublishableKey(
        args: AddressElementActivityContract.Args
    ): String = args.publishableKey

    @Provides
    @Singleton
    internal fun provideAnalyticsRequestFactory(
        context: Context,
        @Named(PUBLISHABLE_KEY) publishableKey: String
    ): AnalyticsRequestFactory = AnalyticsRequestFactory(
        packageManager = context.packageManager,
        packageName = context.packageName.orEmpty(),
        packageInfo = context.packageInfo,
        publishableKeyProvider = { publishableKey },
        networkTypeProvider = NetworkTypeDetector(context)::invoke,
    )

    @Provides
    @Singleton
    internal fun provideGooglePlacesClient(
        context: Context,
        args: AddressElementActivityContract.Args
    ): PlacesClientProxy? = args.config?.googlePlacesApiKey?.let {
        PlacesClientProxy.create(
            context,
            it
        )
    }

    @Provides
    @Singleton
    fun provideEventReporter(
        defaultAddressLauncherEventReporter: DefaultAddressLauncherEventReporter
    ): AddressLauncherEventReporter = defaultAddressLauncherEventReporter
}
