package com.stripe.android.financialconnections.features.manualentry

import dagger.BindsInstance
import dagger.Subcomponent

@Subcomponent
internal interface ManualEntrySubcomponent {

    val viewModel: ManualEntryViewModel

    @Subcomponent.Builder
    interface Builder {

        @BindsInstance
        fun initialState(initialState: ManualEntryState): Builder

        fun build(): ManualEntrySubcomponent
    }
}
