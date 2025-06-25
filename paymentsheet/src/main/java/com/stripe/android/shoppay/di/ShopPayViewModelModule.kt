package com.stripe.android.shoppay.di

import com.stripe.android.shoppay.ShopPayViewModel
import com.stripe.android.shoppay.bridge.BridgeHandler
import dagger.Module
import dagger.Provides

@Module
internal object ShopPayViewModelModule {
    @Provides
    fun provideShopPayViewModel(
        bridgeHandler: BridgeHandler
    ): ShopPayViewModel {
        return ShopPayViewModel(bridgeHandler)
    }
}
