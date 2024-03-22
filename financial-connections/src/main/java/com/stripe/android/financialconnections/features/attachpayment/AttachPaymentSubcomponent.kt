package com.stripe.android.financialconnections.features.attachpayment

import dagger.BindsInstance
import dagger.Subcomponent

@Subcomponent
internal interface AttachPaymentSubcomponent {

    val viewModel: AttachPaymentViewModel

    @Subcomponent.Builder
    interface Builder {

        @BindsInstance
        fun initialState(initialState: AttachPaymentState): Builder

        fun build(): AttachPaymentSubcomponent
    }
}
