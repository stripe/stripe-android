package com.stripe.android.payments.core.injection

import androidx.activity.result.ActivityResultCaller
import androidx.lifecycle.SavedStateHandle
import com.stripe.android.payments.paymentlauncher.PaymentLauncherViewModel
import com.stripe.android.view.AuthActivityStarterHost
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

        @BindsInstance
        fun authActivityStarterHost(authActivityStarterHost: AuthActivityStarterHost): Builder

        @BindsInstance
        fun activityResultCaller(activityResultCaller: ActivityResultCaller): Builder

        fun build(): PaymentLauncherViewModelSubcomponent
    }
}
