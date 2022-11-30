package com.stripe.android.financialconnections.features.manualentrysuccess

import dagger.BindsInstance
import dagger.Subcomponent

@Subcomponent
internal interface ManualEntrySuccessSubcomponent {

    val viewModel: ManualEntrySuccessViewModel

    @Subcomponent.Builder
    interface Builder {

        @BindsInstance
        fun initialState(initialState: ManualEntrySuccessState): Builder

        fun build(): ManualEntrySuccessSubcomponent
    }
}
