package com.stripe.android.common.nfcscan.scanner.apdu.pdol

import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadata

internal interface TagValueProducer {
    class Result(
        val tag: String,
        val value: ByteArray,
    )

    fun produce(paymentMethodMetadata: PaymentMethodMetadata): Result
}
