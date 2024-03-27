package com.stripe.android.financialconnections.features.networkinglinkloginwarmup

import dagger.BindsInstance
import dagger.Subcomponent

@Subcomponent
internal interface NetworkingLinkLoginWarmupSubcomponent {

    val viewModel: NetworkingLinkLoginWarmupViewModel

    @Subcomponent.Factory
    interface Factory {
        fun create(
            @BindsInstance initialState: NetworkingLinkLoginWarmupState,
        ): NetworkingLinkLoginWarmupSubcomponent
    }
}
