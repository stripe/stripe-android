package com.stripe.android.financialconnections.features.accountpicker

import com.stripe.android.financialconnections.presentation.TopAppBarHost
import dagger.BindsInstance
import dagger.Subcomponent

@Subcomponent
internal interface AccountPickerSubcomponent {

    val viewModel: AccountPickerViewModel

    @Subcomponent.Builder
    interface Builder {

        @BindsInstance
        fun initialState(initialState: AccountPickerState): Builder

        @BindsInstance
        fun topAppBarHost(host: TopAppBarHost): Builder

        fun build(): AccountPickerSubcomponent
    }
}
