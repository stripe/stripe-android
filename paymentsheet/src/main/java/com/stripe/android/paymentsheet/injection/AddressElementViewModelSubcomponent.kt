package com.stripe.android.paymentsheet.injection

import android.app.Application
import androidx.lifecycle.SavedStateHandle
import com.stripe.android.paymentsheet.addresselement.AddressElementActivityContract
import com.stripe.android.paymentsheet.addresselement.AddressElementViewModel
import dagger.BindsInstance
import dagger.Subcomponent

@Subcomponent
internal interface AddressElementViewModelSubcomponent {
    val viewModel: AddressElementViewModel

    @Subcomponent.Factory
    interface Factory {
        fun create(
            @BindsInstance
            application: Application,
            @BindsInstance
            savedStateHandle: SavedStateHandle,
            @BindsInstance
            args: AddressElementActivityContract.Args,
        ): AddressElementViewModelSubcomponent
    }
}
