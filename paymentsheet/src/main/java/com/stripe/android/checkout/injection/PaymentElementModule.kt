package com.stripe.android.checkout.injection

import com.stripe.android.checkout.CheckoutControllerStateHolder
import com.stripe.android.checkout.CheckoutSavedPaymentMethodSelectionHandler
import com.stripe.android.checkout.CheckoutSheetLauncher
import com.stripe.android.elements.PaymentElement
import com.stripe.android.paymentelement.CheckoutSessionPreview
import com.stripe.android.paymentelement.embedded.DefaultEmbeddedRowSelectionImmediateActionHandler
import com.stripe.android.paymentelement.embedded.EmbeddedRowSelectionImmediateActionHandler
import com.stripe.android.paymentelement.embedded.EmbeddedSavedPaymentMethodSelectionHandler
import com.stripe.android.paymentelement.embedded.content.DefaultEmbeddedContentHelper
import com.stripe.android.paymentelement.embedded.content.DefaultEmbeddedLinkHelper
import com.stripe.android.paymentelement.embedded.content.DefaultEmbeddedPaymentMethodVerticalLayoutInteractorFactory
import com.stripe.android.paymentelement.embedded.content.DefaultEmbeddedPaymentOptionsPresenter
import com.stripe.android.paymentelement.embedded.content.DefaultEmbeddedWalletsHelper
import com.stripe.android.paymentelement.embedded.content.EmbeddedContentHelper
import com.stripe.android.paymentelement.embedded.content.EmbeddedContentHelperStateHolder
import com.stripe.android.paymentelement.embedded.content.EmbeddedLinkHelper
import com.stripe.android.paymentelement.embedded.content.EmbeddedPaymentMethodVerticalLayoutInteractorFactory
import com.stripe.android.paymentelement.embedded.content.EmbeddedPaymentOptionsPresenter
import com.stripe.android.paymentelement.embedded.content.EmbeddedSheetLauncher
import com.stripe.android.paymentelement.embedded.content.EmbeddedWalletsHelper
import com.stripe.android.uicore.utils.mapAsStateFlow
import dagger.Binds
import dagger.Module
import dagger.Provides
import kotlinx.coroutines.flow.StateFlow

@Module
internal interface PaymentElementModule {
    @Binds
    fun bindsEmbeddedContentHelper(helper: DefaultEmbeddedContentHelper): EmbeddedContentHelper

    @Binds
    fun bindsEmbeddedPaymentOptionsPresenter(
        presenter: DefaultEmbeddedPaymentOptionsPresenter,
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
    fun bindsEmbeddedSavedPaymentMethodSelectionHandler(
        handler: CheckoutSavedPaymentMethodSelectionHandler,
    ): EmbeddedSavedPaymentMethodSelectionHandler

    @Binds
    fun bindsSheetLauncher(launcher: CheckoutSheetLauncher): EmbeddedSheetLauncher

    @OptIn(CheckoutSessionPreview::class)
    companion object {
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
