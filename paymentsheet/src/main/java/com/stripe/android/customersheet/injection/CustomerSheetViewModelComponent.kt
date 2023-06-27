package com.stripe.android.customersheet.injection

import com.stripe.android.customersheet.CustomerSheetViewModel
import dagger.Subcomponent

@Subcomponent
@CustomerSheetViewModelScope
internal interface CustomerSheetViewModelComponent {
    val viewModel: CustomerSheetViewModel

    @Subcomponent.Builder
    interface Builder {
        fun build(): CustomerSheetViewModelComponent
    }
}
