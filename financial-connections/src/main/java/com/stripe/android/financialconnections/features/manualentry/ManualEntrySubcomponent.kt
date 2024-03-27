package com.stripe.android.financialconnections.features.manualentry

import dagger.BindsInstance
import dagger.Subcomponent

@Subcomponent
internal interface ManualEntrySubcomponent {

    val viewModel: ManualEntryViewModel

    @Subcomponent.Factory
    interface Factory {
        fun create(@BindsInstance initialState: ManualEntryState): ManualEntrySubcomponent
    }
}
