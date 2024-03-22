package com.stripe.android.financialconnections.features.success

import dagger.BindsInstance
import dagger.Subcomponent

@Subcomponent
internal interface SuccessSubcomponent {

    val viewModel: SuccessViewModel

    @Subcomponent.Builder
    interface Builder {

        @BindsInstance
        fun initialState(initialState: SuccessState): Builder

        fun build(): SuccessSubcomponent
    }
}
