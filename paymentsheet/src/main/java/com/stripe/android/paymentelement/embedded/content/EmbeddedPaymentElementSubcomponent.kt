@file:OptIn(ExperimentalEmbeddedPaymentElementApi::class)

package com.stripe.android.paymentelement.embedded.content

import android.app.Application
import androidx.activity.result.ActivityResultCaller
import androidx.lifecycle.LifecycleOwner
import com.stripe.android.paymentelement.EmbeddedPaymentElement
import com.stripe.android.paymentelement.ExperimentalEmbeddedPaymentElementApi
import com.stripe.android.paymentelement.embedded.content.EmbeddedPaymentElementViewModelComponent.Builder
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

    @Subcomponent.Builder
    interface Builder {
        @BindsInstance
        fun activityResultCaller(activityResultCaller: ActivityResultCaller): Builder

        @BindsInstance
        fun lifecycleOwner(lifecycleOwner: LifecycleOwner): Builder

        @BindsInstance
        fun resultCallback(resultCallback: EmbeddedPaymentElement.ResultCallback): Builder

        @BindsInstance
        fun application(application: Application): Builder

        fun build(): EmbeddedPaymentElementSubcomponent
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
}
