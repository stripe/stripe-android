package com.stripe.android.financialconnections.features.accountpicker

import dagger.BindsInstance
import dagger.Subcomponent

@Subcomponent
internal interface AccountPickerSubcomponent {

    val viewModel: AccountPickerViewModel

    @Subcomponent.Builder
    interface Builder {

        @BindsInstance
        fun initialState(initialState: AccountPickerState): Builder

        fun build(): AccountPickerSubcomponent
    }
}
