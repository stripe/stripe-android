package com.stripe.android.common.nfcscan.scanner.apdu

import com.google.common.truth.Truth.assertThat
import com.stripe.android.common.nfcscan.scanner.FakeNfcTagTransceiver
import com.stripe.android.isInstanceOf
import kotlinx.coroutines.test.runTest
import org.junit.Test

internal class ReadRecordCommandTest {
    @Test
    fun `transceiveWith sends READ RECORD command with encoded short file identifier`() = test(
        transceiveResult = apduSuccessResponse(byteArrayOf()),
    ) {
        ReadRecordCommand(recordNumber = 5, shortFileIdentifier = 2)
            .transceiveWith(transceiver)

        assertThat(transceiver.transceiveCalls.awaitItem()).isEqualTo(
            byteArrayOf(0x00, 0xB2.toByte(), 0x05, 0x14, 0x00),
        )
    }

    @Test
    fun `transceiveWith returns parsed TLV data on success`() = test(
        transceiveResult = apduSuccessResponse(tlv(tag = 0x57, value = TRACK_2_DATA)),
    ) {
        val result = ReadRecordCommand(recordNumber = 1, shortFileIdentifier = 1)
            .transceiveWith(transceiver)

        assertThat(result.isSuccess).isTrue()

        val tlvData = result.getOrNull()

        assertThat(tlvData).containsKey("57")
        assertThat(tlvData?.getValue("57")?.contentEquals(TRACK_2_DATA)).isTrue()

        assertThat(transceiver.transceiveCalls.awaitItem()).isNotNull()
    }

    @Test
    fun `transceiveWith returns Command error when record is not found`() = test(
        transceiveResult = byteArrayOf(0x6A.toByte(), 0x83.toByte()),
    ) {
        val result = ReadRecordCommand(recordNumber = 1, shortFileIdentifier = 1)
            .transceiveWith(transceiver)

        assertThat(result.exceptionOrNull()).isInstanceOf<ApduResponseError.Command>()
        assertThat(transceiver.transceiveCalls.awaitItem()).isNotNull()
    }

    private fun test(
        transceiveResult: ByteArray,
        block: suspend Scenario.() -> Unit,
    ) = runTest {
        val fakeTransceiver = FakeNfcTagTransceiver(
            transceiveResult = transceiveResult,
        )

        block(Scenario(fakeTransceiver))

        fakeTransceiver.ensureAllEventsConsumed()
    }

    private class Scenario(
        val transceiver: FakeNfcTagTransceiver,
    )

    private companion object {
        val TRACK_2_DATA = byteArrayOf(
            0x12,
            0x34,
            0x56,
            0x78,
            0x90.toByte(),
        )
    }
}
