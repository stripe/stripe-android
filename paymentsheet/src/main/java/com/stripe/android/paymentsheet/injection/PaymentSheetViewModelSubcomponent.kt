package com.stripe.android.paymentsheet.injection

import androidx.lifecycle.SavedStateHandle
import com.stripe.android.paymentelement.confirmation.ConfirmationModule
import com.stripe.android.paymentsheet.PaymentSheetViewModel
import dagger.BindsInstance
import dagger.Subcomponent

@Subcomponent(
    modules = [PaymentSheetViewModelModule::class, ConfirmationModule::class]
)
internal interface PaymentSheetViewModelSubcomponent {
    val viewModel: PaymentSheetViewModel

    @Subcomponent.Builder
    interface Builder {
        fun paymentSheetViewModelModule(
            paymentSheetViewModelModule: PaymentSheetViewModelModule
        ): Builder

        @BindsInstance
        fun savedStateHandle(handle: SavedStateHandle): Builder

        fun build(): PaymentSheetViewModelSubcomponent
    }
}
