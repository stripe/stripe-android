package com.stripe.android.paymentsheet.injection

import com.stripe.android.paymentsheet.forms.FormViewModel
import com.stripe.android.paymentsheet.paymentdatacollection.FormArguments
import dagger.BindsInstance
import dagger.Subcomponent
import kotlinx.coroutines.flow.Flow
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
            @Named(ShowCheckboxFlow) saveForFutureUseVisibleFlow: Flow<Boolean>,
        ): Builder

        @BindsInstance
        fun processingWithLinkFlow(
            @Named(ProcessingWithLinkFlow) processingWithLinkFlow: Flow<Boolean>,
        ): Builder

        fun build(): FormViewModelSubcomponent
    }
}
