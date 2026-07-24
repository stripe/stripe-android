package com.stripe.android.checkout.injection

import com.stripe.android.checkout.CheckoutControllerStateHolder
import com.stripe.android.checkout.CheckoutSheetLauncher
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
        handler: DefaultEmbeddedRowSelectionImmediateActionHandler
    ): EmbeddedRowSelectionImmediateActionHandler

    @Binds
    fun bindsSheetLauncher(launcher: CheckoutSheetLauncher): EmbeddedSheetLauncher

    companion object {
        @Provides
        fun provideEmbeddedContentState(
            stateHolder: CheckoutControllerStateHolder,
        ): StateFlow<EmbeddedContentHelperStateHolder.State?> {
            return stateHolder.stateFlow.mapAsStateFlow { state ->
                state?.let {
                    EmbeddedContentHelperStateHolder.State(
                        paymentMethodMetadata = it.paymentMethodMetadata,
                        appearance = it.embeddedConfiguration.appearance.embeddedAppearance,
                        embeddedViewDisplaysMandateText = it.embeddedConfiguration.embeddedViewDisplaysMandateText,
                        configuration = it.embeddedConfiguration,
                    )
                }
            }
        }
    }
}
