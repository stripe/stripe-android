package com.stripe.android.googlepaylauncher.injection

import androidx.activity.result.ActivityResultLauncher
import com.stripe.android.googlepaylauncher.GooglePayPaymentMethodLauncher
import com.stripe.android.googlepaylauncher.GooglePayPaymentMethodLauncherContract
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
