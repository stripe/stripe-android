package com.stripe.android.paymentsheet.injection

import com.stripe.android.paymentsheet.PaymentSheetViewModel
import dagger.Subcomponent

@Subcomponent(
    modules = [PaymentSheetViewModelModule::class]
)
internal interface PaymentSheetViewModelSubcomponent {
    val viewModel: PaymentSheetViewModel

    @Subcomponent.Builder
    interface Builder {
        fun paymentSheetViewModelModule(
            paymentSheetViewModelModule: PaymentSheetViewModelModule
        ): Builder

        fun build(): PaymentSheetViewModelSubcomponent
    }
}
