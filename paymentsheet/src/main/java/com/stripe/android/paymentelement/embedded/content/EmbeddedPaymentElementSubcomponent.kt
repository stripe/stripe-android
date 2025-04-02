@file:OptIn(ExperimentalEmbeddedPaymentElementApi::class)

package com.stripe.android.paymentelement.embedded.content

import androidx.activity.result.ActivityResultCaller
import androidx.lifecycle.LifecycleOwner
import com.stripe.android.paymentelement.EmbeddedPaymentElement
import com.stripe.android.paymentelement.ExperimentalEmbeddedPaymentElementApi
import com.stripe.android.paymentelement.embedded.DefaultEmbeddedResultCallbackHelper
import com.stripe.android.paymentelement.embedded.EmbeddedResultCallbackHelper
import dagger.Binds
import dagger.BindsInstance
import dagger.Module
import dagger.Subcomponent

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
}
