package com.stripe.android.checkout.injection

import androidx.activity.ComponentActivity
import dagger.BindsInstance
import dagger.Subcomponent

@Subcomponent
internal interface CheckoutPresenterSubcomponent {
    @Subcomponent.Factory
    interface Factory {
        fun create(@BindsInstance activity: ComponentActivity): CheckoutPresenterSubcomponent
    }
}
