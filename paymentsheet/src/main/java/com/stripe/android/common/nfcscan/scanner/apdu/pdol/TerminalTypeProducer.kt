package com.stripe.android.common.nfcscan.scanner.apdu.pdol

import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadata

/*
 * Terminal Type (tag `9F35`) indicates the environment of the terminal, its communications capability, and its
 * operational control. Some card applications (notably American Express) request this tag in their PDOL and reject
 * `GetProcessingOptions` with `6985` ("conditions of use not satisfied") if it is zero/invalid.
 *
 * `0x34` = cardholder-controlled, unattended, online-only terminal.
 */
internal object TerminalTypeProducer : TagValueProducer {
    override fun produce(paymentMethodMetadata: PaymentMethodMetadata): TagValueProducer.Result {
        return TagValueProducer.Result(
            tag = TAG_TERMINAL_TYPE,
            value = TERMINAL_TYPE,
        )
    }

    private const val TAG_TERMINAL_TYPE = "9F35"
    private val TERMINAL_TYPE = byteArrayOf(0x34)
}
