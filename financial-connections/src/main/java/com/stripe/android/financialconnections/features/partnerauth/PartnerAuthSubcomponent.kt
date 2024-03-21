package com.stripe.android.financialconnections.features.partnerauth

import com.stripe.android.financialconnections.navigation.topappbar.TopAppBarHost
import dagger.BindsInstance
import dagger.Subcomponent

@Subcomponent
internal interface PartnerAuthSubcomponent {

    val viewModel: PartnerAuthViewModel

    @Subcomponent.Builder
    interface Builder {

        @BindsInstance
        fun initialState(initialState: SharedPartnerAuthState): Builder

        @BindsInstance
        fun topAppBarHost(host: TopAppBarHost): Builder

        fun build(): PartnerAuthSubcomponent
    }
}
