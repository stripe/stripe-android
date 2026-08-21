package com.stripe.android.paymentsheet.injection

import com.stripe.android.core.injection.PUBLISHABLE_KEY
import com.stripe.android.core.networking.ApiRequest
import com.stripe.android.core.networking.StripeNetworkClient
import com.stripe.android.payments.core.injection.PRODUCT_USAGE
import com.stripe.android.paymentsheet.addresselement.AddressElementActivityContract
import com.stripe.android.paymentsheet.addresselement.AddressElementNavigator
import com.stripe.android.paymentsheet.addresselement.DefaultStripeAutocompleteRepository
import com.stripe.android.paymentsheet.addresselement.NavHostAddressElementNavigator
import com.stripe.android.paymentsheet.addresselement.StripeAutocompleteRepository
import com.stripe.android.paymentsheet.addresselement.StripeHostedPlacesClientProxy
import com.stripe.android.paymentsheet.addresselement.analytics.AddressLauncherEventReporter
import com.stripe.android.paymentsheet.analytics.EventReporter
import com.stripe.android.ui.core.elements.autocomplete.PlacesClientProxy
import dagger.Binds
import dagger.Module
import dagger.Provides
import javax.inject.Named
import javax.inject.Singleton

@Module(
    includes = [AddressElementViewModelModule.Bindings::class],
    subcomponents = [
        AddressElementViewModelSubcomponent::class,
        InputAddressViewModelSubcomponent::class,
        AutocompleteViewModelSubcomponent::class,
    ]
)
internal class AddressElementViewModelModule {
    companion object {
        internal const val INLINE_PLACES_CLIENT = "inline_places_client"
    }

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
    fun provideStripeAutocompleteRepository(
        stripeNetworkClient: StripeNetworkClient,
        args: AddressElementActivityContract.Args,
    ): StripeAutocompleteRepository = DefaultStripeAutocompleteRepository(
        stripeNetworkClient = stripeNetworkClient,
        apiRequestFactory = ApiRequest.Factory(),
        publishableKeyProvider = { args.publishableKey },
    )

    @Provides
    @Singleton
    internal fun providePlacesClient(
        stripeAutocompleteRepository: StripeAutocompleteRepository,
        addressLauncherEventReporter: AddressLauncherEventReporter,
    ): PlacesClientProxy? = PlacesClientProxy.override
        ?: StripeHostedPlacesClientProxy(
            repository = stripeAutocompleteRepository,
            eventReporter = addressLauncherEventReporter,
        )

    @Provides
    @Singleton
    @Named(INLINE_PLACES_CLIENT)
    internal fun provideInlinePlacesClient(
        args: AddressElementActivityContract.Args,
        stripeAutocompleteRepository: StripeAutocompleteRepository,
        addressLauncherEventReporter: AddressLauncherEventReporter,
    ): PlacesClientProxy? {
        if (args.config == null) return null
        return PlacesClientProxy.override ?: StripeHostedPlacesClientProxy(
            repository = stripeAutocompleteRepository,
            eventReporter = addressLauncherEventReporter,
        )
    }

    @Module
    interface Bindings {
        @Binds
        fun bindsAddressElementNavigator(navigator: NavHostAddressElementNavigator): AddressElementNavigator
    }
}
