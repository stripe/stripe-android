package com.stripe.android.common.nfcscan.scanner

import com.google.common.truth.Truth.assertThat
import com.stripe.android.common.nfcscan.scanner.apdu.ApduResponseError
import com.stripe.android.common.nfcscan.scanner.apdu.apduSuccessResponse
import com.stripe.android.common.nfcscan.scanner.apdu.tlv
import com.stripe.android.isInstanceOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Test

internal class ApduCardReaderTest {
    @Test
    fun `readCard selects PPSE then application then probes records`() = runScenario(
        transceiveResults = listOf(
            apduSuccessResponse(tlv(tag = 0x4F, value = VISA_AID)),
            apduSuccessResponse(byteArrayOf()),
        ),
        transceiveResult = RECORD_NOT_FOUND_RESPONSE,
    ) {
        val result = cardReader.readCard(transceiver)

        assertThat(result.exceptionOrNull()).isInstanceOf<IllegalStateException>()

        assertThat(transceiver.transceiveCalls.awaitItem()).isEqualTo(SELECT_PPSE_REQUEST)
        assertThat(transceiver.transceiveCalls.awaitItem()).isEqualTo(SELECT_VISA_APPLICATION_REQUEST)

        assertReadRecordProbes()
    }

    @Test
    fun `readCard continues probing when read record commands fail`() = runScenario(
        transceiveResults = listOf(
            apduSuccessResponse(tlv(tag = 0x4F, value = VISA_AID)),
            apduSuccessResponse(byteArrayOf()),
        ),
        transceiveResult = RECORD_NOT_FOUND_RESPONSE,
    ) {
        val result = cardReader.readCard(transceiver)

        assertThat(result.exceptionOrNull()).isInstanceOf<IllegalStateException>()

        assertThat(transceiver.transceiveCalls.awaitItem()).isEqualTo(SELECT_PPSE_REQUEST)
        assertThat(transceiver.transceiveCalls.awaitItem()).isEqualTo(SELECT_VISA_APPLICATION_REQUEST)

        assertReadRecordProbes()
    }

    @Test
    fun `readCard propagates PPSE selection failure`() = runScenario(
        transceiveResult = RECORD_NOT_FOUND_RESPONSE,
    ) {
        val result = cardReader.readCard(transceiver)

        assertThat(result.exceptionOrNull()).isInstanceOf<ApduResponseError.Command>()
        assertThat(transceiver.transceiveCalls.awaitItem()).isEqualTo(SELECT_PPSE_REQUEST)
    }

    private fun runScenario(
        transceiveResult: ByteArray = apduSuccessResponse(tlv(tag = 0x4F, value = VISA_AID)),
        transceiveResults: List<ByteArray> = emptyList(),
        block: suspend Scenario.() -> Unit,
    ) = runTest {
        val fakeTransceiver = FakeNfcTagTransceiver(
            transceiveResult = transceiveResult,
            transceiveResults = transceiveResults,
        )
        val reader = ApduCardReader(
            workContext = UnconfinedTestDispatcher(testScheduler),
        )

        Scenario(
            cardReader = reader,
            transceiver = fakeTransceiver,
        ).apply { block() }

        fakeTransceiver.ensureAllEventsConsumed()
    }

    private class Scenario(
        val cardReader: ApduCardReader,
        val transceiver: FakeNfcTagTransceiver,
    ) {
        suspend fun assertReadRecordProbes() {
            for (expectedRequest in READ_RECORD_REQUESTS) {
                assertThat(transceiver.transceiveCalls.awaitItem()).isEqualTo(expectedRequest)
            }
        }
    }

    private companion object {
        const val MAX_RECORDS_PER_SFI = 8

        val READ_RECORD_REQUESTS = buildList {
            for (sfi in 1..3) {
                val shortFileIdentifierByte = ((sfi shl 3) or 4).toByte()
                for (record in 1..MAX_RECORDS_PER_SFI) {
                    add(
                        byteArrayOf(
                            0x00,
                            0xB2.toByte(),
                            record.toByte(),
                            shortFileIdentifierByte,
                            0x00,
                        ),
                    )
                }
            }
        }

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
        val SELECT_VISA_APPLICATION_REQUEST = byteArrayOf(
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
    }
}
