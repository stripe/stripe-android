package com.stripe.android.financialconnections.features.bankauthrepair

import com.stripe.android.financialconnections.features.partnerauth.SharedPartnerAuthState
import dagger.BindsInstance
import dagger.Subcomponent

@Subcomponent
internal interface BankAuthRepairSubcomponent {

    val viewModel: BankAuthRepairViewModel

    @Subcomponent.Builder
    interface Builder {

        @BindsInstance
        fun initialState(initialState: SharedPartnerAuthState): Builder

        fun build(): BankAuthRepairSubcomponent
    }
}
