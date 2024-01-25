package com.stripe.android.paymentsheet.injection

import com.stripe.android.paymentsheet.forms.FormViewModel
import com.stripe.android.paymentsheet.paymentdatacollection.FormArguments
import dagger.BindsInstance
import dagger.Subcomponent
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Named

@Subcomponent
internal interface FormViewModelSubcomponent {
    val viewModel: FormViewModel

    @Subcomponent.Builder
    interface Builder {

        @BindsInstance
        fun formArguments(args: FormArguments): Builder

        @BindsInstance
        fun showCheckboxFlow(
            @Named(ShowCheckboxFlow) saveForFutureUseVisibleFlow: StateFlow<Boolean>,
        ): Builder

        @BindsInstance
        fun processingWithLinkFlow(
            @Named(ProcessingWithLinkFlow) processingWithLinkFlow: StateFlow<Boolean>,
        ): Builder

        fun build(): FormViewModelSubcomponent
    }
}
