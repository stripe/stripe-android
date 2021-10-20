package com.stripe.android.paymentsheet.injection

import android.content.res.Resources
import com.stripe.android.payments.core.injection.CoroutineContextModule
import com.stripe.android.paymentsheet.forms.FormViewModel
import dagger.BindsInstance
import dagger.Component
import javax.inject.Singleton

@Singleton
@Component(
    modules = [
        FormViewModelModule::class,
        CoroutineContextModule::class
    ]
)

internal interface FormViewModelComponent {
    fun inject(factory: FormViewModel.Factory)

    @Component.Builder
    interface Builder {
        @BindsInstance
        fun resources(resources: Resources): Builder

        fun build(): FormViewModelComponent
    }
}
