package com.stripe.android.shoppay.di

import com.stripe.android.BuildConfig
import com.stripe.android.SharedPaymentTokenSessionPreview
import com.stripe.android.core.Logger
import com.stripe.android.core.injection.ENABLE_LOGGING
import com.stripe.android.core.injection.IOContext
import com.stripe.android.core.model.parsers.ModelJsonParser
import com.stripe.android.paymentelement.PreparePaymentMethodHandler
import com.stripe.android.paymentelement.ShopPayPreview
import com.stripe.android.paymentelement.callbacks.PaymentElementCallbackIdentifier
import com.stripe.android.paymentelement.callbacks.PaymentElementCallbackReferences
import com.stripe.android.payments.core.injection.PRODUCT_USAGE
import com.stripe.android.payments.core.injection.StripeRepositoryModule
import com.stripe.android.paymentsheet.ShopPayHandlers
import com.stripe.android.shoppay.bridge.ConfirmationRequest
import com.stripe.android.shoppay.bridge.ConfirmationRequestJsonParser
import com.stripe.android.shoppay.bridge.DefaultShopPayBridgeHandler
import com.stripe.android.shoppay.bridge.ECEShippingRate
import com.stripe.android.shoppay.bridge.ECEShippingRateJsonParser
import com.stripe.android.shoppay.bridge.HandleClickRequest
import com.stripe.android.shoppay.bridge.HandleClickRequestJsonParser
import com.stripe.android.shoppay.bridge.ShippingCalculationRequest
import com.stripe.android.shoppay.bridge.ShippingCalculationRequestJsonParser
import com.stripe.android.shoppay.bridge.ShippingRateChangeRequest
import com.stripe.android.shoppay.bridge.ShippingRateChangeRequestJsonParser
import com.stripe.android.shoppay.bridge.ShopPayBridgeHandler
import dagger.Binds
import dagger.Module
import dagger.Provides
import kotlinx.coroutines.Dispatchers
import javax.inject.Named
import kotlin.coroutines.CoroutineContext

@Module(
    includes = [
        StripeRepositoryModule::class
    ]
)
internal interface ShopPayModule {
    @Binds
    fun bindBridgeHandler(bridgeHandler: DefaultShopPayBridgeHandler): ShopPayBridgeHandler

    @Binds
    fun bindsHandleClickRequestJsonParser(
        parser: HandleClickRequestJsonParser
    ): ModelJsonParser<HandleClickRequest>

    @Binds
    fun bindsShippingCalculationRequestJsonParser(
        parser: ShippingCalculationRequestJsonParser
    ): ModelJsonParser<ShippingCalculationRequest>

    @Binds
    fun bindsShippingRateChangeRequestJsonParser(
        parser: ShippingRateChangeRequestJsonParser
    ): ModelJsonParser<ShippingRateChangeRequest>

    @Binds
    fun bindsConfirmationRequestJsonParser(
        parser: ConfirmationRequestJsonParser
    ): ModelJsonParser<ConfirmationRequest>

    @Binds
    fun bindsECEShippingRateJsonParser(
        parser: ECEShippingRateJsonParser
    ): ModelJsonParser<ECEShippingRate>

    companion object {
        @OptIn(ShopPayPreview::class)
        @Provides
        fun provideShopPayHandlers(
            @PaymentElementCallbackIdentifier paymentElementCallbackIdentifier: String
        ): ShopPayHandlers {
            return PaymentElementCallbackReferences[paymentElementCallbackIdentifier]
                ?.shopPayHandlers
                ?: throw IllegalStateException("ShopPayHandlers not found")
        }

        @OptIn(SharedPaymentTokenSessionPreview::class)
        @Provides
        fun providePreparePaymentMethodHandler(
            @PaymentElementCallbackIdentifier paymentElementCallbackIdentifier: String
        ): PreparePaymentMethodHandler {
            return PaymentElementCallbackReferences[paymentElementCallbackIdentifier]
                ?.preparePaymentMethodHandler
                ?: throw IllegalStateException("PreparePaymentMethodHandler not found")
        }

        @Provides
        @IOContext
        fun provideWorkContext(): CoroutineContext = Dispatchers.IO

        @Provides
        @Named(PRODUCT_USAGE)
        fun provideProductUsageTokens() = setOf("PaymentSheet")

        @Provides
        fun provideLogger(@Named(ENABLE_LOGGING) enableLogging: Boolean) = Logger.getInstance(enableLogging)

        @Provides
        @Named(ENABLE_LOGGING)
        fun providesEnableLogging(): Boolean = BuildConfig.DEBUG
    }
}
