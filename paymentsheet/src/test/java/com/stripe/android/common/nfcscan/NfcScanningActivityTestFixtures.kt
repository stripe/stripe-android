package com.stripe.android.common.nfcscan

import com.stripe.android.common.nfcscan.scanner.apdu.apduSuccessResponse
import com.stripe.android.common.nfcscan.scanner.apdu.tlv
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadata
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadataFactory
import com.stripe.android.lpmfoundations.paymentmethod.PaymentSheetCardBrandFilter
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.testing.PaymentIntentFactory

internal object NfcScanningActivityTestFixtures {
    val VISA_AID = byteArrayOf(
        0xA0.toByte(),
        0x00,
        0x00,
        0x00,
        0x03,
        0x10,
        0x10,
    )

    val FILE_NOT_FOUND_RESPONSE = byteArrayOf(0x6A.toByte(), 0x82.toByte())
    val CARD_DECLINED_RESPONSE = byteArrayOf(0x69.toByte(), 0x85.toByte())

    val VALID_TRACK_2_DATA = byteArrayOf(
        0x42,
        0x42,
        0x42,
        0x42,
        0x42,
        0x42,
        0x42,
        0x42,
        0xD3.toByte(),
        0x01,
        0x21,
        0x01,
    )

    val EXPIRED_TRACK_2_DATA = byteArrayOf(
        0x42,
        0x42,
        0x42,
        0x42,
        0x42,
        0x42,
        0x42,
        0x42,
        0xD2.toByte(),
        0x00,
        0x10,
        0x01,
    )

    val EMPTY_PDOL_TEMPLATE = tlv(tag = 0x9F.toByte(), tagContinuation = 0x38, value = byteArrayOf())

    /**
     * PDOL template (tag `9F38`) returned during application selection. Each entry is a
     * tag plus the byte length the terminal must supply — values are omitted from the template.
     *
     * See [com.stripe.android.common.nfcscan.scanner.apdu.pdol.PdolBuilder].
     */
    val FULL_PDOL_TEMPLATE = byteArrayOf(
        // 9F02 (Amount, Authorized) — 6 bytes
        0x9F.toByte(), 0x02, 0x06,
        // 5F2A (Transaction Currency Code) — 2 bytes
        0x5F.toByte(), 0x2A, 0x02,
        // 9F1A (Terminal Country Code) — 2 bytes
        0x9F.toByte(), 0x1A, 0x02,
        // 9F35 (Terminal Type) — 1 byte
        0x9F.toByte(), 0x35, 0x01,
        // 9A (Transaction Date) — 3 bytes
        0x9A.toByte(), 0x03,
        // 9F6E (Enhanced Contactless Reader Capabilities) — 4 bytes
        0x9F.toByte(), 0x6E, 0x04,
        // 9F66 (Terminal Transaction Qualifiers) — 4 bytes
        0x9F.toByte(), 0x66, 0x04,
    )

    val FULL_PDOL_SELECT_RESPONSE = tlv(
        tag = 0x9F.toByte(),
        tagContinuation = 0x38,
        value = FULL_PDOL_TEMPLATE,
    )

    fun successResponses(): List<ByteArray> = listOf(
        apduSuccessResponse(tlv(tag = 0x4F, value = VISA_AID)),
        apduSuccessResponse(EMPTY_PDOL_TEMPLATE),
        apduSuccessResponse(
            tlv(tag = 0x77, value = tlv(tag = 0x57, value = VALID_TRACK_2_DATA)),
        ),
    )

    fun fullPdolSuccessResponses(): List<ByteArray> = listOf(
        apduSuccessResponse(tlv(tag = 0x4F, value = VISA_AID)),
        apduSuccessResponse(FULL_PDOL_SELECT_RESPONSE),
        apduSuccessResponse(
            tlv(tag = 0x77, value = tlv(tag = 0x57, value = VALID_TRACK_2_DATA)),
        ),
    )

    fun paymentMethodMetadataWithPdolData(): PaymentMethodMetadata {
        return PaymentMethodMetadataFactory.create(
            stripeIntent = PaymentIntentFactory.create(
                amount = FULL_PDOL_AMOUNT,
                currency = FULL_PDOL_CURRENCY,
            ).copy(created = FULL_PDOL_CREATED),
        )
    }

    fun paymentMethodMetadataWithVisaDisallowed(): PaymentMethodMetadata {
        return PaymentMethodMetadataFactory.create(
            cardBrandFilter = PaymentSheetCardBrandFilter(
                PaymentSheet.CardBrandAcceptance.disallowed(
                    listOf(PaymentSheet.CardBrandAcceptance.BrandCategory.Visa),
                ),
            ),
        )
    }

    fun declinedCardResponses(): List<ByteArray> = listOf(
        CARD_DECLINED_RESPONSE,
    )

