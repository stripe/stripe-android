package com.stripe.android.financialconnections.features.networkinglinkverification

import dagger.BindsInstance
import dagger.Subcomponent

@Subcomponent
internal interface NetworkingLinkVerificationSubcomponent {

    val viewModel: NetworkingLinkVerificationViewModel

    @Subcomponent.Factory
    interface Factory {
        fun create(
            @BindsInstance initialState: NetworkingLinkVerificationState,
        ): NetworkingLinkVerificationSubcomponent
    }
}
