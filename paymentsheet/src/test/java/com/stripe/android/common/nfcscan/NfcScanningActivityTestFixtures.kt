package com.stripe.android.common.nfcscan

import android.nfc.Tag
import android.nfc.tech.IsoDep
import com.stripe.android.common.nfcscan.scanner.FakeNfcTagTransceiver
import com.stripe.android.common.nfcscan.scanner.apdu.apduSuccessResponse
import com.stripe.android.common.nfcscan.scanner.apdu.tlv
import org.mockito.kotlin.any
import org.mockito.kotlin.doAnswer
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

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

    val RECORD_NOT_FOUND_RESPONSE = byteArrayOf(0x6A.toByte(), 0x83.toByte())
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

    // Form Factor Indicator (tag 0x9F6E) whose first byte low nibble reports a mobile device.
    val MOBILE_WALLET_FORM_FACTOR_DATA = byteArrayOf(0x01, 0x00)

    // Payment Account Reference (tag 0x9F24) marks the credential as tokenized.
    val PAYMENT_ACCOUNT_REFERENCE_DATA = byteArrayOf(0x00, 0x11, 0x22, 0x33)

    private const val TAG_9F = 0x9F.toByte()
    private const val TAG_CONTINUATION_FORM_FACTOR_INDICATOR = 0x6E.toByte()
    private const val TAG_CONTINUATION_PAYMENT_ACCOUNT_REFERENCE = 0x24.toByte()

    fun createIsoDepTag(): Tag = mock()

    fun createConfiguredIsoDep(
        responses: List<ByteArray>,
    ): Pair<Tag, IsoDep> {
        val tag = createIsoDepTag()

        val fakeTransceiver = FakeNfcTagTransceiver(
            transceiveResults = responses,
            transceiveResult = RECORD_NOT_FOUND_RESPONSE,
        )

        val isoDep = mock<IsoDep>()

        whenever(isoDep.transceive(any())).thenAnswer { invocation ->
            fakeTransceiver.transceive(invocation.arguments[0] as ByteArray)
        }

        doAnswer {
            fakeTransceiver.open()
            null
        }.whenever(isoDep).connect()

        doAnswer {
            fakeTransceiver.close()
            null
        }.whenever(isoDep).close()

        return tag to isoDep
    }

    fun successResponses(): List<ByteArray> = listOf(
        apduSuccessResponse(tlv(tag = 0x4F, value = VISA_AID)),
        apduSuccessResponse(byteArrayOf()),
        apduSuccessResponse(tlv(tag = 0x57, value = VALID_TRACK_2_DATA)),
    )

    fun declinedCardResponses(): List<ByteArray> = listOf(
        apduSuccessResponse(tlv(tag = 0x4F, value = VISA_AID)),
        apduSuccessResponse(byteArrayOf()),
        CARD_DECLINED_RESPONSE,
    )

    fun unsupportedCardResponses(): List<ByteArray> = listOf(
        FILE_NOT_FOUND_RESPONSE,
    )

    fun expiredCardResponses(): List<ByteArray> = listOf(
        apduSuccessResponse(tlv(tag = 0x4F, value = VISA_AID)),
        apduSuccessResponse(byteArrayOf()),
        apduSuccessResponse(tlv(tag = 0x57, value = EXPIRED_TRACK_2_DATA)),
    )

    fun mobileWalletCardResponses(): List<ByteArray> = listOf(
        apduSuccessResponse(tlv(tag = 0x4F, value = VISA_AID)),
        apduSuccessResponse(byteArrayOf()),
        apduSuccessResponse(tlv(tag = 0x57, value = VALID_TRACK_2_DATA)),
        apduSuccessResponse(
            tlv(
                tag = TAG_9F,
                tagContinuation = TAG_CONTINUATION_FORM_FACTOR_INDICATOR,
                value = MOBILE_WALLET_FORM_FACTOR_DATA,
            ),
        ),
    )

    fun tokenizedCardResponses(): List<ByteArray> = listOf(
        apduSuccessResponse(tlv(tag = 0x4F, value = VISA_AID)),
        apduSuccessResponse(byteArrayOf()),
        apduSuccessResponse(tlv(tag = 0x57, value = VALID_TRACK_2_DATA)),
        apduSuccessResponse(
            tlv(
                tag = TAG_9F,
                tagContinuation = TAG_CONTINUATION_PAYMENT_ACCOUNT_REFERENCE,
                value = PAYMENT_ACCOUNT_REFERENCE_DATA,
            ),
        ),
    )
}
