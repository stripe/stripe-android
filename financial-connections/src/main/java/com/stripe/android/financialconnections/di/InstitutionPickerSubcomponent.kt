package com.stripe.android.financialconnections.di

import com.stripe.android.financialconnections.presentation.InstitutionPickerState
import com.stripe.android.financialconnections.presentation.InstitutionPickerViewModel
import dagger.BindsInstance
import dagger.Subcomponent

@Subcomponent
internal interface InstitutionPickerSubcomponent {

    val viewModel: InstitutionPickerViewModel

    @Subcomponent.Builder
    interface Builder {

        @BindsInstance
        fun initialState(initialState: InstitutionPickerState): Builder

        fun build(): InstitutionPickerSubcomponent
    }
}
