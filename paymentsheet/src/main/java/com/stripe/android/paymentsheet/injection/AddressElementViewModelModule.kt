package com.stripe.android.paymentsheet.injection

import android.content.Context
import com.stripe.android.core.injection.PUBLISHABLE_KEY
import com.stripe.android.payments.core.analytics.ErrorReporter
import com.stripe.android.payments.core.injection.PRODUCT_USAGE
import com.stripe.android.paymentsheet.addresselement.AddressElementActivityContract
import com.stripe.android.paymentsheet.addresselement.FormControllerSubcomponent
import com.stripe.android.paymentsheet.addresselement.analytics.AddressLauncherEventReporter
import com.stripe.android.paymentsheet.addresselement.analytics.DefaultAddressLauncherEventReporter
import com.stripe.android.paymentsheet.analytics.EventReporter
import com.stripe.android.ui.core.elements.autocomplete.PlacesClientProxy
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
    @Named(PRODUCT_USAGE)
    @Singleton
    fun providesProductUsage() = setOf("PaymentSheet.AddressController")

    @Provides
    @Named(PUBLISHABLE_KEY)
    @Singleton
    fun providesPublishableKey(
        args: AddressElementActivityContract.Args
    ): String = args.publishableKey

    @Provides
    @Singleton
    internal fun provideGooglePlacesClient(
        context: Context,
        args: AddressElementActivityContract.Args
    ): PlacesClientProxy? = args.config?.googlePlacesApiKey?.let {
        PlacesClientProxy.create(
            context,
            it,
            errorReporter = ErrorReporter.createFallbackInstance(context),
        )
    }

    @Provides
    @Singleton
    fun provideEventReporter(
        defaultAddressLauncherEventReporter: DefaultAddressLauncherEventReporter
    ): AddressLauncherEventReporter = defaultAddressLauncherEventReporter
}
