package com.stripe.android.financialconnections.features.networkinglinkverification

import com.stripe.android.financialconnections.navigation.topappbar.TopAppBarHost
import dagger.BindsInstance
import dagger.Subcomponent

@Subcomponent
internal interface NetworkingLinkVerificationSubcomponent {

    val viewModel: NetworkingLinkVerificationViewModel

    @Subcomponent.Builder
    interface Builder {

        @BindsInstance
        fun initialState(initialState: NetworkingLinkVerificationState): Builder

        @BindsInstance
        fun topAppBarHost(host: TopAppBarHost): Builder

        fun build(): NetworkingLinkVerificationSubcomponent
    }
}
