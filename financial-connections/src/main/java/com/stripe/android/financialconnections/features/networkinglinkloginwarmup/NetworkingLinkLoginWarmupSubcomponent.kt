package com.stripe.android.financialconnections.features.networkinglinkloginwarmup

import com.stripe.android.financialconnections.navigation.TopAppBarHost
import dagger.BindsInstance
import dagger.Subcomponent

@Subcomponent
internal interface NetworkingLinkLoginWarmupSubcomponent {

    val viewModel: NetworkingLinkLoginWarmupViewModel

    @Subcomponent.Builder
    interface Builder {

        @BindsInstance
        fun initialState(initialState: NetworkingLinkLoginWarmupState): Builder

        @BindsInstance
        fun topAppBarHost(host: TopAppBarHost): Builder

        fun build(): NetworkingLinkLoginWarmupSubcomponent
    }
}
