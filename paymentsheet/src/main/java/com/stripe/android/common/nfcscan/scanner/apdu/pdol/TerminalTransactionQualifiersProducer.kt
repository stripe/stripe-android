package com.stripe.android.common.nfcscan.scanner.apdu.pdol

import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadata

internal object TerminalTransactionQualifiersProducer : TagValueProducer {
    override fun produce(paymentMethodMetadata: PaymentMethodMetadata): TagValueProducer.Result {
        return TagValueProducer.Result(
            tag = TAG_TERMINAL_TRANSACTION_QUALIFIERS,
            value = TERMINAL_TRANSACTION_QUALIFIERS
        )
    }

    private const val TAG_TERMINAL_TRANSACTION_QUALIFIERS = "9F66"
    private val TERMINAL_TRANSACTION_QUALIFIERS = byteArrayOf(0x26, 0x00, 0x00, 0x00)
}
