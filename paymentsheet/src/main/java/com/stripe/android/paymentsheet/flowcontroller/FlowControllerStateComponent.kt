package com.stripe.android.paymentsheet.flowcontroller

import android.app.Application
import com.stripe.android.common.di.ApplicationIdModule
import com.stripe.android.common.di.MobileSessionIdModule
import com.stripe.android.core.injection.CoreCommonModule
import com.stripe.android.core.injection.CoroutineContextModule
import com.stripe.android.core.injection.ViewModelScope
import com.stripe.android.googlepaylauncher.injection.GooglePayLauncherModule
import com.stripe.android.link.account.LinkAccountHolder
import com.stripe.android.link.verification.DefaultLinkInlineInteractor
import com.stripe.android.networking.PaymentElementRequestSurfaceModule
import com.stripe.android.paymentelement.AnalyticEventCallback
import com.stripe.android.paymentelement.ExperimentalAnalyticEventCallbackApi
import com.stripe.android.paymentelement.callbacks.PaymentElementCallbackIdentifier
import com.stripe.android.paymentelement.confirmation.ConfirmationHandler
import com.stripe.android.paymentelement.confirmation.injection.ExtendedPaymentElementConfirmationModule
import com.stripe.android.payments.core.analytics.ErrorReporter
import com.stripe.android.payments.core.injection.STATUS_BAR_COLOR
import com.stripe.android.payments.core.injection.StripeRepositoryModule
import com.stripe.android.paymentsheet.LinkHandler
import com.stripe.android.paymentsheet.analytics.EventReporter
import com.stripe.android.paymentsheet.injection.CardArtExperimentModule
import com.stripe.android.paymentsheet.injection.LinkHoldbackExposureModule
import com.stripe.android.paymentsheet.injection.PaymentSheetCommonModule
import com.stripe.android.paymentsheet.repositories.PaymentMethodMessagePromotionsHelperModule
import com.stripe.android.paymentsheet.state.TapToAddConnectionStarterModule
import com.stripe.android.paymentsheet.ui.WalletButtonsContent
import com.stripe.android.ui.core.forms.resources.injection.ResourceRepositoryModule
import dagger.BindsInstance
import dagger.Component
import kotlinx.coroutines.CoroutineScope
import javax.inject.Named
import javax.inject.Provider
import javax.inject.Singleton

@OptIn(ExperimentalAnalyticEventCallbackApi::class)
@Singleton
@Component(
    modules = [
        StripeRepositoryModule::class,
        ExtendedPaymentElementConfirmationModule::class,
        TapToAddConnectionStarterModule::class,
        PaymentSheetCommonModule::class,
        PaymentElementRequestSurfaceModule::class,
        FlowControllerModule::class,
        GooglePayLauncherModule::class,
        CoroutineContextModule::class,
        CoreCommonModule::class,
        ResourceRepositoryModule::class,
        ApplicationIdModule::class,
        MobileSessionIdModule::class,
        LinkHoldbackExposureModule::class,
        CardArtExperimentModule::class,
        PaymentMethodMessagePromotionsHelperModule::class,
    ]
)
internal interface FlowControllerStateComponent {
    val flowControllerComponentFactory: FlowControllerComponent.Factory
    val confirmationHandler: ConfirmationHandler
    val linkHandler: LinkHandler
    val errorReporter: ErrorReporter
    val eventReporter: EventReporter
    val walletButtonsContent: WalletButtonsContent
    val linkInlineInteractor: DefaultLinkInlineInteractor
    val linkAccountHolder: LinkAccountHolder
    val analyticEventCallbackProvider: Provider<AnalyticEventCallback?>

    @Component.Factory
    interface Factory {
        fun create(
            @BindsInstance
            @Named(STATUS_BAR_COLOR)
            statusBarColor: Int?,
            @BindsInstance
            application: Application,
            @BindsInstance
            @PaymentElementCallbackIdentifier
            paymentElementCallbackIdentifier: String,
            @BindsInstance
            flowControllerViewModel: FlowControllerViewModel,
            @BindsInstance
            @ViewModelScope
            viewModelScope: CoroutineScope,
        ): FlowControllerStateComponent
    }
}
