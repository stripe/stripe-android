package com.stripe.android.common.nfcscan

import com.stripe.android.common.nfcscan.scanner.apdu.apduSuccessResponse
import com.stripe.android.common.nfcscan.scanner.apdu.tlv
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadata
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadataFactory
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

    val FULL_PDOL_TEMPLATE = byteArrayOf(
        0x9F.toByte(), 0x02, 0x06,
        0x5F.toByte(), 0x2A, 0x02,
        0x9F.toByte(), 0x1A, 0x02,
        0x9F.toByte(), 0x35, 0x01,
        0x9A.toByte(), 0x03,
        0x9F.toByte(), 0x6E, 0x04,
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

    fun declinedCardResponses(): List<ByteArray> = listOf(
        CARD_DECLINED_RESPONSE,
    )

    fun unsupportedCardResponses(): List<ByteArray> = listOf(
        FILE_NOT_FOUND_RESPONSE,
    )

    fun expiredCardResponses(): List<ByteArray> = listOf(
        apduSuccessResponse(tlv(tag = 0x4F, value = VISA_AID)),
        apduSuccessResponse(EMPTY_PDOL_TEMPLATE),
        apduSuccessResponse(
            tlv(tag = 0x77, value = tlv(tag = 0x57, value = EXPIRED_TRACK_2_DATA)),
        ),
    )

    object ApduCommands {
        val SELECT_PPSE = byteArrayOf(
            0x00,
            0xA4.toByte(),
            0x04,
            0x00,
            0x0E,
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

        val SELECT_VISA_APPLICATION = byteArrayOf(
            0x00,
            0xA4.toByte(),
            0x04,
            0x00,
            0x07,
            0xA0.toByte(),
            0x00,
            0x00,
            0x00,
            0x03,
            0x10,
            0x10,
            0x00,
        )

        val GPO_EMPTY_PDOL = byteArrayOf(
            0x80.toByte(),
            0xA8.toByte(),
            0x00,
            0x00,
            0x02,
            0x83.toByte(),
            0x00,
            0x00,
        )

        val GPO_FULL_PDOL = byteArrayOf(
            0x80.toByte(),
            0xA8.toByte(),
            0x00,
            0x00,
            0x18,
            0x83.toByte(),
            0x16,
            0x00,
            0x00,
            0x00,
            0x00,
            0x55,
            0x02,
            0x09,
            0x78,
            0x08,
            0x40,
            0x34,
            0x20,
            0x01,
            0x01,
            0x18,
            0x80.toByte(),
            0x00,
            0x03,
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
