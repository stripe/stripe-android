package com.stripe.android.paymentsheet.injection

import com.stripe.android.paymentsheet.elements.LayoutSpec
import com.stripe.android.paymentsheet.forms.FormViewModel
import com.stripe.android.paymentsheet.paymentdatacollection.FormFragmentArguments
import dagger.BindsInstance
import dagger.Subcomponent

@Subcomponent
internal interface FormViewModelSubcomponent {
    val viewModel: FormViewModel

    @Subcomponent.Builder
    interface Builder {
        @BindsInstance
        fun layout(layoutSpec: LayoutSpec): Builder

        @BindsInstance
        fun formFragmentArguments(
            config: FormFragmentArguments
        ): Builder

        fun build(): FormViewModelSubcomponent
    }
}
