package com.stripe.android.checkout.injection

import androidx.activity.result.ActivityResultCaller
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.SavedStateHandle
import com.stripe.android.checkout.CheckoutControllerStateHolder
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadata
import com.stripe.android.paymentelement.confirmation.ConfirmationHandler
import com.stripe.android.paymentelement.embedded.DefaultEmbeddedRowSelectionImmediateActionHandler
import com.stripe.android.paymentelement.embedded.EmbeddedRowSelectionImmediateActionHandler
import com.stripe.android.paymentelement.embedded.content.DefaultEmbeddedContentHelper
import com.stripe.android.paymentelement.embedded.content.DefaultEmbeddedLinkHelper
import com.stripe.android.paymentelement.embedded.content.DefaultEmbeddedWalletsHelper
import com.stripe.android.paymentelement.embedded.content.EmbeddedContentHelper
import com.stripe.android.paymentelement.embedded.content.EmbeddedLinkHelper
import com.stripe.android.paymentelement.embedded.content.EmbeddedWalletsHelper
import com.stripe.android.paymentsheet.CustomerStateHolder
import com.stripe.android.paymentsheet.DefaultCustomerStateHolder
import com.stripe.android.uicore.utils.mapAsStateFlow
import com.stripe.android.uicore.utils.stateFlowOf
import dagger.Binds
import dagger.Module
import dagger.Provides
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Singleton

@Module
internal interface PaymentElementModule {
    @Binds
    fun bindsEmbeddedContentHelper(
        helper: DefaultEmbeddedContentHelper
    ): EmbeddedContentHelper

    @Binds
    fun bindsLinkHelper(
        helper: DefaultEmbeddedLinkHelper
    ): EmbeddedLinkHelper

    @Binds
    fun bindsEmbeddedRowSelectionImmediateActionHandler(
        handler: DefaultEmbeddedRowSelectionImmediateActionHandler
    ): EmbeddedRowSelectionImmediateActionHandler

    @Binds
    fun bindsWalletsHelper(
        helper: DefaultEmbeddedWalletsHelper
    ): EmbeddedWalletsHelper

    companion object {
        @Provides
        @Singleton
        fun provideCustomerStateHolder(
            savedStateHandle: SavedStateHandle,
            stateHolder: CheckoutControllerStateHolder,
        ): CustomerStateHolder {
            val paymentMethodMetadataStateFlow = stateHolder.stateFlow.mapAsStateFlow { it?.paymentMethodMetadata }
            val customerMetadata = paymentMethodMetadataStateFlow.mapAsStateFlow { it?.customerMetadata }
            return DefaultCustomerStateHolder(
                savedStateHandle = savedStateHandle,
                selection = stateHolder.stateFlow.mapAsStateFlow { it?.paymentSelection },
                customerMetadata = customerMetadata,
                paymentMethodMetadataFlow = paymentMethodMetadataStateFlow,
            )
        }

        @Provides
        @Singleton
        fun provideConfirmationHandler(): ConfirmationHandler {
            return object : ConfirmationHandler {
                override val hasReloadedFromProcessDeath: Boolean
                    get() = false
                override val state: StateFlow<ConfirmationHandler.State>
                    get() = stateFlowOf(ConfirmationHandler.State.Idle)

                override fun register(
                    activityResultCaller: ActivityResultCaller,
                    lifecycleOwner: LifecycleOwner
                ) {
                }

                override fun bootstrap(paymentMethodMetadata: PaymentMethodMetadata) {
                }

                override suspend fun start(arguments: ConfirmationHandler.Args) {
                }

                override suspend fun awaitResult(): ConfirmationHandler.Result? {
                    return null
                }
            }
        }
    }
}
