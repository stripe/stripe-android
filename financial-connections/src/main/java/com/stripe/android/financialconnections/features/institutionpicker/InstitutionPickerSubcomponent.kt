package com.stripe.android.financialconnections.features.institutionpicker

import dagger.BindsInstance
import dagger.Subcomponent

@Subcomponent
internal interface InstitutionPickerSubcomponent {

    val viewModel: InstitutionPickerViewModel

    @Subcomponent.Factory
    interface Factory {
        fun create(@BindsInstance initialState: InstitutionPickerState): InstitutionPickerSubcomponent
    }
}
