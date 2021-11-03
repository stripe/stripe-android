package com.stripe.android.googlepaylauncher.injection

import androidx.lifecycle.SavedStateHandle
import com.stripe.android.googlepaylauncher.GooglePayPaymentMethodLauncherContract
import com.stripe.android.googlepaylauncher.GooglePayPaymentMethodLauncherViewModel
import dagger.BindsInstance
import dagger.Subcomponent

@Subcomponent
internal interface GooglePayPaymentMethodLauncherViewModelSubcomponent {
    val viewModel: GooglePayPaymentMethodLauncherViewModel

    @Subcomponent.Builder
    interface Builder {

        @BindsInstance
        fun args(args: GooglePayPaymentMethodLauncherContract.Args): Builder

        @BindsInstance
        fun savedStateHandle(savedStateHandle: SavedStateHandle): Builder

        fun build(): GooglePayPaymentMethodLauncherViewModelSubcomponent
    }
}
