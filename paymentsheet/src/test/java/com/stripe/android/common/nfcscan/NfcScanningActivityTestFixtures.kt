package com.stripe.android.common.nfcscan

import com.stripe.android.common.nfcscan.scanner.apdu.apduSuccessResponse
import com.stripe.android.common.nfcscan.scanner.apdu.tlv

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

    fun successResponses(): List<ByteArray> = listOf(
        apduSuccessResponse(tlv(tag = 0x4F, value = VISA_AID)),
        apduSuccessResponse(EMPTY_PDOL_TEMPLATE),
        apduSuccessResponse(
            tlv(tag = 0x77, value = tlv(tag = 0x57, value = VALID_TRACK_2_DATA)),
        ),
    )

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
}
