package com.stripe.android.financialconnections.features.accountpicker

import dagger.BindsInstance
import dagger.Subcomponent

@Subcomponent
internal interface AccountPickerSubcomponent {

    val viewModel: AccountPickerViewModel

    @Subcomponent.Factory
    interface Factory {
        fun create(@BindsInstance initialState: AccountPickerState): AccountPickerSubcomponent
    }
}
