package com.stripe.android.financialconnections.features.consent

import com.stripe.android.financialconnections.presentation.TopAppBarHost
import dagger.BindsInstance
import dagger.Subcomponent

@Subcomponent
internal interface ConsentSubcomponent {

    val viewModel: ConsentViewModel

    @Subcomponent.Builder
    interface Builder {

        @BindsInstance
        fun initialState(state: ConsentState): Builder

        @BindsInstance
        fun topAppBarHost(host: TopAppBarHost): Builder

        fun build(): ConsentSubcomponent
    }
}
