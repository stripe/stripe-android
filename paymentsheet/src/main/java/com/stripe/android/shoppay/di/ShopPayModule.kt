package com.stripe.android.shoppay.di

import com.stripe.android.BuildConfig
import com.stripe.android.SharedPaymentTokenSessionPreview
import com.stripe.android.core.injection.ENABLE_LOGGING
import com.stripe.android.core.model.parsers.ModelJsonParser
import com.stripe.android.core.networking.AnalyticsRequestExecutor
import com.stripe.android.core.networking.AnalyticsRequestFactory
import com.stripe.android.core.utils.DefaultDurationProvider
import com.stripe.android.core.utils.DurationProvider
import com.stripe.android.core.utils.RealUserFacingLogger
import com.stripe.android.core.utils.UserFacingLogger
import com.stripe.android.elements.payment.AnalyticEventCallback
import com.stripe.android.elements.payment.PreparePaymentMethodHandler
import com.stripe.android.elements.payment.ShopPayHandlers
import com.stripe.android.elements.payment.ShopPayPreview
import com.stripe.android.networking.PaymentAnalyticsRequestFactory
import com.stripe.android.paymentelement.ExperimentalAnalyticEventCallbackApi
import com.stripe.android.paymentelement.callbacks.PaymentElementCallbackIdentifier
import com.stripe.android.paymentelement.callbacks.PaymentElementCallbackReferences
import com.stripe.android.payments.core.analytics.ErrorReporter
import com.stripe.android.payments.core.analytics.RealErrorReporter
import com.stripe.android.payments.core.injection.PRODUCT_USAGE
import com.stripe.android.payments.core.injection.StripeRepositoryModule
import com.stripe.android.paymentsheet.analytics.DefaultEventReporter
import com.stripe.android.paymentsheet.analytics.EventReporter
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
import com.stripe.android.ui.core.IsStripeCardScanAvailable
import dagger.Binds
import dagger.Module
import dagger.Provides
import javax.inject.Named

@Module(
    includes = [
        StripeRepositoryModule::class,
    ]
)
internal interface ShopPayModule {
    @Binds
    fun bindBridgeHandler(bridgeHandler: DefaultShopPayBridgeHandler): ShopPayBridgeHandler

    @Binds
    fun bindEventReporter(eventReporter: DefaultEventReporter): EventReporter

    @Binds
    fun bindUserFacingLogger(logger: RealUserFacingLogger): UserFacingLogger

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

    @Binds
    fun bindsAnalyticsRequestFactory(
        paymentAnalyticsRequestFactory: PaymentAnalyticsRequestFactory
    ): AnalyticsRequestFactory

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
        ): PreparePaymentMethodHandler? {
            return PaymentElementCallbackReferences[paymentElementCallbackIdentifier]
                ?.preparePaymentMethodHandler
        }

        @Provides
        @Named(PRODUCT_USAGE)
        fun provideProductUsageTokens() = setOf("PaymentSheet")

        @Provides
        @Named(ENABLE_LOGGING)
        fun providesEnableLogging(): Boolean = BuildConfig.DEBUG

        @Provides
        fun provideEventReporterMode(): EventReporter.Mode = EventReporter.Mode.Custom

        @Provides
        fun provideDurationProvider(): DurationProvider = DefaultDurationProvider.instance

        @Provides
        internal fun providesErrorReporter(
            analyticsRequestFactory: AnalyticsRequestFactory,
            analyticsRequestExecutor: AnalyticsRequestExecutor
        ): ErrorReporter = RealErrorReporter(
            analyticsRequestFactory = analyticsRequestFactory,
            analyticsRequestExecutor = analyticsRequestExecutor,
        )

        @OptIn(ExperimentalAnalyticEventCallbackApi::class)
        @Provides
        fun provideAnalyticEventCallback(
            @PaymentElementCallbackIdentifier paymentElementCallbackIdentifier: String
        ): AnalyticEventCallback? {
            return PaymentElementCallbackReferences[paymentElementCallbackIdentifier]?.analyticEventCallback
        }

        @Provides
        fun provideIsStripeCardScanAvailable(): IsStripeCardScanAvailable {
            return object : IsStripeCardScanAvailable {
                override fun invoke() = false
            }
        }
    }
}
