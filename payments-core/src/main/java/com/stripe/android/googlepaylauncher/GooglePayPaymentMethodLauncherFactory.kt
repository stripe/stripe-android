package com.stripe.android.googlepaylauncher

import androidx.activity.result.ActivityResultLauncher
import dagger.assisted.AssistedFactory
import kotlinx.coroutines.CoroutineScope

@AssistedFactory
interface GooglePayPaymentMethodLauncherFactory {
    fun create(
        lifecycleScope: CoroutineScope,
        config: GooglePayPaymentMethodLauncher.Config,
        readyCallback: GooglePayPaymentMethodLauncher.ReadyCallback,
        activityResultLauncher: ActivityResultLauncher<GooglePayPaymentMethodLauncherContract.Args>
    ): GooglePayPaymentMethodLauncher
}
