package com.stripe.android.financialconnections.features.manualentrysuccess

import com.stripe.android.financialconnections.navigation.TopAppBarHost
import dagger.BindsInstance
import dagger.Subcomponent

@Subcomponent
internal interface ManualEntrySuccessSubcomponent {

    val viewModel: ManualEntrySuccessViewModel

    @Subcomponent.Builder
    interface Builder {

        @BindsInstance
        fun initialState(initialState: ManualEntrySuccessState): Builder

        @BindsInstance
        fun topAppBarHost(host: TopAppBarHost): Builder

        fun build(): ManualEntrySuccessSubcomponent
    }
}
