package com.stripe.android.paymentelement.embedded.content

import android.app.Application
import androidx.activity.result.ActivityResultCaller
import androidx.lifecycle.LifecycleOwner
import com.stripe.android.PaymentConfiguration
import com.stripe.android.core.injection.IS_LIVE_MODE
import com.stripe.android.paymentelement.EmbeddedPaymentElement
import com.stripe.android.paymentelement.embedded.DefaultEmbeddedResultCallbackHelper
import com.stripe.android.paymentelement.embedded.EmbeddedResultCallbackHelper
import dagger.Binds
import dagger.BindsInstance
import dagger.Module
import dagger.Provides
import dagger.Subcomponent
import javax.inject.Named
import javax.inject.Provider

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

    companion object {
        @Provides
        fun paymentConfiguration(application: Application): PaymentConfiguration {
            return PaymentConfiguration.getInstance(application)
        }

        @Provides
        @Named(IS_LIVE_MODE)
        fun isLiveMode(
            paymentConfiguration: Provider<PaymentConfiguration>
        ): () -> Boolean = { paymentConfiguration.get().publishableKey.startsWith("pk_live") }
    }
}
