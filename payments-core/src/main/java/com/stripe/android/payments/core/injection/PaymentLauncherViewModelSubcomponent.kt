package com.stripe.android.payments.core.injection

import androidx.lifecycle.SavedStateHandle
import com.stripe.android.payments.paymentlauncher.PaymentLauncherViewModel
import dagger.BindsInstance
import dagger.Subcomponent
import javax.inject.Named

@Subcomponent
internal interface PaymentLauncherViewModelSubcomponent {
    val viewModel: PaymentLauncherViewModel

    @Subcomponent.Builder
    interface Builder {

        @BindsInstance
        fun isPaymentIntent(@Named(IS_PAYMENT_INTENT) isPaymentIntent: Boolean): Builder

        @BindsInstance
        fun savedStateHandle(handle: SavedStateHandle): Builder

        fun build(): PaymentLauncherViewModelSubcomponent
    }
}
