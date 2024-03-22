package com.stripe.android.financialconnections.features.networkinglinkverification

import dagger.BindsInstance
import dagger.Subcomponent

@Subcomponent
internal interface NetworkingLinkVerificationSubcomponent {

    val viewModel: NetworkingLinkVerificationViewModel

    @Subcomponent.Builder
    interface Builder {

        @BindsInstance
        fun initialState(initialState: NetworkingLinkVerificationState): Builder

        fun build(): NetworkingLinkVerificationSubcomponent
    }
}
