package com.stripe.android.financialconnections.features.reset

import dagger.BindsInstance
import dagger.Subcomponent

@Subcomponent
internal interface ResetSubcomponent {

    val viewModel: ResetViewModel

    @Subcomponent.Builder
    interface Builder {

        @BindsInstance
        fun initialState(initialState: ResetState): Builder

        fun build(): ResetSubcomponent
    }
}
