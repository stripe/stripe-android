package com.stripe.android.financialconnections.features.accountupdate

import dagger.BindsInstance
import dagger.Subcomponent

@Subcomponent
internal interface AccountUpdateRequiredSubcomponent {

    val viewModel: AccountUpdateRequiredViewModel

    @Subcomponent.Factory
    interface Factory {
        fun create(
            @BindsInstance initialState: AccountUpdateRequiredState,
        ): AccountUpdateRequiredSubcomponent
    }
}
