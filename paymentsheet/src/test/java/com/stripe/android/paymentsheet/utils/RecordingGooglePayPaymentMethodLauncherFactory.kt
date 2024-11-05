package com.stripe.android.paymentsheet.utils

import androidx.activity.result.ActivityResultLauncher
import com.stripe.android.CardBrandFilter
import com.stripe.android.googlepaylauncher.GooglePayPaymentMethodLauncher
import com.stripe.android.googlepaylauncher.GooglePayPaymentMethodLauncherContractV2
import com.stripe.android.googlepaylauncher.injection.GooglePayPaymentMethodLauncherFactory
import kotlinx.coroutines.CoroutineScope

internal class RecordingGooglePayPaymentMethodLauncherFactory(
    private val googlePayPaymentMethodLauncher: GooglePayPaymentMethodLauncher,
) : GooglePayPaymentMethodLauncherFactory {

    var config: GooglePayPaymentMethodLauncher.Config? = null
        private set

    override fun create(
        lifecycleScope: CoroutineScope,
        config: GooglePayPaymentMethodLauncher.Config,
        readyCallback: GooglePayPaymentMethodLauncher.ReadyCallback,
        activityResultLauncher: ActivityResultLauncher<GooglePayPaymentMethodLauncherContractV2.Args>,
        skipReadyCheck: Boolean,
        cardBrandFilter: CardBrandFilter
    ): GooglePayPaymentMethodLauncher {
        this.config = config
        return googlePayPaymentMethodLauncher
    }
}
