package com.stripe.android.paymentsheet.paymentdatacollection.ach.di

import androidx.lifecycle.SavedStateHandle
import com.stripe.android.paymentsheet.paymentdatacollection.ach.USBankAccountFormViewModel
import com.stripe.android.uicore.elements.AutocompleteAddressInteractor
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

        @BindsInstance
        fun autocompleteAddressInteractorFactory(interactorFactory: AutocompleteAddressInteractor.Factory?): Builder

        fun build(): USBankAccountFormViewModelSubcomponent
    }
}
