package com.stripe.android.paymentsheet.injection

import androidx.lifecycle.SavedStateHandle
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
        fun formFragmentArguments(
            config: FormFragmentArguments
        ): Builder

        @BindsInstance
        fun savedStateHandle(handle: SavedStateHandle): Builder

        fun build(): FormViewModelSubcomponent
    }
}
