package com.stripe.android.financialconnections.features.consent

import dagger.BindsInstance
import dagger.Subcomponent

@Subcomponent
internal interface ConsentSubcomponent {

    val viewModel: ConsentViewModel

    @Subcomponent.Builder
    interface Builder {

        @BindsInstance
        fun initialState(state: ConsentState): Builder

        fun build(): ConsentSubcomponent
    }
}
