package com.stripe.android.paymentelement.confirmation.epms

import androidx.activity.result.ActivityResultCallback
import com.stripe.android.payments.paymentlauncher.PaymentResult

internal fun ActivityResultCallback<*>.asExternalPaymentMethodCallback(): ActivityResultCallback<PaymentResult> {
    @Suppress("UNCHECKED_CAST")
    return this as ActivityResultCallback<PaymentResult>
}
