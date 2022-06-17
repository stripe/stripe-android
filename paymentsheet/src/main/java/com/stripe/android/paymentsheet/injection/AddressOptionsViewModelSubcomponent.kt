package com.stripe.android.paymentsheet.injection

import android.app.Application
import androidx.lifecycle.SavedStateHandle
import com.stripe.android.paymentsheet.AddressOptionsActivityContract
import com.stripe.android.paymentsheet.AddressOptionsViewModel
import dagger.BindsInstance
import dagger.Subcomponent

@Subcomponent
internal interface AddressOptionsViewModelSubcomponent {
    val viewModel: AddressOptionsViewModel

    @Subcomponent.Builder
    interface Builder {
        @BindsInstance
        fun application(application: Application): Builder

        @BindsInstance
        fun savedStateHandle(handle: SavedStateHandle): Builder

        @BindsInstance
        fun args(args: AddressOptionsActivityContract.Args): Builder

        fun build(): AddressOptionsViewModelSubcomponent
    }
}