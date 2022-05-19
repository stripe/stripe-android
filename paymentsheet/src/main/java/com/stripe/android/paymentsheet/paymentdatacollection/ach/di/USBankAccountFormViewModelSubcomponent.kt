package com.stripe.android.paymentsheet.paymentdatacollection.ach.di

import androidx.lifecycle.SavedStateHandle
import com.stripe.android.paymentsheet.paymentdatacollection.ach.USBankAccountFormViewModel
import dagger.BindsInstance
import dagger.Subcomponent

@Subcomponent
internal interface USBankAccountFormViewModelSubcomponent {
    val viewModel: USBankAccountFormViewModel

    @Subcomponent.Builder
    interface Builder {
        @BindsInstance
        fun savedStateHandle(handle: SavedStateHandle): Builder

        @BindsInstance
        fun configuration(configuration: USBankAccountFormViewModel.Args): Builder

        fun build(): USBankAccountFormViewModelSubcomponent
    }
}
