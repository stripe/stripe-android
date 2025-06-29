package com.stripe.android.shoppay.di

import com.stripe.android.shoppay.ShopPayViewModel
import com.stripe.android.shoppay.bridge.ShopPayBridgeHandler
import dagger.Module
import dagger.Provides

@Module
internal object ShopPayViewModelModule {
    @Provides
    fun provideShopPayViewModel(
        bridgeHandler: ShopPayBridgeHandler
    ): ShopPayViewModel {
        return ShopPayViewModel(bridgeHandler)
    }
}
