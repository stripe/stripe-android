package com.stripe.android.paymentsheet.injection

import com.stripe.android.paymentsheet.forms.FormViewModel
import com.stripe.android.paymentsheet.paymentdatacollection.FormFragmentArguments
import dagger.BindsInstance
import dagger.Subcomponent
import kotlinx.coroutines.flow.Flow

@Subcomponent
internal interface FormViewModelSubcomponent {
    val viewModel: FormViewModel

    @Subcomponent.Builder
    interface Builder {
        @BindsInstance
        fun formFragmentArguments(
            config: FormFragmentArguments
        ): Builder

        @BindsInstance
        fun showCheckboxFlow(saveForFutureUseVisibleFlow: Flow<Boolean>): Builder

        fun build(): FormViewModelSubcomponent
    }
}
