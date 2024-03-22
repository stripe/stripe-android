package com.stripe.android.financialconnections.features.exit

import dagger.BindsInstance
import dagger.Subcomponent

@Subcomponent
internal interface ExitSubcomponent {

    val viewModel: ExitViewModel

    @Subcomponent.Factory
    interface Factory {
        fun create(
            @BindsInstance initialState: ExitState,
        ): ExitSubcomponent
    }
}
