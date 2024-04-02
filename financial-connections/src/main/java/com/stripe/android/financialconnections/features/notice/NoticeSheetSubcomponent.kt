package com.stripe.android.financialconnections.features.notice

import dagger.BindsInstance
import dagger.Subcomponent

@Subcomponent
internal interface NoticeSheetSubcomponent {

    val viewModel: NoticeSheetViewModel

    @Subcomponent.Factory
    interface Factory {
        fun create(
            @BindsInstance initialState: NoticeSheetState,
        ): NoticeSheetSubcomponent
    }
}
