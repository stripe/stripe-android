package com.stripe.android.shoppay.di

import com.stripe.android.shoppay.bridge.BridgeHandler
import com.stripe.android.shoppay.bridge.DefaultBridgeHandler
import dagger.Binds
import dagger.Module

@Module
internal interface ShopPayModule {
    @Binds
    fun bindBridgeHandler(bridgeHandler: DefaultBridgeHandler): BridgeHandler
}
