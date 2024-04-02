package com.stripe.android.financialconnections.features.static_sheet

import dagger.BindsInstance
import dagger.Subcomponent

@Subcomponent
internal interface StaticSheetSubcomponent {

    val viewModel: StaticSheetViewModel

    @Subcomponent.Factory
    interface Factory {
        fun create(
            @BindsInstance initialState: StaticSheetState,
        ): StaticSheetSubcomponent
    }
}
