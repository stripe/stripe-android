package com.stripe.android.checkout.injection

import com.stripe.android.checkout.CheckoutControllerStateHolder
import com.stripe.android.checkout.CheckoutLinkPaymentOptionsPresenter
import com.stripe.android.checkout.CheckoutSheetLauncher
import com.stripe.android.elements.PaymentElement
import com.stripe.android.link.LinkActivityContract
import com.stripe.android.link.LinkPaymentLauncher
import com.stripe.android.link.LinkPaymentMethodSelectionLauncher
import com.stripe.android.link.account.LinkAccountHolder
import com.stripe.android.link.account.LinkStore
import com.stripe.android.link.gate.LinkGate
import com.stripe.android.link.injection.LinkAnalyticsComponent
import com.stripe.android.paymentelement.CheckoutSessionPreview
import com.stripe.android.paymentelement.callbacks.PaymentElementCallbackIdentifier
import com.stripe.android.paymentelement.embedded.DefaultEmbeddedRowSelectionImmediateActionHandler
import com.stripe.android.paymentelement.embedded.EmbeddedRowSelectionImmediateActionHandler
import com.stripe.android.paymentelement.embedded.EmbeddedSelectionHolder
import com.stripe.android.paymentelement.embedded.content.DefaultEmbeddedContentHelper
import com.stripe.android.paymentelement.embedded.content.DefaultEmbeddedLinkHelper
import com.stripe.android.paymentelement.embedded.content.DefaultEmbeddedPaymentMethodVerticalLayoutInteractorFactory
import com.stripe.android.paymentelement.embedded.content.DefaultEmbeddedWalletsHelper
import com.stripe.android.paymentelement.embedded.content.EmbeddedContentHelper
import com.stripe.android.paymentelement.embedded.content.EmbeddedContentHelperStateHolder
import com.stripe.android.paymentelement.embedded.content.EmbeddedLinkHelper
import com.stripe.android.paymentelement.embedded.content.EmbeddedPaymentMethodVerticalLayoutInteractorFactory
import com.stripe.android.paymentelement.embedded.content.EmbeddedPaymentOptionsPresenter
import com.stripe.android.paymentelement.embedded.content.EmbeddedSheetLauncher
import com.stripe.android.paymentelement.embedded.content.EmbeddedWalletsHelper
import com.stripe.android.payments.core.injection.STATUS_BAR_COLOR
import com.stripe.android.paymentsheet.verticalmode.ImmediateVerticalPaymentSelectionHandler
import com.stripe.android.paymentsheet.verticalmode.VerticalPaymentSelectionHandler
import com.stripe.android.uicore.utils.mapAsStateFlow
import dagger.Binds
import dagger.Module
import dagger.Provides
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Named

internal const val CHECKOUT_LINK_PAYMENT_METHOD_SELECTION_LAUNCHER =
    "LinkPaymentLauncher_CheckoutPaymentMethodSelection"

@Module
internal interface PaymentElementModule {
    @Binds
    fun bindsEmbeddedContentHelper(helper: DefaultEmbeddedContentHelper): EmbeddedContentHelper

    @Binds
    fun bindsEmbeddedPaymentOptionsPresenter(
        presenter: CheckoutLinkPaymentOptionsPresenter,
    ): EmbeddedPaymentOptionsPresenter

    @Binds
    fun bindsEmbeddedPaymentMethodVerticalLayoutInteractorFactory(
        factory: DefaultEmbeddedPaymentMethodVerticalLayoutInteractorFactory
    ): EmbeddedPaymentMethodVerticalLayoutInteractorFactory

    @Binds
    fun bindsWalletsHelper(helper: DefaultEmbeddedWalletsHelper): EmbeddedWalletsHelper

    @Binds
    fun bindsLinkHelper(helper: DefaultEmbeddedLinkHelper): EmbeddedLinkHelper

    @Binds
    fun bindsEmbeddedRowSelectionImmediateActionHandler(
        handler: DefaultEmbeddedRowSelectionImmediateActionHandler,
    ): EmbeddedRowSelectionImmediateActionHandler

    @Binds
    fun bindsSheetLauncher(launcher: CheckoutSheetLauncher): EmbeddedSheetLauncher

    @OptIn(CheckoutSessionPreview::class)
    companion object {
        @Provides
        fun provideVerticalPaymentSelectionHandler(
            selectionHolder: EmbeddedSelectionHolder,
            immediateActionHandler: EmbeddedRowSelectionImmediateActionHandler,
        ): VerticalPaymentSelectionHandler {
            return ImmediateVerticalPaymentSelectionHandler(
                updateSelection = { selection, _ -> selectionHolder.setSelection(selection) },
                completionAction = immediateActionHandler::invoke,
            )
        }

        @Provides
        fun providePaymentElementConfiguration(
            stateHolder: CheckoutControllerStateHolder,
        ): PaymentElement.Configuration.State {
            return requireNotNull(stateHolder.state).configuration.paymentElementConfiguration
        }

        @Provides
        fun provideEmbeddedContentState(
            stateHolder: CheckoutControllerStateHolder,
        ): StateFlow<EmbeddedContentHelperStateHolder.State?> {
            return stateHolder.stateFlow.mapAsStateFlow { state ->
                state?.let {
                    EmbeddedContentHelperStateHolder.State(
                        paymentMethodMetadata = it.paymentMethodMetadata,
                        embeddedViewDisplaysMandateText = it.embeddedConfiguration.embeddedViewDisplaysMandateText,
                        configuration = it.embeddedConfiguration,
                    )
                }
            }
        }

        @Provides
        @Named(CHECKOUT_LINK_PAYMENT_METHOD_SELECTION_LAUNCHER)
        fun provideCheckoutLinkPaymentLauncher(
            linkAnalyticsComponentFactory: LinkAnalyticsComponent.Factory,
            linkActivityContract: LinkActivityContract,
            @PaymentElementCallbackIdentifier identifier: String,
            linkStore: LinkStore,
        ): LinkPaymentLauncher {
            return LinkPaymentLauncher(
                linkAnalyticsComponentFactory = linkAnalyticsComponentFactory,
                paymentElementCallbackIdentifier = identifier,
                linkActivityContract = linkActivityContract,
                linkStore = linkStore,
            )
        }

        @Provides
        fun provideLinkPaymentMethodSelectionLauncher(
            @Named(CHECKOUT_LINK_PAYMENT_METHOD_SELECTION_LAUNCHER) launcher: LinkPaymentLauncher,
            linkGateFactory: LinkGate.Factory,
            linkAccountHolder: LinkAccountHolder,
            @Named(STATUS_BAR_COLOR) statusBarColor: Int?,
        ): LinkPaymentMethodSelectionLauncher {
            return LinkPaymentMethodSelectionLauncher(
                launcher = launcher,
                linkGateFactory = linkGateFactory,
                linkAccountHolder = linkAccountHolder,
                statusBarColor = statusBarColor,
            )
        }
    }
}
