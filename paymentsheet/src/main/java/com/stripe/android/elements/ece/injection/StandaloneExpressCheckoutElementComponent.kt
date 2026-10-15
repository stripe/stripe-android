package com.stripe.android.elements.ece.injection

import android.app.Application
import android.content.Context
import android.content.res.Resources
import androidx.lifecycle.SavedStateHandle
import com.stripe.android.common.di.ElementsSessionClientParamsModule
import com.stripe.android.core.injection.CoreCommonModule
import com.stripe.android.core.injection.CoroutineContextModule
import com.stripe.android.core.injection.ViewModelScope
import com.stripe.android.elements.ece.ExpressCheckoutElementIntegration
import com.stripe.android.elements.ece.ExpressCheckoutElementInteractor
import com.stripe.android.elements.ece.StandaloneExpressCheckoutElementController
import com.stripe.android.googlepaylauncher.GooglePayPaymentDataUpdateCallback
import com.stripe.android.googlepaylauncher.GooglePayPaymentDataUpdateResponse
import com.stripe.android.googlepaylauncher.injection.GooglePayLauncherModule
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadata
import com.stripe.android.networking.PaymentElementRequestSurfaceModule
import com.stripe.android.paymentelement.callbacks.PaymentElementCallbackIdentifier
import com.stripe.android.paymentelement.callbacks.PaymentElementCallbackReferences
import com.stripe.android.paymentelement.confirmation.ALLOWS_MANUAL_CONFIRMATION
import com.stripe.android.paymentelement.confirmation.ConfirmationHandler
import com.stripe.android.paymentelement.confirmation.cvc.CvcRecollectionConfirmationModule
import com.stripe.android.paymentelement.confirmation.injection.PaymentElementConfirmationModule
import com.stripe.android.payments.core.injection.ApiRequestOptionsModule
import com.stripe.android.payments.core.injection.PRODUCT_USAGE
import com.stripe.android.payments.core.injection.STATUS_BAR_COLOR
import com.stripe.android.payments.core.injection.StripeRepositoryModule
import com.stripe.android.paymentsheet.analytics.EventReporter
import com.stripe.android.paymentsheet.injection.ApiConfigurationModule
import com.stripe.android.paymentsheet.injection.LinkHoldbackExposureModule
import com.stripe.android.paymentsheet.injection.PaymentMethodMessagePromotionsExperimentHandlerModule
import com.stripe.android.paymentsheet.injection.PaymentSheetCommonModule
import com.stripe.android.paymentsheet.repositories.NoOpPaymentMethodMessagingPromotionHelperModule
import com.stripe.android.paymentsheet.state.NoOpTapToAddConnectionStarterModule
import dagger.BindsInstance
import dagger.Component
import dagger.Module
import dagger.Provides
import kotlinx.coroutines.CoroutineScope
import javax.inject.Named
import javax.inject.Provider
import javax.inject.Singleton

@Singleton
@Component(
    modules = [
        StandaloneExpressCheckoutElementModule::class,
        ExpressCheckoutElementInteractorModule::class,
        StripeRepositoryModule::class,
        ApiRequestOptionsModule::class,
        ApiConfigurationModule::class,
        PaymentElementConfirmationModule::class,
        CvcRecollectionConfirmationModule::class,
        NoOpTapToAddConnectionStarterModule::class,
        PaymentSheetCommonModule::class,
        PaymentElementRequestSurfaceModule::class,
        GooglePayLauncherModule::class,
        CoroutineContextModule::class,
        CoreCommonModule::class,
        ElementsSessionClientParamsModule::class,
        LinkHoldbackExposureModule::class,
        NoOpPaymentMethodMessagingPromotionHelperModule::class,
        PaymentMethodMessagePromotionsExperimentHandlerModule::class,
    ]
)
internal interface StandaloneExpressCheckoutElementComponent {
    val controller: StandaloneExpressCheckoutElementController
    val interactor: ExpressCheckoutElementInteractor

    @Component.Factory
    interface Factory {
        fun create(
            @BindsInstance application: Application,
            @BindsInstance savedStateHandle: SavedStateHandle,
            @BindsInstance @PaymentElementCallbackIdentifier callbackIdentifier: String,
            @BindsInstance @Named(STATUS_BAR_COLOR) statusBarColor: Int?,
            @BindsInstance @ViewModelScope coroutineScope: CoroutineScope,
        ): StandaloneExpressCheckoutElementComponent
    }
}

@Module
internal object StandaloneExpressCheckoutElementModule {
    @Provides
    fun provideIntegration(
        controller: StandaloneExpressCheckoutElementController,
    ): ExpressCheckoutElementIntegration = controller

    @Provides
    fun provideContext(application: Application): Context = application

    @Provides
    fun provideResources(context: Context): Resources = context.resources

    @Provides
    @Named(PRODUCT_USAGE)
    fun provideProductUsage(): Set<String> = setOf("ExpressCheckoutElement")

    @Provides
    fun provideEventReporterMode(): EventReporter.Mode = EventReporter.Mode.Custom

    @Provides
    @Named(ALLOWS_MANUAL_CONFIRMATION)
    @Suppress("FunctionOnlyReturningConstant")
    fun provideAllowsManualConfirmation(): Boolean = false

    @Provides
    @Singleton
    fun provideConfirmationHandler(
        factory: ConfirmationHandler.Factory,
        @ViewModelScope scope: CoroutineScope,
    ): ConfirmationHandler = factory.create(scope)

    @Provides
    fun providePaymentMethodMetadata(
        controller: Provider<StandaloneExpressCheckoutElementController>,
    ): PaymentMethodMetadata? = controller.get().paymentMethodMetadata()

    @Provides
    fun provideGooglePayCallback(
        @PaymentElementCallbackIdentifier callbackIdentifier: String,
    ): GooglePayPaymentDataUpdateCallback = GooglePayPaymentDataUpdateCallback { update ->
        val callback = PaymentElementCallbackReferences.getExact(callbackIdentifier)?.googlePayPaymentDataUpdateCallback
        callback?.onPaymentDataChanged(update) ?: GooglePayPaymentDataUpdateResponse(
            newTransactionInfo = null,
            error = null,
        )
    }
}
