package com.stripe.android.paymentsheet.injection

import android.app.Application
import com.stripe.android.paymentsheet.addresselement.AutocompleteViewModel
import dagger.BindsInstance
import dagger.Subcomponent

@Subcomponent
internal interface AutocompleteViewModelSubcomponent {
    val autoCompleteViewModel: AutocompleteViewModel

    @Subcomponent.Factory
    interface Factory {
        fun create(
            @BindsInstance
            application: Application,
            @BindsInstance
            configuration: AutocompleteViewModel.Args,
        ): AutocompleteViewModelSubcomponent
    }
}
