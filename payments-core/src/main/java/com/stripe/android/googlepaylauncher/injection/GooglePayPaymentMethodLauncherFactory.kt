package com.stripe.android.googlepaylauncher.injection

import androidx.activity.result.ActivityResultLauncher
import androidx.annotation.RestrictTo
import com.stripe.android.googlepaylauncher.GooglePayPaymentMethodLauncher
import com.stripe.android.googlepaylauncher.GooglePayPaymentMethodLauncherContract
import dagger.assisted.AssistedFactory
import kotlinx.coroutines.CoroutineScope

@AssistedFactory
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
interface GooglePayPaymentMethodLauncherFactory {
    fun create(
        lifecycleScope: CoroutineScope,
        config: GooglePayPaymentMethodLauncher.Config,
        readyCallback: GooglePayPaymentMethodLauncher.ReadyCallback,
        activityResultLauncher: ActivityResultLauncher<GooglePayPaymentMethodLauncherContract.Args>,
        skipReadyCheck: Boolean = false
    ): GooglePayPaymentMethodLauncher
}
