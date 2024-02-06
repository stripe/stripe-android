package com.stripe.android.financialconnections.features.error

import dagger.BindsInstance
import dagger.Subcomponent

@Subcomponent
internal interface ErrorSubcomponent {

    val viewModel: ErrorViewModel

    @Subcomponent.Builder
    interface Builder {

        @BindsInstance
        fun initialState(initialState: ErrorState): Builder

        fun build(): ErrorSubcomponent
    }
}
