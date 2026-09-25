package com.stripe.android.paymentsheet.injection

import android.content.Context
import com.stripe.android.checkout.CheckoutSessionTaxRegionUpdater
import com.stripe.android.checkout.toCheckoutAddress
import com.stripe.android.core.networking.AnalyticsRequestExecutor
import com.stripe.android.core.networking.AnalyticsRequestFactory
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
import com.stripe.android.paymentsheet.addresselement.analytics.DefaultShippingAddressElementEventReporter
import com.stripe.android.paymentsheet.addresselement.analytics.NoOpShippingAddressElementEventReporter
import com.stripe.android.paymentsheet.addresselement.analytics.ShippingAddressElementEventReporter
import com.stripe.android.paymentsheet.repositories.CheckoutSessionResponse
import com.stripe.android.paymentsheet.repositories.CheckoutSessionResponse.TaxAddressSource
import com.stripe.android.ui.core.elements.autocomplete.PlacesClientProxy
import dagger.Binds
import dagger.Module
import dagger.Provides
import javax.inject.Named
import javax.inject.Provider
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
        requestOptionsProvider: Provider<ApiRequest.Options>,
    ): StripeAutocompleteRepository = DefaultStripeAutocompleteRepository(
        stripeNetworkClient = stripeNetworkClient,
        apiRequestFactory = ApiRequest.Factory(),
        requestOptionsProvider = requestOptionsProvider,
    )

    @Provides
    @Singleton
    internal fun provideShippingAddressElementEventReporter(
        args: AddressElementActivityContract.Args,
        analyticsRequestExecutor: AnalyticsRequestExecutor,
        analyticsRequestFactory: AnalyticsRequestFactory,
    ): ShippingAddressElementEventReporter {
        return when (args) {
            is AddressElementActivityContract.Args.Standalone -> {
                NoOpShippingAddressElementEventReporter
            }
            is AddressElementActivityContract.Args.CheckoutShipping -> {
                DefaultShippingAddressElementEventReporter(
                    analyticsRequestExecutor = analyticsRequestExecutor,
                    analyticsRequestFactory = analyticsRequestFactory,
                    checkoutSessionId = args.checkoutSessionResponse.id,
                )
            }
        }
    }

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
                errorReporter = ErrorReporter.createFallbackInstance(
                    context = context,
                    apiConfigurationProvider = { args.apiConfiguration },
                ),
            )
        }
    }

    @Provides
    @Singleton
    fun provideApiConfiguration(
        args: AddressElementActivityContract.Args
    ) = args.apiConfiguration

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
        val address = addressDetails.address?.toCheckoutAddress()
            ?: return Result.failure(IllegalArgumentException("Country is required."))

        return taxRegionUpdater.updateServerStateIfNeeded(
            checkoutSessionResponse = checkoutSessionResponse,
            addressSource = TaxAddressSource.SHIPPING,
            address = address,
        ).map { response ->
            AddressElementActivityContract.Result.CheckoutShippingSucceeded(
                address = addressDetails,
                checkoutSessionResponse = response,
            )
        }
    }
}
