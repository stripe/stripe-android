@file:OptIn(CheckoutSessionPreview::class)

package com.stripe.android.checkout.injection

import androidx.activity.result.ActivityResultCaller
import androidx.lifecycle.LifecycleOwner
import com.stripe.android.checkout.CheckoutPaymentElementInitializer
import com.stripe.android.checkout.CheckoutPresenter
import com.stripe.android.paymentelement.CheckoutSessionPreview
import com.stripe.android.payments.core.injection.STATUS_BAR_COLOR
import dagger.BindsInstance
import dagger.Subcomponent
import javax.inject.Named

@Subcomponent(
    modules = [
        CurrencySelectorElementModule::class,
        ExpressCheckoutElementModule::class,
        PaymentElementModule::class,
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
            @BindsInstance @Named(STATUS_BAR_COLOR) statusBarColor: Int?,
        ): CheckoutPresenterSubcomponent
    }
}
