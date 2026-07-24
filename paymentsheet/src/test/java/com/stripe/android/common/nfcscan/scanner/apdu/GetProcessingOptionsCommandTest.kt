package com.stripe.android.common.nfcscan.scanner.apdu

import com.google.common.truth.Truth.assertThat
import com.stripe.android.common.nfcscan.scanner.FakeNfcTagTransceiver
import kotlinx.coroutines.test.runTest
import org.junit.Test

internal class GetProcessingOptionsCommandTest {
    @Test
    fun `transceiveWith sends GET PROCESSING OPTIONS command without PDOL`() = test(
        transceiveResult = GPO_WITHOUT_AFL_RESPONSE,
    ) {
        GetProcessingOptionsCommand(processingOptionsDataObjectList = null)
            .transceiveWith(transceiver)

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
    fun `transceiveWith sends GET PROCESSING OPTIONS command with TTQ from PDOL`() = test(
        transceiveResult = GPO_WITHOUT_AFL_RESPONSE,
    ) {
        GetProcessingOptionsCommand(processingOptionsDataObjectList = PDOL_WITH_TTQ)
            .transceiveWith(transceiver)

        assertThat(transceiver.transceiveCalls.awaitItem()).isEqualTo(
            byteArrayOf(
                0x80.toByte(),
                0xA8.toByte(),
                0x00,
                0x00,
                0x06,
                0x83.toByte(),
                0x04,
                0x36,
                0x00,
                0x00,
                0x00,
                0x00,
            ),
        )
    }

    @Test
    fun `transceiveWith pads unknown PDOL values with zeroes`() = test(
        transceiveResult = GPO_WITHOUT_AFL_RESPONSE,
    ) {
        GetProcessingOptionsCommand(processingOptionsDataObjectList = PDOL_WITH_AMOUNT)
            .transceiveWith(transceiver)

        assertThat(transceiver.transceiveCalls.awaitItem()).isEqualTo(
            byteArrayOf(
                0x80.toByte(),
                0xA8.toByte(),
                0x00,
                0x00,
                0x08,
                0x83.toByte(),
                0x06,
                0x00,
                0x00,
                0x00,
                0x00,
                0x00,
                0x00,
                0x00,
            ),
        )
    }

    @Test
    fun `transceiveWith returns AFL records from response template 77`() = test(
        transceiveResult = apduSuccessResponse(
            tlv(tag = 0x77, value = tlv(tag = 0x94.toByte(), value = AFL)),
        ),
    ) {
        val result = GetProcessingOptionsCommand(processingOptionsDataObjectList = null)
            .transceiveWith(transceiver)

        assertThat(result.isSuccess).isTrue()
        assertThat(result.getOrNull()?.recordLocators).containsExactly(
            GetProcessingOptionsCommand.RecordLocator(
                shortFileIdentifier = 2,
                firstRecord = 1,
                lastRecord = 3,
            ),
            GetProcessingOptionsCommand.RecordLocator(
                shortFileIdentifier = 4,
                firstRecord = 2,
                lastRecord = 2,
            ),
        ).inOrder()
        assertThat(transceiver.transceiveCalls.awaitItem()).isNotNull()
    }

    @Test
    fun `transceiveWith returns AFL records from response template 80`() = test(
        transceiveResult = apduSuccessResponse(
            tlv(
                tag = 0x80.toByte(),
                value = byteArrayOf(0x00, 0x00) + AFL,
            ),
        ),
    ) {
        val result = GetProcessingOptionsCommand(processingOptionsDataObjectList = null)
            .transceiveWith(transceiver)

        assertThat(result.isSuccess).isTrue()
        assertThat(result.getOrNull()?.recordLocators).containsExactly(
            GetProcessingOptionsCommand.RecordLocator(
                shortFileIdentifier = 2,
                firstRecord = 1,
                lastRecord = 3,
            ),
            GetProcessingOptionsCommand.RecordLocator(
                shortFileIdentifier = 4,
                firstRecord = 2,
                lastRecord = 2,
            ),
        ).inOrder()
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
        val PDOL_WITH_TTQ = byteArrayOf(0x9F.toByte(), 0x66, 0x04)
        val PDOL_WITH_AMOUNT = byteArrayOf(0x9F.toByte(), 0x02, 0x06)
        val AFL = byteArrayOf(
            0x10,
            0x01,
            0x03,
            0x00,
            0x20,
            0x02,
            0x02,
            0x00,
        )
        val GPO_WITHOUT_AFL_RESPONSE = apduSuccessResponse(
            tlv(tag = 0x80.toByte(), value = byteArrayOf(0x00, 0x00)),
        )
    }
}
