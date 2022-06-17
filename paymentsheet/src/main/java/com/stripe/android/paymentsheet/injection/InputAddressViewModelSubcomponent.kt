package com.stripe.android.paymentsheet.injection

import com.stripe.android.paymentsheet.addresselement.InputAddressViewModel
import dagger.Subcomponent

@Subcomponent
internal interface InputAddressViewModelSubcomponent {
    val inputAddressViewModel: InputAddressViewModel

    @Subcomponent.Builder
    interface Builder {

        fun build(): InputAddressViewModelSubcomponent
    }
}
