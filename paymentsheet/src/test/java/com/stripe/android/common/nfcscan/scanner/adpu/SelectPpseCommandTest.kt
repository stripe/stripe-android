package com.stripe.android.common.nfcscan.scanner.adpu

import com.google.common.truth.Truth.assertThat
import com.stripe.android.common.nfcscan.scanner.FakeNfcTagTransceiver
import kotlinx.coroutines.test.runTest
import org.junit.Test

internal class SelectPpseCommandTest {
    @Test
    fun `transceiveWith sends SELECT PPSE command`() = runTest {
        val fakeTransceiver = FakeNfcTagTransceiver(
            transceiveResult = apduSuccessResponse(
                tlv(tag = 0x4F, value = VISA_AID),
            ),
        )

        SelectPpseCommand.transceiveWith(fakeTransceiver)

        assertThat(fakeTransceiver.transceiveCalls.awaitItem()).isEqualTo(SELECT_PPSE_REQUEST)
        fakeTransceiver.ensureAllEventsConsumed()
    }

    @Test
    fun `transceiveWith returns ApplicationIdentifier from AID tag`() = runTest {
        val fakeTransceiver = FakeNfcTagTransceiver(
            transceiveResult = apduSuccessResponse(
                tlv(tag = 0x4F, value = VISA_AID),
            ),
        )

        val result = SelectPpseCommand.transceiveWith(fakeTransceiver)

        assertThat(result.getOrNull()).isEqualTo(
            ApplicationIdentifier(VISA_AID.toHexString()),
        )
        fakeTransceiver.transceiveCalls.awaitItem()
        fakeTransceiver.ensureAllEventsConsumed()
    }

    @Test
    fun `transceiveWith returns ApplicationIdentifier from DF name tag`() = runTest {
        val fakeTransceiver = FakeNfcTagTransceiver(
            transceiveResult = apduSuccessResponse(
                tlv(tag = 0x84.toByte(), value = MASTERCARD_AID),
            ),
        )

        val result = SelectPpseCommand.transceiveWith(fakeTransceiver)

        assertThat(result.getOrNull()).isEqualTo(
            ApplicationIdentifier(MASTERCARD_AID.toHexString()),
        )
        fakeTransceiver.transceiveCalls.awaitItem()
        fakeTransceiver.ensureAllEventsConsumed()
    }

    @Test
    fun `transceiveWith returns ApplicationIdentifier from supported card brand prefix`() = runTest {
        val fakeTransceiver = FakeNfcTagTransceiver(
            transceiveResult = apduSuccessResponse(
                tlv(tag = 0x42, value = VISA_PREFIX),
            ),
        )

        val result = SelectPpseCommand.transceiveWith(fakeTransceiver)

        assertThat(result.getOrNull()).isEqualTo(
            ApplicationIdentifier(VISA_PREFIX.toHexString().uppercase()),
        )
        fakeTransceiver.transceiveCalls.awaitItem()
        fakeTransceiver.ensureAllEventsConsumed()
    }

    @Test
    fun `transceiveWith returns Parsing error when tlv contains no AID`() = runTest {
        val fakeTransceiver = FakeNfcTagTransceiver(
            transceiveResult = apduSuccessResponse(
                tlv(tag = 0x50, value = byteArrayOf(0x56, 0x49, 0x53, 0x41)),
            ),
        )

        val result = SelectPpseCommand.transceiveWith(fakeTransceiver)

        assertThat(result.exceptionOrNull()).isInstanceOf(ApduResponseError.Parsing::class.java)
        fakeTransceiver.transceiveCalls.awaitItem()
        fakeTransceiver.ensureAllEventsConsumed()
    }

    private companion object {
        val VISA_AID = byteArrayOf(
            0xA0.toByte(),
            0x00,
            0x00,
            0x00,
            0x03,
            0x10,
            0x10,
        )
        val MASTERCARD_AID = byteArrayOf(
            0xA0.toByte(),
            0x00,
            0x00,
            0x00,
            0x04,
            0x10,
            0x10,
        )
        val VISA_PREFIX = byteArrayOf(
            0xA0.toByte(),
            0x00,
            0x00,
            0x00,
            0x03,
        )
        val SELECT_PPSE_REQUEST = byteArrayOf(
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
    }
}
