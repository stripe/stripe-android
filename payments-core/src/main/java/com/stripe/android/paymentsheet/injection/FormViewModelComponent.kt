package com.stripe.android.paymentsheet.injection

import com.stripe.android.paymentsheet.forms.FormViewModel
import com.stripe.android.paymentsheet.specifications.LayoutSpec
import dagger.BindsInstance
import dagger.Component
import javax.inject.Named
import javax.inject.Singleton

@Singleton
@Component(
    modules = [
        ResourceRepositoryModule::class,
        FormViewModelModule::class
    ]
)
internal interface FormViewModelComponent {
    val viewModel: FormViewModel

    @Component.Builder
    interface Builder {
        @BindsInstance
        fun layout(layoutSpec: LayoutSpec): Builder

        @BindsInstance
        fun saveForFutureUseValue(@Named(SAVE_FOR_FUTURE_USE_INITIAL_VALUE) saveForFutureUseValue: Boolean): Builder

        @BindsInstance
        fun saveForFutureUseVisibility(@Named(SAVE_FOR_FUTURE_USE_VISIBILITY) saveForFutureUseVisibility: Boolean): Builder

        @BindsInstance
        fun merchantName(merchantName: String): Builder

        fun build(): FormViewModelComponent
    }

}
