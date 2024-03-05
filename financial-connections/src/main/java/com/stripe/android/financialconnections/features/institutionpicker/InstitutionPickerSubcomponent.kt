package com.stripe.android.financialconnections.features.institutionpicker

import com.stripe.android.financialconnections.presentation.TopAppBarHost
import dagger.BindsInstance
import dagger.Subcomponent

@Subcomponent
internal interface InstitutionPickerSubcomponent {

    val viewModel: InstitutionPickerViewModel

    @Subcomponent.Builder
    interface Builder {

        @BindsInstance
        fun initialState(initialState: InstitutionPickerState): Builder

        @BindsInstance
        fun topAppBarHost(host: TopAppBarHost): Builder

        fun build(): InstitutionPickerSubcomponent
    }
}
