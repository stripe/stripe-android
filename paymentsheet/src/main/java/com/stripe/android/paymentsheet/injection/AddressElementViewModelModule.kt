package com.stripe.android.paymentsheet.injection

import android.content.Context
import com.stripe.android.core.injection.PUBLISHABLE_KEY
import com.stripe.android.core.networking.ApiRequest
import com.stripe.android.core.networking.StripeNetworkClient
import com.stripe.android.payments.core.analytics.ErrorReporter
import com.stripe.android.payments.core.injection.PRODUCT_USAGE
import com.stripe.android.paymentsheet.addresselement.AddressElementActivityContract
import com.stripe.android.paymentsheet.addresselement.AddressElementNavigator
import com.stripe.android.paymentsheet.addresselement.DefaultStripeAutocompleteRepository
import com.stripe.android.paymentsheet.addresselement.NavHostAddressElementNavigator
import com.stripe.android.paymentsheet.addresselement.StripeAutocompleteRepository
import com.stripe.android.paymentsheet.addresselement.StripeHostedPlacesClientProxy
import com.stripe.android.paymentsheet.addresselement.analytics.AddressLauncherEventReporter
import com.stripe.android.paymentsheet.addresselement.analytics.DefaultAddressLauncherEventReporter
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
    fun provideApiRequestFactory(): ApiRequest.Factory = ApiRequest.Factory()

    @Provides
    @Singleton
    fun provideStripeAutocompleteRepository(
        stripeNetworkClient: StripeNetworkClient,
        apiRequestFactory: ApiRequest.Factory,
        args: AddressElementActivityContract.Args,
    ): StripeAutocompleteRepository = DefaultStripeAutocompleteRepository(
        stripeNetworkClient = stripeNetworkClient,
        apiRequestFactory = apiRequestFactory,
        publishableKeyProvider = { args.publishableKey },
    )

    @Provides
    @Singleton
    @Named(INLINE_PLACES_CLIENT)
    internal fun provideInlinePlacesClient(
        args: AddressElementActivityContract.Args,
        stripeAutocompleteRepository: StripeAutocompleteRepository,
        googlePlacesClient: PlacesClientProxy?,
    ): PlacesClientProxy? {
        val config = args.config ?: return null
        return if (config.useStripeHostedAutocomplete) {
            StripeHostedPlacesClientProxy(repository = stripeAutocompleteRepository)
        } else {
            googlePlacesClient
        }
    }

    @Provides
    @Singleton
    internal fun provideGooglePlacesClient(
        context: Context,
        args: AddressElementActivityContract.Args,
    ): PlacesClientProxy? {
        val config = args.config ?: return null
        return config.googlePlacesApiKey?.let {
            PlacesClientProxy.create(
                context,
                it,
                errorReporter = ErrorReporter.createFallbackInstance(context),
            )
        }
    }

    @Provides
    @Singleton
    fun provideEventReporter(
        defaultAddressLauncherEventReporter: DefaultAddressLauncherEventReporter
    ): AddressLauncherEventReporter = defaultAddressLauncherEventReporter

    @Module
    interface Bindings {
        @Binds
        fun bindsAddressElementNavigator(navigator: NavHostAddressElementNavigator): AddressElementNavigator
    }
}
