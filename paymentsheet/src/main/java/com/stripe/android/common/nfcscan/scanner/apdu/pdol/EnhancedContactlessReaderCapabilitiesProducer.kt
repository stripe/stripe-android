package com.stripe.android.common.nfcscan.scanner.apdu.pdol

import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadata

/*
 * Enhanced Contactless Reader Capabilities (tag `9F6E`) is an American Express (Expresspay) specific field that tells
 * the card which contactless modes and features the reader supports. Amex cards request this tag in their PDOL and
 * reject `GetProcessingOptions` with `6985` ("conditions of use not satisfied") when it is all-zero, because that
 * advertises a reader that supports nothing.
 *
 * The value below advertises support for contact + contactless EMV mode so the card proceeds with an EMV-mode
 * transaction (which is what exposes the Track 2 / PAN records we read).
 */
internal object EnhancedContactlessReaderCapabilitiesProducer : TagValueProducer {
    override fun produce(paymentMethodMetadata: PaymentMethodMetadata): TagValueProducer.Result {
        return TagValueProducer.Result(
            tag = TAG_ENHANCED_CONTACTLESS_READER_CAPABILITIES,
            value = ENHANCED_CONTACTLESS_READER_CAPABILITIES,
        )
    }

    private const val TAG_ENHANCED_CONTACTLESS_READER_CAPABILITIES = "9F6E"

    // Byte 1: contact + contactless EMV mode supported. Remaining bytes reserved/zero.
    private val ENHANCED_CONTACTLESS_READER_CAPABILITIES =
        byteArrayOf(0xD8.toByte(), 0xE0.toByte(), 0x00, 0x00)
}
