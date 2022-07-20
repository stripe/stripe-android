package com.stripe.android.paymentsheet.injection

import android.content.Context
import android.content.res.Resources
import com.stripe.android.core.injection.CoroutineContextModule
import com.stripe.android.core.injection.LoggingModule
import com.stripe.android.payments.core.injection.PRODUCT_USAGE
import com.stripe.android.paymentsheet.forms.FormViewModel
import dagger.BindsInstance
import dagger.Component
import javax.inject.Named
import javax.inject.Singleton

@Singleton
@Component(
    modules = [
        FormViewModelModule::class,
        CoroutineContextModule::class,
        PaymentSheetCommonModule::class,
        LoggingModule::class,
    ]
)
internal interface FormViewModelComponent {
    fun inject(factory: FormViewModel.Factory)

    @Component.Builder
    interface Builder {
        @BindsInstance
        fun context(context: Context): Builder

        @BindsInstance
        fun resources(resources: Resources): Builder

        @BindsInstance
        fun productUsage(@Named(PRODUCT_USAGE) productUsage: Set<String>): Builder

        fun build(): FormViewModelComponent
    }
}
