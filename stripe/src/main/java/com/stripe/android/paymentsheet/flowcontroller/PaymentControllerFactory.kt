package com.stripe.android.paymentsheet.flowcontroller

import androidx.activity.result.ActivityResultLauncher
import com.stripe.android.PaymentController
import com.stripe.android.PaymentRelayStarter
import com.stripe.android.auth.PaymentBrowserAuthContract
import com.stripe.android.payments.PaymentFlowResult

internal fun interface PaymentControllerFactory {
    fun create(
        paymentRelayLauncher: ActivityResultLauncher<PaymentRelayStarter.Args>,
        paymentBrowserAuthLauncher: ActivityResultLauncher<PaymentBrowserAuthContract.Args>,
        stripe3ds2ChallengeLauncher: ActivityResultLauncher<PaymentFlowResult.Unvalidated>
    ): PaymentController
}
