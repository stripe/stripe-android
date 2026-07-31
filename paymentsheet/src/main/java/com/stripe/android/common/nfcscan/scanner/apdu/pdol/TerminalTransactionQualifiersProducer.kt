package com.stripe.android.common.nfcscan.scanner.apdu.pdol

import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadata

/*
 * Terminal Transaction Qualifiers (tag `9F66`) is a Visa payWave-specific field that tells the card the reader's
 * contactless capabilities and requirements. Visa cards request this tag in their PDOL.
 *
 * The value below matches an online-only terminal on a cardholder-controlled device:
 * - Byte 1 `0x20`: qVSDC supported; online-capable; no contact chip, magstripe, offline-only, PIN, signature, or ODA.
 * - Byte 2 `0x00`: online cryptogram not required and CVM not required at transaction start (transient bits).
 * - Byte 3 `0x40`: mobile functionality / consumer device CVM supported.
 * - Byte 4 `0x00`: RFU.
 */
internal object TerminalTransactionQualifiersProducer : TagValueProducer {
    override fun produce(paymentMethodMetadata: PaymentMethodMetadata): TagValueProducer.Result {
        return TagValueProducer.Result(
            tag = TAG_TERMINAL_TRANSACTION_QUALIFIERS,
            value = TERMINAL_TRANSACTION_QUALIFIERS,
        )
    }

    private const val TAG_TERMINAL_TRANSACTION_QUALIFIERS = "9F66"

    private val TERMINAL_TRANSACTION_QUALIFIERS =
        byteArrayOf(0x20, 0x00, 0x40, 0x00)
}
