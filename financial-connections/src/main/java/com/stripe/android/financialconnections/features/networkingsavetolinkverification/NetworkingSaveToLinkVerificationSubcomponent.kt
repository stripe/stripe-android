package com.stripe.android.financialconnections.features.networkingsavetolinkverification

import com.stripe.android.financialconnections.navigation.topappbar.TopAppBarHost
import dagger.BindsInstance
import dagger.Subcomponent

@Subcomponent
internal interface NetworkingSaveToLinkVerificationSubcomponent {

    val viewModel: NetworkingSaveToLinkVerificationViewModel

    @Subcomponent.Builder
    interface Builder {

        @BindsInstance
        fun initialState(initialState: NetworkingSaveToLinkVerificationState): Builder

        @BindsInstance
        fun topAppBarHost(host: TopAppBarHost): Builder

        fun build(): NetworkingSaveToLinkVerificationSubcomponent
    }
}
