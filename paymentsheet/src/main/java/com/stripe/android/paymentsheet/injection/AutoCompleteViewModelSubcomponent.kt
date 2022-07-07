package com.stripe.android.paymentsheet.injection

import android.app.Application
import com.stripe.android.paymentsheet.addresselement.AutocompleteViewModel
import dagger.BindsInstance
import dagger.Subcomponent

@Subcomponent
internal interface AutoCompleteViewModelSubcomponent {
    val autoCompleteViewModel: AutocompleteViewModel

    @Subcomponent.Builder
    interface Builder {
        @BindsInstance
        fun application(application: Application): Builder

        fun build(): AutoCompleteViewModelSubcomponent
    }
}
