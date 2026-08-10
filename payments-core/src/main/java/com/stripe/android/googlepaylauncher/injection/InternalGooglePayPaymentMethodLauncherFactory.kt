package com.stripe.android.googlepaylauncher.injection

import androidx.activity.result.ActivityResultLauncher
import androidx.annotation.RestrictTo
import androidx.lifecycle.LifecycleOwner
import com.stripe.android.googlepaylauncher.GooglePayPaymentMethodLauncherContractV2
import com.stripe.android.googlepaylauncher.InternalGooglePayPaymentMethodLauncher
import com.stripe.android.googlepaylauncher.callback.GooglePayPaymentDataChangedCallback
import dagger.assisted.AssistedFactory

@AssistedFactory
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
interface InternalGooglePayPaymentMethodLauncherFactory {
    fun create(
        instanceId: String,
        lifecycleOwner: LifecycleOwner,
        onPaymentDataChangedCallback: GooglePayPaymentDataChangedCallback?,
        activityResultLauncher: ActivityResultLauncher<GooglePayPaymentMethodLauncherContractV2.Args>,
    ): InternalGooglePayPaymentMethodLauncher
}
