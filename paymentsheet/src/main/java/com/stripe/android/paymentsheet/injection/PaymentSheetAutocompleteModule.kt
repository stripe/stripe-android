package com.stripe.android.paymentsheet.injection

import com.stripe.android.core.ApiConfiguration
import com.stripe.android.core.networking.ApiRequest
import com.stripe.android.core.networking.RequestCredentials
import com.stripe.android.core.networking.StripeNetworkClient
import com.stripe.android.payments.core.injection.RequestCredentialsModule
import com.stripe.android.paymentsheet.addresselement.DefaultStripeAutocompleteRepository
import com.stripe.android.paymentsheet.addresselement.StripeAutocompleteRepository
import dagger.Module
import dagger.Provides
import javax.inject.Provider
import javax.inject.Singleton

@Module(includes = [RequestCredentialsModule::class])
internal class PaymentSheetAutocompleteModule {
    @Provides
    @Singleton
    fun provideStripeAutocompleteRepository(
        apiConfigurationProvider: Provider<ApiConfiguration.State>,
        stripeNetworkClient: StripeNetworkClient,
        requestOptionsProvider: Provider<RequestCredentials>,
    ): StripeAutocompleteRepository = DefaultStripeAutocompleteRepository(
        apiConfigurationProvider = apiConfigurationProvider,
        stripeNetworkClient = stripeNetworkClient,
        apiRequestFactory = ApiRequest.Factory(),
        requestOptionsProvider = requestOptionsProvider,
    )
}
