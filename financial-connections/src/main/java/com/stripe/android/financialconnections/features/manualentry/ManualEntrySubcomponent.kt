package com.stripe.android.financialconnections.features.manualentry

import com.stripe.android.financialconnections.navigation.TopAppBarHost
import dagger.BindsInstance
import dagger.Subcomponent

@Subcomponent
internal interface ManualEntrySubcomponent {

    val viewModel: ManualEntryViewModel

    @Subcomponent.Builder
    interface Builder {

        @BindsInstance
        fun initialState(initialState: ManualEntryState): Builder

        @BindsInstance
        fun topAppBarHost(host: TopAppBarHost): Builder

        fun build(): ManualEntrySubcomponent
    }
}
