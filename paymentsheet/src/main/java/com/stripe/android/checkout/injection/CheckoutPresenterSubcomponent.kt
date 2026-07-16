@file:OptIn(CheckoutSessionPreview::class)

package com.stripe.android.checkout.injection

import androidx.activity.ComponentActivity
import com.stripe.android.checkout.CheckoutPaymentElementInitializer
import com.stripe.android.checkout.CheckoutPresenter
import com.stripe.android.paymentelement.CheckoutSessionPreview
import com.stripe.android.paymentelement.embedded.content.EmbeddedPaymentElementScope
import dagger.BindsInstance
import dagger.Subcomponent

@EmbeddedPaymentElementScope
@Subcomponent(
    modules = [
        CurrencySelectorElementModule::class,
        CheckoutPresenterModule::class,
    ]
)
internal interface CheckoutPresenterSubcomponent {
    val presenter: CheckoutPresenter
    val initializer: CheckoutPaymentElementInitializer

    @Subcomponent.Factory
    interface Factory {
        fun create(
            @BindsInstance activity: ComponentActivity,
        ): CheckoutPresenterSubcomponent
    }
}
