package com.stripe.android.paymentsheet.injection

import com.stripe.android.Stripe
import com.stripe.android.core.injection.PUBLISHABLE_KEY
import com.stripe.android.core.injection.STRIPE_ACCOUNT_ID
import com.stripe.android.core.networking.ApiRequest
import com.stripe.android.core.networking.StripeNetworkClient
import com.stripe.android.core.version.StripeSdkVersion
import com.stripe.android.paymentsheet.addresselement.DefaultStripeAutocompleteApiService
import com.stripe.android.paymentsheet.addresselement.StripeAutocompleteApiService
import dagger.Module
import dagger.Provides
import javax.inject.Named
import javax.inject.Singleton

@Module
internal object PaymentSheetAutocompleteModule {
    @Provides
    @Singleton
    fun provideStripeAutocompleteApiService(
        stripeNetworkClient: StripeNetworkClient,
        @Named(PUBLISHABLE_KEY) publishableKeyProvider: () -> String,
        @Named(STRIPE_ACCOUNT_ID) stripeAccountIdProvider: () -> String?
    ): StripeAutocompleteApiService {
        return DefaultStripeAutocompleteApiService(
            stripeNetworkClient = stripeNetworkClient,
            apiRequestFactory = ApiRequest.Factory(
                appInfo = Stripe.appInfo,
                apiVersion = Stripe.API_VERSION,
                sdkVersion = StripeSdkVersion.VERSION,
            ),
            publishableKeyProvider = publishableKeyProvider,
            stripeAccountIdProvider = stripeAccountIdProvider,
        )
    }
}
