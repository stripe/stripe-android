package com.stripe.android.paymentelement.embedded.content

import androidx.activity.result.ActivityResultCaller
import androidx.activity.result.ActivityResultRegistryOwner
import androidx.lifecycle.LifecycleOwner
import com.stripe.android.link.LinkActivityContract
import com.stripe.android.link.LinkPaymentLauncher
import com.stripe.android.link.account.LinkStore
import com.stripe.android.link.injection.LinkAnalyticsComponent
import com.stripe.android.paymentelement.EmbeddedPaymentElement
import com.stripe.android.paymentelement.callbacks.PaymentElementCallbackIdentifier
import com.stripe.android.paymentelement.embedded.DefaultEmbeddedResultCallbackHelper
import com.stripe.android.paymentelement.embedded.EmbeddedResultCallbackHelper
import com.stripe.android.paymentelement.embedded.linkinline.DefaultEmbeddedLinkInlineActivityHost
import com.stripe.android.paymentelement.embedded.linkinline.EMBEDDED_LINK_WALLET_LAUNCHER
import com.stripe.android.paymentelement.embedded.linkinline.EmbeddedLinkInlineActivityHost
import dagger.Binds
import dagger.BindsInstance
import dagger.Module
import dagger.Provides
import dagger.Subcomponent
import javax.inject.Named

@Subcomponent(
    modules = [
        EmbeddedPaymentElementModule::class,
    ]
)
@EmbeddedPaymentElementScope
internal interface EmbeddedPaymentElementSubcomponent {
    val embeddedPaymentElement: EmbeddedPaymentElement
    val initializer: EmbeddedPaymentElementInitializer

    @Subcomponent.Factory
    interface Factory {
        fun build(
            @BindsInstance activityResultCaller: ActivityResultCaller,
            @BindsInstance activityResultRegistryOwner: ActivityResultRegistryOwner,
            @BindsInstance lifecycleOwner: LifecycleOwner,
            @BindsInstance resultCallback: EmbeddedPaymentElement.ResultCallback,
        ): EmbeddedPaymentElementSubcomponent
    }
}

@Module
internal interface EmbeddedPaymentElementModule {
    @Binds
    fun bindsSheetLauncher(
        launcher: DefaultEmbeddedSheetLauncher
    ): EmbeddedSheetLauncher

    @Binds
    fun bindsConfirmationHelper(
        confirmationHelper: DefaultEmbeddedConfirmationHelper
    ): EmbeddedConfirmationHelper

    @Binds
    fun bindsEmbeddedResultCallbackHelper(helper: DefaultEmbeddedResultCallbackHelper): EmbeddedResultCallbackHelper

    @Binds
    fun bindsEmbeddedLinkInlineActivityHost(
        host: DefaultEmbeddedLinkInlineActivityHost,
    ): EmbeddedLinkInlineActivityHost

    companion object {
        @Provides
        @Named(EMBEDDED_LINK_WALLET_LAUNCHER)
        @EmbeddedPaymentElementScope
        fun provideEmbeddedLinkWalletLauncher(
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
    }
}
