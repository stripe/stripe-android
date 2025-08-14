package com.stripe.android.googlepaylauncher.injection

import androidx.activity.result.ActivityResultLauncher
import androidx.annotation.RestrictTo
import com.stripe.android.CardBrandFilter
import com.stripe.android.googlepaylauncher.GooglePayPaymentMethodLauncher
import com.stripe.android.googlepaylauncher.GooglePayPaymentMethodLauncherContractV2
import com.stripe.android.model.PassiveCaptchaParams
import dagger.assisted.AssistedFactory
import kotlinx.coroutines.CoroutineScope

@AssistedFactory
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
interface GooglePayPaymentMethodLauncherFactory {
    fun create(
        lifecycleScope: CoroutineScope,
        config: GooglePayPaymentMethodLauncher.Config,
        readyCallback: GooglePayPaymentMethodLauncher.ReadyCallback,
        activityResultLauncher: ActivityResultLauncher<GooglePayPaymentMethodLauncherContractV2.Args>,
        skipReadyCheck: Boolean = false,
        cardBrandFilter: CardBrandFilter,
        passiveCaptchaParams: PassiveCaptchaParams?
    ): GooglePayPaymentMethodLauncher
}
