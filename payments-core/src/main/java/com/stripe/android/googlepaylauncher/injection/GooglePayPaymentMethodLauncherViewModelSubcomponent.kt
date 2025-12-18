package com.stripe.android.googlepaylauncher.injection

import androidx.lifecycle.SavedStateHandle
import com.stripe.android.googlepaylauncher.GooglePayPaymentMethodLauncherContractV2
import com.stripe.android.googlepaylauncher.GooglePayPaymentMethodLauncherViewModel
import dagger.BindsInstance
import dagger.Subcomponent

@Subcomponent
internal interface GooglePayPaymentMethodLauncherViewModelSubcomponent {
    val viewModel: GooglePayPaymentMethodLauncherViewModel

    @Subcomponent.Factory
    interface Factory {
        fun create(
            @BindsInstance
            args: GooglePayPaymentMethodLauncherContractV2.Args,
            @BindsInstance
            savedStateHandle: SavedStateHandle,
        ): GooglePayPaymentMethodLauncherViewModelSubcomponent
    }
}
