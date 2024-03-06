package com.stripe.android.financialconnections.features.reset

import com.stripe.android.financialconnections.presentation.TopAppBarHost
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
        fun topAppBarHost(host: TopAppBarHost): Builder

        fun build(): ResetSubcomponent
    }
}
