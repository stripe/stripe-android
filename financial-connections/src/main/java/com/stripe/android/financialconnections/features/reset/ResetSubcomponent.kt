package com.stripe.android.financialconnections.features.reset

import com.stripe.android.financialconnections.navigation.topappbar.TopAppBarHost
import dagger.BindsInstance
import dagger.Subcomponent

@Subcomponent
internal interface ResetSubcomponent {

    val viewModel: ResetViewModel

    @Subcomponent.Builder
    interface Builder {

        @BindsInstance
        fun initialState(initialState: ResetState): Builder

        @BindsInstance
        fun topAppBarHost(topAppBarHost: TopAppBarHost): Builder

        fun build(): ResetSubcomponent
    }
}
