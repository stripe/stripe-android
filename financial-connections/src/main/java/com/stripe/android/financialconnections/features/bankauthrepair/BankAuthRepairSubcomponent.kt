package com.stripe.android.financialconnections.features.bankauthrepair

import com.stripe.android.financialconnections.features.partnerauth.SharedPartnerAuthState
import dagger.BindsInstance
import dagger.Subcomponent

@Subcomponent
internal interface BankAuthRepairSubcomponent {

    val viewModel: BankAuthRepairViewModel

    @Subcomponent.Factory
    interface Factory {
        fun create(@BindsInstance initialState: SharedPartnerAuthState): BankAuthRepairSubcomponent
    }
}
