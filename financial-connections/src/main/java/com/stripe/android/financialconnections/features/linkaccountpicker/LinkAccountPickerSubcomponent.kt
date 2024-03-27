package com.stripe.android.financialconnections.features.linkaccountpicker

import dagger.BindsInstance
import dagger.Subcomponent

@Subcomponent
internal interface LinkAccountPickerSubcomponent {

    val viewModel: LinkAccountPickerViewModel

    @Subcomponent.Factory
    interface Factory {
        fun create(@BindsInstance initialState: LinkAccountPickerState): LinkAccountPickerSubcomponent
    }
}
