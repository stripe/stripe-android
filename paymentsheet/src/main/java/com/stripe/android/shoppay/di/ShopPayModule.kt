package com.stripe.android.shoppay.di

import com.stripe.android.core.model.parsers.ModelJsonParser
import com.stripe.android.shoppay.bridge.BridgeHandler
import com.stripe.android.shoppay.bridge.DefaultBridgeHandler
import com.stripe.android.shoppay.bridge.HandleClickRequest
import com.stripe.android.shoppay.bridge.HandleClickRequestJsonParser
import dagger.Binds
import dagger.Module
import dagger.Provides

@Module
internal interface ShopPayModule {
    @Binds
    fun bindBridgeHandler(bridgeHandler: DefaultBridgeHandler): BridgeHandler

    companion object {
        @Provides
        fun providesHandleClickRequestJsonParser(): ModelJsonParser<HandleClickRequest> {
            return HandleClickRequestJsonParser()
        }
    }
}
