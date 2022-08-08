package com.stripe.android.paymentsheet.injection

import android.app.Application
import com.stripe.android.paymentsheet.addresselement.AutocompleteViewModel
import dagger.BindsInstance
import dagger.Subcomponent

@Subcomponent
internal interface AutocompleteViewModelSubcomponent {
    val autoCompleteViewModel: AutocompleteViewModel

    @Subcomponent.Builder
    interface Builder {
        @BindsInstance
        fun application(application: Application): Builder

        @BindsInstance
        fun configuration(configuration: AutocompleteViewModel.Args): Builder

        fun build(): AutocompleteViewModelSubcomponent
    }
}
