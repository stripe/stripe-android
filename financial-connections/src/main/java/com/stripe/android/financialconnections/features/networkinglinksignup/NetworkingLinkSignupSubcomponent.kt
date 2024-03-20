package com.stripe.android.financialconnections.features.networkinglinksignup

import com.stripe.android.financialconnections.navigation.TopAppBarHost
import dagger.BindsInstance
import dagger.Subcomponent

@Subcomponent
internal interface NetworkingLinkSignupSubcomponent {

    val viewModel: NetworkingLinkSignupViewModel

    @Subcomponent.Builder
    interface Builder {

        @BindsInstance
        fun initialState(initialState: NetworkingLinkSignupState): Builder

        @BindsInstance
        fun topAppBarHost(host: TopAppBarHost): Builder

        fun build(): NetworkingLinkSignupSubcomponent
    }
}