    fun unsupportedCardResponses(): List<ByteArray> = listOf(
        FILE_NOT_FOUND_RESPONSE,
    )

    fun selectApplicationFailureResponses(): List<ByteArray> = listOf(
        apduSuccessResponse(tlv(tag = 0x4F, value = VISA_AID)),
        FILE_NOT_FOUND_RESPONSE,
    )

    fun expiredCardResponses(): List<ByteArray> = listOf(
        apduSuccessResponse(tlv(tag = 0x4F, value = VISA_AID)),
        apduSuccessResponse(EMPTY_PDOL_TEMPLATE),
        apduSuccessResponse(
            tlv(tag = 0x77, value = tlv(tag = 0x57, value = EXPIRED_TRACK_2_DATA)),
        ),
    )

    /*
     * Expected APDU command bytes sent to the card during NFC scanning.
     * Format: CLA | INS | P1 | P2 | Lc | command data | Le
     */
    object ApduCommands {
        /*
         * SELECT PPSE — locates the contactless payment application directory.
         * CLA=0x00, INS=SELECT (0xA4), P1=select by name (0x04), P2=first occurrence (0x00)
         */
        val SELECT_PPSE = byteArrayOf(
            0x00, // CLA
            0xA4.toByte(), // INS (SELECT)
            0x04, // P1 (select by name)
            0x00, // P2 (first or only occurrence)
            0x0E, // Lc (data length)
            // "2PAY.SYS.DDF01" (PPSE directory name)
            0x32,
            0x50,
            0x41,
            0x59,
            0x2E,
            0x53,
            0x59,
            0x53,
            0x2E,
            0x44,
            0x44,
            0x46,
            0x30,
            0x31,
            0x00,
        )

        /*
         * SELECT application — selects the Visa payment application by AID.
         * CLA=0x00, INS=SELECT (0xA4), P1=select by name (0x04), P2=first occurrence (0x00)
         */
        val SELECT_VISA_APPLICATION = byteArrayOf(
            0x00, // CLA
            0xA4.toByte(), // INS (SELECT)
            0x04, // P1 (select by name)
            0x00, // P2 (first or only occurrence)
            0x07, // Lc (AID length)
            // Visa AID (Application Identifier)
            0xA0.toByte(),
            0x00,
            0x00,
            0x00,
            0x03,
            0x10,
            0x10,
            0x00,
        )

        /*
         * GET PROCESSING OPTIONS — card returned an empty PDOL template, so no terminal data is sent.
         * CLA=0x80, INS=GET PROCESSING OPTIONS (0xA8)
         */
        val GPO_EMPTY_PDOL = byteArrayOf(
            0x80.toByte(), // CLA
            0xA8.toByte(), // INS (GET PROCESSING OPTIONS)
            0x00, // P1
            0x00, // P2
            0x02, // Lc (command data length)
            0x83.toByte(), // tag 83 (command template)
            0x00, // length (empty PDOL — card requested no terminal data)
            0x00,
        )

        /*
         * GET PROCESSING OPTIONS with populated PDOL data built from [FULL_PDOL_TEMPLATE] and
         * [paymentMethodMetadataWithPdolData]. Command data is tag `83` followed by terminal
         * values concatenated in PDOL order.
         * CLA=0x80, INS=GET PROCESSING OPTIONS (0xA8)
         */
        val GPO_FULL_PDOL = byteArrayOf(
            0x80.toByte(), // CLA
            0xA8.toByte(), // INS (GET PROCESSING OPTIONS)
            0x00, // P1
            0x00, // P2
            0x18, // Lc (command data length)
            0x83.toByte(), // tag 83 (command template)
            0x16, // length (22 bytes of PDOL response)
            // 9F02 — Amount, Authorized (€55.02 = 5502 minor units, BCD)
            0x00,
            0x00,
            0x00,
            0x00,
            0x55,
            0x02,
            // 5F2A — Transaction Currency Code (EUR = 978, BCD)
            0x09,
            0x78,
            // 9F1A — Terminal Country Code (US = 840, BCD; from default locale in tests)
            0x08,
            0x40,
            // 9F35 — Terminal Type (0x34 = cardholder-controlled, unattended, online-only)
            0x34,
            // 9A — Transaction Date (2020-01-01, BCD YYMMDD; from [FULL_PDOL_CREATED])
            0x20,
            0x01,
            0x01,
            // 9F6E — Enhanced Contactless Reader Capabilities (Amex Expresspay)
            0x18,
            0x80.toByte(),
            0x00,
            0x03,
            // 9F66 — Terminal Transaction Qualifiers (Visa payWave)
            0x20,
            0x00,
            0x40,
            0x00,
            0x00,
        )
    }

    private const val FULL_PDOL_AMOUNT = 5502L
    private const val FULL_PDOL_CURRENCY = "eur"
    private const val FULL_PDOL_CREATED = 1577838600L
}
