package com.stripe.android.financialconnections.features.institutionpicker

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
