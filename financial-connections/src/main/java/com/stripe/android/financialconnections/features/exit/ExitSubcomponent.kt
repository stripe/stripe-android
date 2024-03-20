package com.stripe.android.financialconnections.features.exit

import com.stripe.android.financialconnections.navigation.TopAppBarHost
import dagger.BindsInstance
import dagger.Subcomponent

@Subcomponent
internal interface ExitSubcomponent {

    val viewModel: ExitViewModel

    @Subcomponent.Factory
    interface Factory {
        fun create(
            @BindsInstance initialState: ExitState,
            @BindsInstance topAppBarHost: TopAppBarHost,
        ): ExitSubcomponent
    }
}
