package com.stripe.android.financialconnections.features.bankauthrepair

import com.stripe.android.financialconnections.features.partnerauth.PartnerAuthState
import dagger.BindsInstance
import dagger.Subcomponent

@Subcomponent
internal interface BankAuthRepairSubcomponent {

    val viewModel: BankAuthRepairViewModel

    @Subcomponent.Builder
    interface Builder {

        @BindsInstance
        fun initialState(initialState: PartnerAuthState): Builder

        fun build(): BankAuthRepairSubcomponent
    }
}
