package com.stripe.android.paymentsheet.injection

import android.content.Context
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
    @Named(INLINE_PLACES_CLIENT)
    internal fun provideInlinePlacesClient(
        args: AddressElementActivityContract.Args,
        stripeAutocompleteRepository: StripeAutocompleteRepository,
        googlePlacesClient: PlacesClientProxy?,
        addressLauncherEventReporter: AddressLauncherEventReporter,
    ): PlacesClientProxy? {
        val config = args.config ?: return null
        return if (config.useStripeHostedAutocomplete) {
            StripeHostedPlacesClientProxy(
                repository = stripeAutocompleteRepository,
                eventReporter = addressLauncherEventReporter,
            )
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
                errorReporter = ErrorReporter.createFallbackInstance(context, publishableKeyProvider = { args.publishableKey }),
            )
        }
    }

    @Module
    interface Bindings {
        @Binds
        fun bindsAddressElementNavigator(navigator: NavHostAddressElementNavigator): AddressElementNavigator
    }
}
