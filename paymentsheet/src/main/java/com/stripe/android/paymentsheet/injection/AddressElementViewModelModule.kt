package com.stripe.android.paymentsheet.injection

import android.content.Context
import com.stripe.android.checkout.CheckoutSessionTaxRegionUpdater
import com.stripe.android.checkout.toCheckoutAddress
import com.stripe.android.core.networking.ApiRequest
import com.stripe.android.core.networking.StripeNetworkClient
import com.stripe.android.paymentelement.CheckoutSessionPreview
import com.stripe.android.payments.core.analytics.ErrorReporter
import com.stripe.android.payments.core.injection.PRODUCT_USAGE
import com.stripe.android.paymentsheet.addresselement.AddressDetails
import com.stripe.android.paymentsheet.addresselement.AddressElementActivityContract
import com.stripe.android.paymentsheet.addresselement.AddressElementNavigator
import com.stripe.android.paymentsheet.addresselement.AddressElementPrimaryButtonAction
import com.stripe.android.paymentsheet.addresselement.DefaultStripeAutocompleteRepository
import com.stripe.android.paymentsheet.addresselement.NavHostAddressElementNavigator
import com.stripe.android.paymentsheet.addresselement.StripeAutocompleteRepository
import com.stripe.android.paymentsheet.addresselement.StripeHostedPlacesClientProxy
import com.stripe.android.paymentsheet.addresselement.analytics.AddressLauncherEventReporter
import com.stripe.android.paymentsheet.repositories.CheckoutSessionResponse
import com.stripe.android.paymentsheet.repositories.CheckoutSessionResponse.TaxAddressSource
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
    @Named(PRODUCT_USAGE)
    fun providesProductUsage() = setOf("PaymentSheet.AddressController")

    @Provides
    @OptIn(CheckoutSessionPreview::class)
    internal fun providePrimaryButtonAction(
        args: AddressElementActivityContract.Args,
        taxRegionUpdater: CheckoutSessionTaxRegionUpdater,
    ): AddressElementPrimaryButtonAction = when (args) {
        is AddressElementActivityContract.Args.Standalone -> {
            StandalonePrimaryButtonAction
        }
        is AddressElementActivityContract.Args.CheckoutShipping -> {
            CheckoutShippingPrimaryButtonAction(
                checkoutSessionResponse = args.checkoutSessionResponse,
                taxRegionUpdater = taxRegionUpdater,
            )
        }
    }

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
                errorReporter = ErrorReporter.createFallbackInstance(context),
            )
        }
    }

    @Module
    interface Bindings {
        @Binds
        fun bindsAddressElementNavigator(navigator: NavHostAddressElementNavigator): AddressElementNavigator
    }
}

private object StandalonePrimaryButtonAction : AddressElementPrimaryButtonAction {
    override suspend fun invoke(
        addressDetails: AddressDetails,
    ): Result<AddressElementActivityContract.Result> {
        return Result.success(
            AddressElementActivityContract.Result.StandaloneSucceeded(addressDetails)
        )
    }
}

@OptIn(CheckoutSessionPreview::class)
private class CheckoutShippingPrimaryButtonAction(
    private val checkoutSessionResponse: CheckoutSessionResponse,
    private val taxRegionUpdater: CheckoutSessionTaxRegionUpdater,
) : AddressElementPrimaryButtonAction {
    override suspend fun invoke(
        addressDetails: AddressDetails,
    ): Result<AddressElementActivityContract.Result> {
        return taxRegionUpdater.updateServerStateIfNeeded(
            checkoutSessionResponse = checkoutSessionResponse,
            addressSource = TaxAddressSource.SHIPPING,
            address = requireNotNull(addressDetails.address?.toCheckoutAddress()),
        ).map { response ->
            AddressElementActivityContract.Result.CheckoutShippingSucceeded(
                address = addressDetails,
                checkoutSessionResponse = response,
            )
        }
    }
}
