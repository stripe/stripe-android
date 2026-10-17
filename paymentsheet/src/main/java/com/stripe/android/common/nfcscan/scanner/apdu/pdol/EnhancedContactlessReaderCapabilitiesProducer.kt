package com.stripe.android.common.nfcscan.scanner.apdu.pdol

import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadata

/*
 * Enhanced Contactless Reader Capabilities (tag `9F6E`) is an American Express (Expresspay) specific field that tells
 * the card which contactless modes and features the reader supports. Amex cards request this tag in their PDOL and
 * reject `GetProcessingOptions` with `6985` ("conditions of use not satisfied") when it is all-zero, because that
 * advertises a reader that supports nothing.
 *
 * The value below matches the EMVCo Kernel C-4 mPOS-C/CSP profile for an online-only terminal on a
 * cardholder-controlled device (see [TerminalTypeProducer] `0x34`):
 * - Byte 1 `0x18`: contactless EMV partial online + mobile supported; no contact or magstripe.
 * - Byte 2 `0x80`: mobile CVM only (no online PIN, signature, or offline PIN on the phone reader).
 * - Byte 3 `0x00`: online-only (not offline-only); CVM-not-required at transaction start.
 * - Byte 4 `0x03`: C-4 kernel version 2.7+.
 */
internal object EnhancedContactlessReaderCapabilitiesProducer : TagValueProducer {
    override fun produce(paymentMethodMetadata: PaymentMethodMetadata): TagValueProducer.Result {
        return TagValueProducer.Result(
            tag = TAG_ENHANCED_CONTACTLESS_READER_CAPABILITIES,
            value = ENHANCED_CONTACTLESS_READER_CAPABILITIES,
        )
    }

    private const val TAG_ENHANCED_CONTACTLESS_READER_CAPABILITIES = "9F6E"

    private val ENHANCED_CONTACTLESS_READER_CAPABILITIES =
        byteArrayOf(0x18, 0x80.toByte(), 0x00, 0x03)
}
