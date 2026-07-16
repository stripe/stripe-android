@file:OptIn(CheckoutSessionPreview::class)

package com.stripe.android.checkout.injection

import androidx.activity.result.ActivityResultCaller
import androidx.lifecycle.LifecycleOwner
import com.stripe.android.checkout.CheckoutConfirmationHelper
import com.stripe.android.checkout.CheckoutPresenter
import com.stripe.android.checkout.DefaultCheckoutConfirmationHelper
import com.stripe.android.paymentelement.CheckoutSessionPreview
import dagger.Binds
import dagger.BindsInstance
import dagger.Module
import dagger.Subcomponent

@Subcomponent(
    modules = [
        CheckoutPresenterModule::class,
        CurrencySelectorElementModule::class,
        ExpressCheckoutElementModule::class,
    ]
)
internal interface CheckoutPresenterSubcomponent {
    val presenter: CheckoutPresenter

    @Subcomponent.Factory
    interface Factory {
        fun create(
            @BindsInstance activityResultCaller: ActivityResultCaller,
            @BindsInstance lifecycleOwner: LifecycleOwner,
        ): CheckoutPresenterSubcomponent
    }
}

@Module
internal interface CheckoutPresenterModule {
    @Binds
    fun bindsConfirmationHelper(
        confirmationHelper: DefaultCheckoutConfirmationHelper
    ): CheckoutConfirmationHelper
}
