package com.stripe.android.financialconnections.features.linkstepupverification

import dagger.BindsInstance
import dagger.Subcomponent

@Subcomponent
internal interface LinkStepUpVerificationSubcomponent {

    val viewModel: LinkStepUpVerificationViewModel

    @Subcomponent.Builder
    interface Builder {

        @BindsInstance
        fun initialState(initialState: LinkStepUpVerificationState): Builder

        fun build(): LinkStepUpVerificationSubcomponent
    }
}
