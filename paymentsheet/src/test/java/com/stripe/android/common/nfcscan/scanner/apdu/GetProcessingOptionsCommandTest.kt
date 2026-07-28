package com.stripe.android.common.nfcscan.scanner.apdu

import com.google.common.truth.Truth.assertThat
import com.stripe.android.common.nfcscan.scanner.FakeNfcTagTransceiver
import kotlinx.coroutines.test.runTest
import org.junit.Test

internal class GetProcessingOptionsCommandTest {
    @Test
    fun `transceiveWith sends GPO command with empty PDOL`() = test(
        transceiveResult = apduSuccessResponse(byteArrayOf()),
    ) {
        GetProcessingOptionsCommand.transceiveWith(transceiver)

        assertThat(transceiver.transceiveCalls.awaitItem()).isEqualTo(
            byteArrayOf(
                0x80.toByte(),
                0xA8.toByte(),
                0x00,
                0x00,
                0x02,
                0x83.toByte(),
                0x00,
                0x00,
            ),
        )
    }

    @Test
    fun `transceiveWith parses AFL from response template format 2`() = test(
        transceiveResult = apduSuccessResponse(
            tlv(
                tag = 0x77,
                value = tlv(tag = 0x94.toByte(), value = byteArrayOf(0x08, 0x01, 0x01, 0x00)),
            ),
        ),
    ) {
        val result = GetProcessingOptionsCommand.transceiveWith(transceiver)

        assertThat(result.isSuccess).isTrue()
        assertThat(result.getOrNull()?.aflEntries).containsExactly(
            ProcessingOptionsInfo.AflEntry(
                shortFileIdentifier = 1,
                firstRecord = 1,
                lastRecord = 1,
            ),
        )
        assertThat(transceiver.transceiveCalls.awaitItem()).isNotNull()
    }

    @Test
    fun `transceiveWith parses AFL from response template format 1`() = test(
        transceiveResult = apduSuccessResponse(
            tlv(
                tag = 0x80.toByte(),
                value = byteArrayOf(
                    0x00,
                    0x00,
                    0x08,
                    0x01,
                    0x01,
                    0x00,
                ),
            ),
        ),
    ) {
        val result = GetProcessingOptionsCommand.transceiveWith(transceiver)

        assertThat(result.isSuccess).isTrue()
        assertThat(result.getOrNull()?.aflEntries).containsExactly(
            ProcessingOptionsInfo.AflEntry(
                shortFileIdentifier = 1,
                firstRecord = 1,
                lastRecord = 1,
            ),
        )
        assertThat(transceiver.transceiveCalls.awaitItem()).isNotNull()
    }

    @Test
    fun `transceiveWith extracts card records from response template format 2`() = test(
        transceiveResult = apduSuccessResponse(
            tlv(
                tag = 0x77,
                value = tlv(tag = 0x57, value = TRACK_2_DATA),
            ),
        ),
    ) {
        val result = GetProcessingOptionsCommand.transceiveWith(transceiver)

        assertThat(result.isSuccess).isTrue()
        assertThat(result.getOrNull()?.records?.getValue("57")?.contentEquals(TRACK_2_DATA)).isTrue()
        assertThat(result.getOrNull()?.aflEntries).isEmpty()
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
            0x41,
            0x11,
            0x11,
            0x11,
            0x11,
            0x11,
            0x11,
            0x11,
            0xD2.toByte(),
            0x51,
            0x21,
            0x01,
        )
    }
}
