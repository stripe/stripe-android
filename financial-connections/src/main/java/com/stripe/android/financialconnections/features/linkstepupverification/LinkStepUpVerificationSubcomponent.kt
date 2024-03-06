package com.stripe.android.financialconnections.features.linkstepupverification

import com.stripe.android.financialconnections.presentation.TopAppBarHost
import dagger.BindsInstance
import dagger.Subcomponent

@Subcomponent
internal interface LinkStepUpVerificationSubcomponent {

    val viewModel: LinkStepUpVerificationViewModel

    @Subcomponent.Builder
    interface Builder {

        @BindsInstance
        fun initialState(initialState: LinkStepUpVerificationState): Builder

        @BindsInstance
        fun topAppBarHost(host: TopAppBarHost): Builder

        fun build(): LinkStepUpVerificationSubcomponent
    }
}
