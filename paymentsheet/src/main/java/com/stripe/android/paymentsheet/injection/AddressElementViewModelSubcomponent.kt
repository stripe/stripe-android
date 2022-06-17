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

    @Subcomponent.Builder
    interface Builder {
        @BindsInstance
        fun application(application: Application): Builder

        @BindsInstance
        fun savedStateHandle(handle: SavedStateHandle): Builder

        @BindsInstance
        fun args(args: AddressElementActivityContract.Args): Builder

        fun build(): AddressElementViewModelSubcomponent
    }
}
