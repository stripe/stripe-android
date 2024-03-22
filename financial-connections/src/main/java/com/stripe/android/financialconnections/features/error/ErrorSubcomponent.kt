package com.stripe.android.financialconnections.features.error

import dagger.BindsInstance
import dagger.Subcomponent

@Subcomponent
internal interface ErrorSubcomponent {

    val viewModel: ErrorViewModel

    @Subcomponent.Factory
    interface Factory {
        fun create(
            @BindsInstance initialState: ErrorState,
        ): ErrorSubcomponent
    }
}
