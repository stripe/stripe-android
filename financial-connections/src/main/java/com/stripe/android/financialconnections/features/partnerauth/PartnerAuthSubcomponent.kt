package com.stripe.android.financialconnections.features.partnerauth

import dagger.BindsInstance
import dagger.Subcomponent

@Subcomponent
internal interface PartnerAuthSubcomponent {

    val viewModel: PartnerAuthViewModel

    @Subcomponent.Factory
    interface Factory {
        fun create(@BindsInstance initialState: SharedPartnerAuthState): PartnerAuthSubcomponent
    }
}
