package com.stripe.android.financialconnections.features.manualentrysuccess

import dagger.BindsInstance
import dagger.Subcomponent

@Subcomponent
internal interface ManualEntrySuccessSubcomponent {

    val viewModel: ManualEntrySuccessViewModel

    @Subcomponent.Factory
    interface Factory {
        fun create(
            @BindsInstance initialState: ManualEntrySuccessState,
        ): ManualEntrySuccessSubcomponent
    }
}
