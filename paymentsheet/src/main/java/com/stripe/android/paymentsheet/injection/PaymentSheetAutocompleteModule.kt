package com.stripe.android.paymentsheet.injection

import com.stripe.android.core.injection.PUBLISHABLE_KEY
import com.stripe.android.core.networking.ApiRequest
import com.stripe.android.core.networking.StripeNetworkClient
import com.stripe.android.paymentsheet.addresselement.DefaultStripeAutocompleteRepository
import com.stripe.android.paymentsheet.addresselement.StripeAutocompleteRepository
import dagger.Module
import dagger.Provides
import javax.inject.Named
import javax.inject.Singleton

@Module
internal class PaymentSheetAutocompleteModule {
    @Provides
    @Singleton
    fun provideApiRequestFactory(): ApiRequest.Factory = ApiRequest.Factory()

    @Provides
    @Singleton
    fun provideStripeAutocompleteRepository(
        stripeNetworkClient: StripeNetworkClient,
        apiRequestFactory: ApiRequest.Factory,
        @Named(PUBLISHABLE_KEY) publishableKeyProvider: () -> String,
    ): StripeAutocompleteRepository = DefaultStripeAutocompleteRepository(
        stripeNetworkClient = stripeNetworkClient,
        apiRequestFactory = apiRequestFactory,
        publishableKeyProvider = publishableKeyProvider,
    )
}
