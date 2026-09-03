package com.stripe.android.paymentsheet.injection

import com.stripe.android.paymentsheet.addresselement.AddressElementActivityProcessingState
import com.stripe.android.paymentsheet.addresselement.InputAddressViewModel
import dagger.BindsInstance
import dagger.Subcomponent

@Subcomponent
internal interface InputAddressViewModelSubcomponent {
    val inputAddressViewModel: InputAddressViewModel

    @Subcomponent.Factory
    interface Factory {
        fun create(
            @BindsInstance
            processingState: AddressElementActivityProcessingState,
        ): InputAddressViewModelSubcomponent
    }
}
