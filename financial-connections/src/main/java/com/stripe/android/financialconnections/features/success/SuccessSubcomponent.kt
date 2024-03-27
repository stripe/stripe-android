package com.stripe.android.financialconnections.features.success

import dagger.BindsInstance
import dagger.Subcomponent

@Subcomponent
internal interface SuccessSubcomponent {

    val viewModel: SuccessViewModel

    @Subcomponent.Factory
    interface Factory {
        fun create(@BindsInstance initialState: SuccessState): SuccessSubcomponent
    }
}
