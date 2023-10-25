@file:RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)

package com.stripe.android.payments.paymentlauncher

import androidx.annotation.RestrictTo

internal fun PaymentLauncher.PaymentResultCallback.toLauncherResultCallback():
    PaymentLauncher.PaymentLauncherResultCallback {
    return PaymentLauncher.PaymentLauncherResultCallback { result ->
        when (result) {
            is PaymentLauncherResult.Completed -> onPaymentResult(PaymentResult.Completed)
            is PaymentLauncherResult.Failed -> onPaymentResult(PaymentResult.Failed(result.throwable))
            is PaymentLauncherResult.Canceled -> onPaymentResult(PaymentResult.Canceled)
        }
    }
}

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
fun toPaymentLauncherResultCallback(
    callback: (result: PaymentResult) -> Unit
): (result: PaymentLauncherResult) -> Unit {
    return { result ->
        when (result) {
            is PaymentLauncherResult.Completed -> callback.invoke(PaymentResult.Completed)
            is PaymentLauncherResult.Failed -> callback.invoke(PaymentResult.Failed(result.throwable))
            is PaymentLauncherResult.Canceled -> callback.invoke(PaymentResult.Canceled)
        }
    }
}
