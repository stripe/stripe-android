package com.stripe.android.financialconnections.features.networkinglinksignup

import dagger.BindsInstance
import dagger.Subcomponent

@Subcomponent
internal interface NetworkingLinkSignupSubcomponent {

    val viewModel: NetworkingLinkSignupViewModel

    @Subcomponent.Builder
    interface Builder {

        @BindsInstance
        fun initialState(initialState: NetworkingLinkSignupState): Builder

        fun build(): NetworkingLinkSignupSubcomponent
    }
}
