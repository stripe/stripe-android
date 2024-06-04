package com.stripe.android.paymentsheet.paymentdatacollection.polling.di

import androidx.lifecycle.SavedStateHandle
import com.stripe.android.paymentsheet.paymentdatacollection.polling.PollingViewModel
import dagger.BindsInstance
import dagger.Subcomponent

@Subcomponent
internal interface PollingViewModelSubcomponent {

    val viewModel: PollingViewModel

    @Subcomponent.Builder
    interface Builder {
        @BindsInstance
        fun savedStateHandle(handle: SavedStateHandle): Builder

        @BindsInstance
        fun args(args: PollingViewModel.Args): Builder

        fun build(): PollingViewModelSubcomponent
    }
}
