package com.stripe.android.paymentsheet.injection

import androidx.lifecycle.SavedStateHandle
import com.stripe.android.paymentsheet.PaymentSheetContractV2
import com.stripe.android.paymentsheet.PaymentSheetViewModel
import dagger.BindsInstance
import dagger.Subcomponent

@Subcomponent(
    modules = [
        PaymentSheetViewModelModule::class
    ]
)
internal interface PaymentSheetViewModelSubcomponent {
    val viewModel: PaymentSheetViewModel

    @Subcomponent.Builder
    interface Builder {

        @BindsInstance
        fun args(
            args: PaymentSheetContractV2.Args
        ): Builder

        @BindsInstance
        fun savedStateHandle(handle: SavedStateHandle): Builder

        fun build(): PaymentSheetViewModelSubcomponent
    }
}
