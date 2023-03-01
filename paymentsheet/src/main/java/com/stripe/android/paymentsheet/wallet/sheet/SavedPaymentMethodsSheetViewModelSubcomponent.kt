package com.stripe.android.paymentsheet.wallet.sheet

import android.app.Application
import androidx.lifecycle.SavedStateHandle
import dagger.BindsInstance
import dagger.Subcomponent

@Subcomponent
internal interface SavedPaymentMethodsSheetViewModelSubcomponent {
    val viewModel: SavedPaymentMethodsSheetViewModel

    @Subcomponent.Builder
    interface Builder {
        @BindsInstance
        fun application(application: Application): Builder

        @BindsInstance
        fun savedStateHandle(handle: SavedStateHandle): Builder

        @BindsInstance
        fun args(args: SavedPaymentMethodsSheetContract.Args): Builder

        fun build(): SavedPaymentMethodsSheetViewModelSubcomponent
    }
}
