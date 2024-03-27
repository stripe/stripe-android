package com.stripe.android.financialconnections.features.networkinglinksignup

import dagger.BindsInstance
import dagger.Subcomponent

@Subcomponent
internal interface NetworkingLinkSignupSubcomponent {

    val viewModel: NetworkingLinkSignupViewModel

    @Subcomponent.Factory
    interface Factory {
        fun create(
            @BindsInstance initialState: NetworkingLinkSignupState,
        ): NetworkingLinkSignupSubcomponent
    }
}
