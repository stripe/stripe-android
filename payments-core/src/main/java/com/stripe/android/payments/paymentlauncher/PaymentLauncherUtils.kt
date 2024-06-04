@file:RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)

package com.stripe.android.payments.paymentlauncher

import androidx.annotation.RestrictTo

internal fun PaymentLauncher.PaymentResultCallback.toInternalResultCallback():
    PaymentLauncher.InternalPaymentResultCallback {
    return PaymentLauncher.InternalPaymentResultCallback { result ->
        when (result) {
            is InternalPaymentResult.Completed -> onPaymentResult(PaymentResult.Completed)
            is InternalPaymentResult.Failed -> onPaymentResult(PaymentResult.Failed(result.throwable))
            is InternalPaymentResult.Canceled -> onPaymentResult(PaymentResult.Canceled)
        }
    }
}

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
fun toInternalPaymentResultCallback(
    callback: (result: PaymentResult) -> Unit
): (result: InternalPaymentResult) -> Unit {
    return { result ->
        when (result) {
            is InternalPaymentResult.Completed -> callback.invoke(PaymentResult.Completed)
            is InternalPaymentResult.Failed -> callback.invoke(PaymentResult.Failed(result.throwable))
            is InternalPaymentResult.Canceled -> callback.invoke(PaymentResult.Canceled)
        }
    }
}
