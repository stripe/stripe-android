package com.stripe.android.googlepaylauncher.injection

import androidx.activity.result.ActivityResultLauncher
import androidx.annotation.RestrictTo
import com.stripe.android.googlepaylauncher.GooglePayPaymentMethodLauncherContractV2
import com.stripe.android.googlepaylauncher.InternalGooglePayPaymentMethodLauncher
import dagger.assisted.AssistedFactory

@AssistedFactory
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
interface InternalGooglePayPaymentMethodLauncherFactory {
    fun create(
        activityResultLauncher: ActivityResultLauncher<GooglePayPaymentMethodLauncherContractV2.Args>,
    ): InternalGooglePayPaymentMethodLauncher
}
