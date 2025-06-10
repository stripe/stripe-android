package com.stripe.android.paymentsheet.flowcontroller

import android.app.Application
import android.content.Context
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.stripe.android.link.LinkActivityContract
import com.stripe.android.link.LinkPaymentLauncher
import com.stripe.android.link.account.LinkStore
import com.stripe.android.link.injection.LinkAnalyticsComponent
import com.stripe.android.paymentelement.callbacks.PaymentElementCallbackIdentifier
import com.stripe.android.paymentelement.confirmation.ALLOWS_MANUAL_CONFIRMATION
import com.stripe.android.paymentelement.confirmation.ConfirmationHandler
import com.stripe.android.payments.core.injection.PRODUCT_USAGE
import com.stripe.android.paymentsheet.analytics.EventReporter
import com.stripe.android.paymentsheet.flowcontroller.DefaultFlowController.Companion.FLOW_CONTROLLER_LINK_LAUNCHER
import com.stripe.android.paymentsheet.flowcontroller.DefaultFlowController.Companion.WALLETS_BUTTON_LINK_LAUNCHER
import com.stripe.android.paymentsheet.injection.PaymentOptionsViewModelSubcomponent
import com.stripe.android.paymentsheet.ui.DefaultWalletButtonsInteractor
import com.stripe.android.paymentsheet.ui.WalletButtonsContent
import com.stripe.android.uicore.image.StripeImageLoader
import dagger.Module
import dagger.Provides
import kotlinx.coroutines.CoroutineScope
import javax.inject.Named
import javax.inject.Singleton

@Module(
    subcomponents = [
        PaymentOptionsViewModelSubcomponent::class,
    ]
)
internal object FlowControllerModule {
    @Provides
    @Singleton
    fun providesAppContext(application: Application): Context = application.applicationContext

    @Provides
    @Singleton
    fun provideEventReporterMode(): EventReporter.Mode = EventReporter.Mode.Custom

    @Provides
    @Singleton
    @Named(FLOW_CONTROLLER_LINK_LAUNCHER)
    fun provideFlowControllerLinkLauncher(
        linkAnalyticsComponentBuilder: LinkAnalyticsComponent.Builder,
        linkActivityContract: LinkActivityContract,
        @PaymentElementCallbackIdentifier identifier: String,
        linkStore: LinkStore,
    ) = LinkPaymentLauncher(
        linkAnalyticsComponentBuilder,
        identifier,
        linkActivityContract,
        linkStore,
    )

    @Provides
    @Singleton
    @Named(WALLETS_BUTTON_LINK_LAUNCHER)
    fun provideWalletsButtonLinkLauncher(
        linkAnalyticsComponentBuilder: LinkAnalyticsComponent.Builder,
        linkActivityContract: LinkActivityContract,
        @PaymentElementCallbackIdentifier identifier: String,
        linkStore: LinkStore,
    ) = LinkPaymentLauncher(
        linkAnalyticsComponentBuilder,
        identifier,
        linkActivityContract,
        linkStore,
    )

    @Provides
    @Singleton
    @Named(PRODUCT_USAGE)
    fun provideProductUsageTokens() = setOf("PaymentSheet.FlowController")

    @Provides
    @Singleton
    fun provideViewModelScope(viewModel: FlowControllerViewModel): CoroutineScope {
        return viewModel.viewModelScope
    }

    @Provides
    @Singleton
    fun providesSavedStateHandle(
        viewModel: FlowControllerViewModel,
    ): SavedStateHandle {
        return viewModel.handle
    }

    @Provides
    @Singleton
    fun providesConfirmationHandler(
        confirmationHandlerFactory: ConfirmationHandler.Factory,
        viewModel: FlowControllerViewModel,
    ): ConfirmationHandler {
        return confirmationHandlerFactory.create(viewModel.viewModelScope)
    }

    @Provides
    @Singleton
    fun providesWalletButtonsContent(
        viewModel: FlowControllerViewModel,
    ): WalletButtonsContent {
        return WalletButtonsContent(
            interactor = DefaultWalletButtonsInteractor.create(viewModel)
        )
    }

    @Provides
    @Singleton
    fun provideStripeImageLoader(context: Context): StripeImageLoader {
        return StripeImageLoader(context)
    }

    @Provides
    @Singleton
    @Named(ALLOWS_MANUAL_CONFIRMATION)
    fun provideAllowsManualConfirmation() = true
}
