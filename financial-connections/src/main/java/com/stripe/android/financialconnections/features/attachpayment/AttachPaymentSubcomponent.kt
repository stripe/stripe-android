package com.stripe.android.financialconnections.features.attachpayment

import dagger.BindsInstance
import dagger.Subcomponent

@Subcomponent
internal interface AttachPaymentSubcomponent {

    val viewModel: AttachPaymentViewModel

    @Subcomponent.Factory
    interface Factory {
        fun create(@BindsInstance initialState: AttachPaymentState): AttachPaymentSubcomponent
    }
}
