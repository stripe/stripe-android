package com.stripe.android.checkout.injection

import com.stripe.android.checkout.CheckoutControllerStateHolder
import com.stripe.android.checkout.CheckoutSheetLauncher
import com.stripe.android.checkout.CheckoutSheetLinkHelper
import com.stripe.android.checkout.DefaultCheckoutSheetLinkHelper
import com.stripe.android.checkout.PAYMENT_ELEMENT_LINK_LAUNCHER
import com.stripe.android.elements.PaymentElement
import com.stripe.android.link.LinkActivityContract
import com.stripe.android.link.LinkPaymentLauncher
import com.stripe.android.link.account.LinkStore
import com.stripe.android.link.injection.LinkAnalyticsComponent
import com.stripe.android.paymentelement.CheckoutSessionPreview
import com.stripe.android.paymentelement.callbacks.PaymentElementCallbackIdentifier
import com.stripe.android.paymentelement.embedded.DefaultEmbeddedRowSelectionImmediateActionHandler
import com.stripe.android.paymentelement.embedded.EmbeddedRowSelectionImmediateActionHandler
import com.stripe.android.paymentelement.embedded.content.DefaultEmbeddedContentHelper
import com.stripe.android.paymentelement.embedded.content.DefaultEmbeddedLinkHelper
import com.stripe.android.paymentelement.embedded.content.DefaultEmbeddedPaymentMethodVerticalLayoutInteractorFactory
import com.stripe.android.paymentelement.embedded.content.DefaultEmbeddedWalletsHelper
import com.stripe.android.paymentelement.embedded.content.EmbeddedContentHelper
import com.stripe.android.paymentelement.embedded.content.EmbeddedContentHelperStateHolder
import com.stripe.android.paymentelement.embedded.content.EmbeddedLinkHelper
import com.stripe.android.paymentelement.embedded.content.EmbeddedPaymentMethodVerticalLayoutInteractorFactory
import com.stripe.android.paymentelement.embedded.content.EmbeddedSheetLauncher
import com.stripe.android.paymentelement.embedded.content.EmbeddedWalletsHelper
import com.stripe.android.uicore.utils.mapAsStateFlow
import dagger.Binds
import dagger.Module
import dagger.Provides
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Named

@Module
internal interface PaymentElementModule {
    @Binds
    fun bindsEmbeddedContentHelper(helper: DefaultEmbeddedContentHelper): EmbeddedContentHelper

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

    @Binds
    fun bindsCheckoutSheetLinkHelper(helper: DefaultCheckoutSheetLinkHelper): CheckoutSheetLinkHelper

    @OptIn(CheckoutSessionPreview::class)
    companion object {
        @Provides
        @Named(PAYMENT_ELEMENT_LINK_LAUNCHER)
        fun providePaymentElementLinkLauncher(
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
    }
}
