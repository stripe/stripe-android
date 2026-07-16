@file:OptIn(CheckoutSessionPreview::class)

package com.stripe.android.checkout.injection

import androidx.activity.result.ActivityResultCaller
import androidx.lifecycle.LifecycleOwner
import com.stripe.android.checkout.CheckoutEmbeddedResultCallbackHelper
import com.stripe.android.checkout.CheckoutPaymentElementInitializer
import com.stripe.android.checkout.CheckoutPresenter
import com.stripe.android.paymentelement.CheckoutSessionPreview
import com.stripe.android.paymentelement.embedded.EmbeddedResultCallbackHelper
import com.stripe.android.paymentelement.embedded.content.DefaultEmbeddedSheetLauncher
import com.stripe.android.paymentelement.embedded.content.EmbeddedPaymentElementScope
import com.stripe.android.paymentelement.embedded.content.EmbeddedSheetLauncher
import dagger.Binds
import dagger.BindsInstance
import dagger.Module
import dagger.Subcomponent

@EmbeddedPaymentElementScope
@Subcomponent(
    modules = [
        CurrencySelectorElementModule::class,
        CheckoutSheetLauncherModule::class,
    ]
)
internal interface CheckoutPresenterSubcomponent {
    val presenter: CheckoutPresenter
    val initializer: CheckoutPaymentElementInitializer

    @Subcomponent.Factory
    interface Factory {
        fun create(
            @BindsInstance activityResultCaller: ActivityResultCaller,
            @BindsInstance lifecycleOwner: LifecycleOwner,
        ): CheckoutPresenterSubcomponent
    }
}

@Module
internal interface CheckoutSheetLauncherModule {
    @Binds
    fun bindsSheetLauncher(launcher: DefaultEmbeddedSheetLauncher): EmbeddedSheetLauncher

    @Binds
    fun bindsEmbeddedResultCallbackHelper(
        helper: CheckoutEmbeddedResultCallbackHelper
    ): EmbeddedResultCallbackHelper
}
