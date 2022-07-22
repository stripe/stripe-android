package com.stripe.android.financialconnections.features.partnerauth

import dagger.BindsInstance
import dagger.Subcomponent

@Subcomponent
internal interface PartnerAuthSubcomponent {

    val viewModel: PartnerAuthViewModel

    @Subcomponent.Builder
    interface Builder {

        @BindsInstance
        fun initialState(initialState: PartnerAuthState): Builder

        fun build(): PartnerAuthSubcomponent
    }
}
