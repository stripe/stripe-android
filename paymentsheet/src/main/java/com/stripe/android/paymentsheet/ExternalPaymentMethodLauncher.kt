package com.stripe.android.paymentsheet

import androidx.activity.result.ActivityResultLauncher
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

internal class ExternalPaymentMethodLauncher @AssistedInject internal constructor(
    @Assisted private val hostActivityLauncher: ActivityResultLauncher<ExternalPaymentMethodInput>,
) {
    fun confirm(input: ExternalPaymentMethodInput) {
        hostActivityLauncher.launch(input)
    }
}
