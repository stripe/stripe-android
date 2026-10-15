package com.stripe.android.elements.injection

import android.app.Application
import android.content.Context
import android.content.res.Resources
import androidx.lifecycle.SavedStateHandle
import com.stripe.android.common.di.ElementsSessionClientParamsModule
import com.stripe.android.core.injection.CoreCommonModule
import com.stripe.android.core.injection.CoroutineContextModule
import com.stripe.android.core.injection.ViewModelScope
import com.stripe.android.elements.ElementsController
import com.stripe.android.elements.ElementsControllerSavedState
import com.stripe.android.elements.ElementsControllerStateHolder
import com.stripe.android.elements.ece.ElementsExpressCheckoutElementIntegration
import com.stripe.android.elements.ece.ElementsGooglePayPaymentDataUpdateCallback
import com.stripe.android.elements.ece.ExpressCheckoutElementIntegration
import com.stripe.android.elements.ece.injection.ExpressCheckoutElementCommonModule
import com.stripe.android.googlepaylauncher.GooglePayPaymentDataUpdateCallback
import com.stripe.android.googlepaylauncher.injection.GooglePayLauncherModule
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadata
import com.stripe.android.networking.PaymentElementRequestSurfaceModule
import com.stripe.android.paymentelement.callbacks.PaymentElementCallbackIdentifier
import com.stripe.android.paymentelement.confirmation.ALLOWS_MANUAL_CONFIRMATION
import com.stripe.android.paymentelement.confirmation.ConfirmationHandler
import com.stripe.android.paymentelement.confirmation.cvc.CvcRecollectionConfirmationModule
import com.stripe.android.paymentelement.confirmation.injection.PaymentElementConfirmationModule
import com.stripe.android.payments.core.injection.ApiRequestOptionsModule
import com.stripe.android.payments.core.injection.PRODUCT_USAGE
import com.stripe.android.payments.core.injection.StripeRepositoryModule
import com.stripe.android.paymentsheet.PaymentSheetResultCallback
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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import javax.inject.Named
import javax.inject.Singleton

@Singleton
@Component(
    modules = [
        ElementsControllerModule::class,
        ExpressCheckoutElementCommonModule::class,
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
internal interface ElementsControllerComponent {
    val controller: ElementsController

    @Component.Factory
    interface Factory {
        fun create(
            @BindsInstance application: Application,
            @BindsInstance savedState: ElementsControllerSavedState,
            @BindsInstance resultCallback: PaymentSheetResultCallback,
            @BindsInstance @PaymentElementCallbackIdentifier callbackIdentifier: String,
        ): ElementsControllerComponent
    }
}

@Module
internal object ElementsControllerModule {
    @Provides
    fun provideSavedStateHandle(savedState: ElementsControllerSavedState): SavedStateHandle = savedState.handle

    @Provides
    @Singleton
    @ViewModelScope
    fun provideCoroutineScope(): CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    @Provides
    fun provideIntegration(
        integration: ElementsExpressCheckoutElementIntegration,
    ): ExpressCheckoutElementIntegration = integration

    @Provides
    fun provideContext(application: Application): Context = application

    @Provides
    fun provideResources(context: Context): Resources = context.resources

    @Provides
    @Named(PRODUCT_USAGE)
    fun provideProductUsage(): Set<String> = setOf("ElementsController")

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
        stateHolder: ElementsControllerStateHolder,
    ): PaymentMethodMetadata? = stateHolder.state?.paymentMethodMetadata

    @Provides
    fun provideGooglePayCallback(
        callback: ElementsGooglePayPaymentDataUpdateCallback,
    ): GooglePayPaymentDataUpdateCallback = callback
}
