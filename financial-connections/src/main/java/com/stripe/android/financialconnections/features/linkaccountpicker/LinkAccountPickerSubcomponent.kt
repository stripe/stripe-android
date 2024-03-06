package com.stripe.android.financialconnections.features.linkaccountpicker

import com.stripe.android.financialconnections.presentation.TopAppBarHost
import dagger.BindsInstance
import dagger.Subcomponent

@Subcomponent
internal interface LinkAccountPickerSubcomponent {

    val viewModel: LinkAccountPickerViewModel

    @Subcomponent.Builder
    interface Builder {

        @BindsInstance
        fun initialState(initialState: LinkAccountPickerState): Builder

        @BindsInstance
        fun topAppBarHost(host: TopAppBarHost): Builder

        fun build(): LinkAccountPickerSubcomponent
    }
}
