package com.stripe.android.paymentsheet.injection

import com.stripe.android.paymentsheet.forms.FormViewModel
import com.stripe.android.paymentsheet.paymentdatacollection.FormFragmentArguments
import com.stripe.android.ui.core.elements.LayoutSpec
import dagger.BindsInstance
import dagger.Subcomponent

@Subcomponent
internal interface FormViewModelSubcomponent {
    val viewModel: FormViewModel

    @Subcomponent.Builder
    interface Builder {
        @BindsInstance
        fun paymentMethodCode(paymentMethodCode: String): Builder

        @BindsInstance
        fun formFragmentArguments(
            config: FormFragmentArguments
        ): Builder

        fun build(): FormViewModelSubcomponent
    }
}
