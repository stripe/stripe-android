package com.stripe.android.paymentsheet.paymentdatacollection.polling.di

import androidx.lifecycle.SavedStateHandle
import com.stripe.android.paymentsheet.paymentdatacollection.polling.PollingViewModel
import dagger.BindsInstance
import dagger.Subcomponent

@Subcomponent
internal interface PollingViewModelSubcomponent {

    val viewModel: PollingViewModel

    @Subcomponent.Factory
    interface Factory {
        fun create(
            @BindsInstance
            savedStateHandle: SavedStateHandle,
            @BindsInstance
            args: PollingViewModel.Args,
        ): PollingViewModelSubcomponent
    }
}
