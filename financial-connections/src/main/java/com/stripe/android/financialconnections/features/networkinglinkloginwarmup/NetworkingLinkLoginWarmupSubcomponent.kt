package com.stripe.android.financialconnections.features.networkinglinkloginwarmup

import dagger.BindsInstance
import dagger.Subcomponent

@Subcomponent
internal interface NetworkingLinkLoginWarmupSubcomponent {

    val viewModel: NetworkingLinkLoginWarmupViewModel

    @Subcomponent.Builder
    interface Builder {

        @BindsInstance
        fun initialState(initialState: NetworkingLinkLoginWarmupState): Builder

        fun build(): NetworkingLinkLoginWarmupSubcomponent
    }
}
