package com.stripe.android.paymentsheet.flowcontroller

import androidx.activity.result.ActivityResultLauncher
import com.stripe.android.PaymentController
import com.stripe.android.PaymentRelayStarter
import com.stripe.android.auth.PaymentAuthWebViewContract

internal fun interface PaymentControllerFactory {
    fun create(
        paymentRelayLauncher: ActivityResultLauncher<PaymentRelayStarter.Args>,
        paymentAuthWebViewLauncher: ActivityResultLauncher<PaymentAuthWebViewContract.Args>
    ): PaymentController
}
