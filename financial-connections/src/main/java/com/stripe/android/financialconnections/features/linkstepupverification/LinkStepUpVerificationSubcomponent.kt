package com.stripe.android.financialconnections.features.linkstepupverification

import dagger.BindsInstance
import dagger.Subcomponent

@Subcomponent
internal interface LinkStepUpVerificationSubcomponent {

    val viewModel: LinkStepUpVerificationViewModel

    @Subcomponent.Factory
    interface Factory {
        fun create(
            @BindsInstance initialState: LinkStepUpVerificationState,
        ): LinkStepUpVerificationSubcomponent
    }
}
