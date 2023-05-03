package com.stripe.android.financialconnections.features.linkaccountpicker

import dagger.BindsInstance
import dagger.Subcomponent

@Subcomponent
internal interface LinkAccountPickerSubcomponent {

    val viewModel: LinkAccountPickerViewModel

    @Subcomponent.Builder
    interface Builder {

        @BindsInstance
        fun initialState(initialState: LinkAccountPickerState): Builder

        fun build(): LinkAccountPickerSubcomponent
    }
}
