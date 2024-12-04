package com.stripe.android.paymentsheet.injection

import android.app.Application
import androidx.lifecycle.SavedStateHandle
import com.stripe.android.paymentelement.confirmation.ConfirmationModule
import com.stripe.android.paymentsheet.PaymentOptionContract
import com.stripe.android.paymentsheet.PaymentOptionsViewModel
import dagger.BindsInstance
import dagger.Subcomponent

@Subcomponent(
    modules = [ConfirmationModule::class]
)
internal interface PaymentOptionsViewModelSubcomponent {
    val viewModel: PaymentOptionsViewModel

    @Subcomponent.Builder
    interface Builder {
        @BindsInstance
        fun application(application: Application): Builder

        @BindsInstance
        fun savedStateHandle(handle: SavedStateHandle): Builder

        @BindsInstance
        fun args(args: PaymentOptionContract.Args): Builder

        fun build(): PaymentOptionsViewModelSubcomponent
    }
}
