package com.stripe.android.paymentsheet.paymentdatacollection.ach.di

import androidx.lifecycle.SavedStateHandle
import com.stripe.android.paymentsheet.paymentdatacollection.ach.USBankAccountFormViewModel
import com.stripe.android.uicore.elements.AutocompleteAddressInteractor
import dagger.BindsInstance
import dagger.Subcomponent

@Subcomponent
internal interface USBankAccountFormViewModelSubcomponent {
    val viewModel: USBankAccountFormViewModel

    @Subcomponent.Factory
    interface Factory {
        fun create(
            @BindsInstance
            savedStateHandle: SavedStateHandle,
            @BindsInstance
            configuration: USBankAccountFormViewModel.Args,
            @BindsInstance
            autocompleteAddressInteractorFactory: AutocompleteAddressInteractor.Factory?,
        ): USBankAccountFormViewModelSubcomponent
    }
}
