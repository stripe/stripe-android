package com.stripe.android.financialconnections.features.consent

import dagger.BindsInstance
import dagger.Subcomponent

@Subcomponent
internal interface ConsentSubcomponent {

    val viewModel: ConsentViewModel

    @Subcomponent.Factory
    interface Factory {
        fun create(@BindsInstance state: ConsentState): ConsentSubcomponent
    }
}
