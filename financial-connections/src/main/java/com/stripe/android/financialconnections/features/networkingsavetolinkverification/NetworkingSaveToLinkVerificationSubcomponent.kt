package com.stripe.android.financialconnections.features.networkingsavetolinkverification

import dagger.BindsInstance
import dagger.Subcomponent

@Subcomponent
internal interface NetworkingSaveToLinkVerificationSubcomponent {

    val viewModel: NetworkingSaveToLinkVerificationViewModel

    @Subcomponent.Builder
    interface Builder {

        @BindsInstance
        fun initialState(initialState: NetworkingSaveToLinkVerificationState): Builder

        fun build(): NetworkingSaveToLinkVerificationSubcomponent
    }
}
