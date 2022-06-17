package com.stripe.android.paymentsheet.injection

import com.stripe.android.paymentsheet.addresselement.AutoCompleteViewModel
import dagger.Subcomponent

@Subcomponent
internal interface AutoCompleteViewModelSubcomponent {
    val autoCompleteViewModel: AutoCompleteViewModel

    @Subcomponent.Builder
    interface Builder {

        fun build(): AutoCompleteViewModelSubcomponent
    }
}