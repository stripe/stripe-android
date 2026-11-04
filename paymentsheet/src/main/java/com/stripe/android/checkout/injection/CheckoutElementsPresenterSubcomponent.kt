@file:OptIn(CheckoutSessionPreview::class)

package com.stripe.android.checkout.injection

import androidx.activity.result.ActivityResultCaller
import androidx.activity.result.ActivityResultRegistry
import androidx.lifecycle.LifecycleOwner
import com.stripe.android.checkout.CheckoutElementsPresenter
import com.stripe.android.checkout.CheckoutElementsPresenterInitializer
import com.stripe.android.paymentelement.CheckoutSessionPreview
import com.stripe.android.payments.core.injection.STATUS_BAR_COLOR
import dagger.BindsInstance
import dagger.Subcomponent
import javax.inject.Named
import javax.inject.Scope

@CheckoutElementsPresenterScope
@Subcomponent(
    modules = [
        ExpressCheckoutElementModule::class,
        PaymentElementModule::class,
    ]
)
internal interface CheckoutElementsPresenterSubcomponent {
    val presenter: CheckoutElementsPresenter
    val initializer: CheckoutElementsPresenterInitializer

    @Subcomponent.Factory
    interface Factory {
        fun create(
            @BindsInstance activityResultCaller: ActivityResultCaller,
            @BindsInstance lifecycleOwner: LifecycleOwner,
            @BindsInstance activityResultRegistry: ActivityResultRegistry,
            @BindsInstance @Named(STATUS_BAR_COLOR) statusBarColor: Int?,
        ): CheckoutElementsPresenterSubcomponent
    }
}

@Scope
@Retention(AnnotationRetention.RUNTIME)
internal annotation class CheckoutElementsPresenterScope
