package com.stripe.android.shoppay.di

import com.stripe.android.SharedPaymentTokenSessionPreview
import com.stripe.android.core.networking.ApiRequest
import com.stripe.android.networking.StripeRepository
import com.stripe.android.paymentelement.PreparePaymentMethodHandler
import com.stripe.android.shoppay.ShopPayViewModel
import com.stripe.android.shoppay.bridge.ShopPayBridgeHandler
import dagger.Module
import dagger.Provides

@Module
internal object ShopPayViewModelModule {
    @OptIn(SharedPaymentTokenSessionPreview::class)
    @Provides
    fun provideShopPayViewModel(
        bridgeHandler: ShopPayBridgeHandler,
        stripeApiRepository: StripeRepository,
        requestOptions: ApiRequest.Options,
        preparePaymentMethodHandler: PreparePaymentMethodHandler
    ): ShopPayViewModel {
        return ShopPayViewModel(
            bridgeHandler = bridgeHandler,
            stripeApiRepository = stripeApiRepository,
            requestOptions = requestOptions,
            paymentMethodHandler = preparePaymentMethodHandler
        )
    }
}
