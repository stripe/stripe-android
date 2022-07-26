package com.stripe.android.paymentsheet.injection

import android.content.Context
import com.stripe.android.core.injection.CoreCommonModule
import com.stripe.android.core.injection.CoroutineContextModule
import com.stripe.android.paymentsheet.forms.FormViewModel
import com.stripe.android.ui.core.forms.resources.injection.ResourceRepositoryModule
import dagger.BindsInstance
import dagger.Component
import javax.inject.Singleton

@Singleton
@Component(
    modules = [
        FormViewModelModule::class,
        CoroutineContextModule::class,
        ResourceRepositoryModule::class,
        CoreCommonModule::class
    ]
)
internal interface FormViewModelComponent {
    fun inject(factory: FormViewModel.Factory)

    @Component.Builder
    interface Builder {
        @BindsInstance
        fun context(context: Context): Builder

        fun build(): FormViewModelComponent
    }
}
