package com.stripe.android.link.injection

import com.stripe.android.link.ui.paymentmethod.FormViewModel
import com.stripe.android.ui.core.elements.LayoutSpec
import dagger.BindsInstance
import dagger.Subcomponent

/**
 * Subcomponent used by FormViewModel.
 */
@Subcomponent(
    modules = [FormViewModelModule::class]
)
internal interface FormViewModelSubcomponent {
    val formViewModel: FormViewModel

    @Subcomponent.Builder
    interface Builder {

        @BindsInstance
        fun formSpec(formSpec: LayoutSpec): Builder

        fun build(): FormViewModelSubcomponent
    }
}
