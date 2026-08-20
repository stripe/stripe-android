package com.stripe.android.paymentelement.embedded.sheet

import android.app.Application
import android.content.Context
import androidx.lifecycle.SavedStateHandle
import com.stripe.android.common.di.ElementsSessionClientParamsModule
import com.stripe.android.core.injection.CoreCommonModule
import com.stripe.android.core.injection.CoroutineContextModule
import com.stripe.android.core.injection.ViewModelScope
import com.stripe.android.googlepaylauncher.injection.GooglePayLauncherModule
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadata
import com.stripe.android.networking.PaymentElementRequestSurfaceModule
import com.stripe.android.paymentelement.callbacks.PaymentElementCallbackIdentifier
import com.stripe.android.payments.core.injection.PRODUCT_USAGE
import com.stripe.android.payments.core.injection.StripeRepositoryModule
import com.stripe.android.paymentsheet.PaymentSheetContract
import com.stripe.android.paymentsheet.analytics.EventReporter
import com.stripe.android.paymentsheet.cvcrecollection.CvcRecollectionHandler
import com.stripe.android.paymentsheet.cvcrecollection.CvcRecollectionHandlerImpl
import com.stripe.android.paymentsheet.injection.LinkHoldbackExposureModule
import com.stripe.android.paymentsheet.injection.PaymentMethodMessagePromotionsExperimentHandlerModule
import com.stripe.android.paymentsheet.injection.PaymentSheetCommonModule
import com.stripe.android.paymentsheet.repositories.PaymentMethodMessagePromotionsHelper
import com.stripe.android.paymentsheet.repositories.PaymentMethodMessagePromotionsHelperModule
import com.stripe.android.paymentsheet.state.PaymentElementLoader
import com.stripe.android.paymentsheet.state.TapToAddConnectionStarterModule
import com.stripe.android.ui.core.forms.resources.injection.ResourceRepositoryModule
import dagger.BindsInstance
import dagger.Component
import dagger.Module
import dagger.Provides
import kotlinx.coroutines.CoroutineScope
import javax.inject.Named
import javax.inject.Singleton

@Singleton
@Component(
    modules = [
        StripeRepositoryModule::class,
        PaymentSheetCommonModule::class,
        PaymentElementRequestSurfaceModule::class,
        GooglePayLauncherModule::class,
        CoroutineContextModule::class,
        CoreCommonModule::class,
        ResourceRepositoryModule::class,
        ElementsSessionClientParamsModule::class,
        LinkHoldbackExposureModule::class,
        SheetActivityLoaderModule::class,
        TapToAddConnectionStarterModule::class,
        PaymentMethodMessagePromotionsHelperModule::class,
        PaymentMethodMessagePromotionsExperimentHandlerModule::class,
    ]
)
internal interface SheetActivityLoaderComponent {
    val paymentElementLoader: PaymentElementLoader
    val promotionsHelper: PaymentMethodMessagePromotionsHelper
    val eventReporter: EventReporter

    @Component.Factory
    interface Factory {
        fun build(
            @BindsInstance application: Application,
            @BindsInstance savedStateHandle: SavedStateHandle,
            @BindsInstance args: PaymentSheetContract.Args,
            @BindsInstance
            @ViewModelScope
            viewModelScope: CoroutineScope,
        ): SheetActivityLoaderComponent
    }
}

@Module
internal object SheetActivityLoaderModule {
    @Provides
    fun provideContext(application: Application): Context = application

    @Provides
    @PaymentElementCallbackIdentifier
    fun provideCallbackIdentifier(args: PaymentSheetContract.Args): String {
        return args.paymentElementCallbackIdentifier
    }

    @Provides
    @Singleton
    fun provideEventReporterMode(): EventReporter.Mode = EventReporter.Mode.Complete

    @Provides
    @Named(PRODUCT_USAGE)
    fun provideProductUsage(): Set<String> = setOf("PaymentSheet")

    @Provides
    @Suppress("FunctionOnlyReturningConstant")
    fun providePaymentMethodMetadata(): PaymentMethodMetadata? = null

    @Provides
    fun provideCvcRecollectionHandler(): CvcRecollectionHandler {
        return CvcRecollectionHandlerImpl()
    }
}
