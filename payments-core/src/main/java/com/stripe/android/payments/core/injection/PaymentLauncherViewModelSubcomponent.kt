package com.stripe.android.payments.core.injection

import androidx.lifecycle.SavedStateHandle
import com.stripe.android.payments.paymentlauncher.PaymentLauncherViewModel
import dagger.BindsInstance
import dagger.Subcomponent
import javax.inject.Named

@Subcomponent
internal interface PaymentLauncherViewModelSubcomponent {
    val viewModel: PaymentLauncherViewModel

    @Subcomponent.Factory
    interface Factory {
        fun create(
            @BindsInstance
            @Named(IS_PAYMENT_INTENT)
            isPaymentIntent: Boolean,
            @BindsInstance
            handle: SavedStateHandle,
        ): PaymentLauncherViewModelSubcomponent
    }
}
