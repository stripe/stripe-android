package com.stripe.android.financialconnections.features.attachpayment

import com.stripe.android.financialconnections.presentation.TopAppBarHost
import dagger.BindsInstance
import dagger.Subcomponent

@Subcomponent
internal interface AttachPaymentSubcomponent {

    val viewModel: AttachPaymentViewModel

    @Subcomponent.Builder
    interface Builder {

        @BindsInstance
        fun initialState(initialState: AttachPaymentState): Builder

        @BindsInstance
        fun topAppBarHost(host: TopAppBarHost): Builder

        fun build(): AttachPaymentSubcomponent
    }
}
