package com.stripe.android.financialconnections.features.networkingsavetolinkverification

import dagger.BindsInstance
import dagger.Subcomponent

@Subcomponent
internal interface NetworkingSaveToLinkVerificationSubcomponent {

    val viewModel: NetworkingSaveToLinkVerificationViewModel

    @Subcomponent.Factory
    interface Factory {
        fun create(
            @BindsInstance initialState: NetworkingSaveToLinkVerificationState,
        ): NetworkingSaveToLinkVerificationSubcomponent
    }
}
