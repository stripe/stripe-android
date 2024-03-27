package com.stripe.android.financialconnections.features.reset

import dagger.BindsInstance
import dagger.Subcomponent

@Subcomponent
internal interface ResetSubcomponent {

    val viewModel: ResetViewModel

    @Subcomponent.Factory
    interface Factory {
        fun create(@BindsInstance initialState: ResetState): ResetSubcomponent
    }
}
