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

        assertThat(cardDataParser.parseCalls.awaitItem()).isNotNull()
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

        assertThat(cardDataParser.parseCalls.awaitItem()).isNotNull()
    }

    @Test
    fun `readCard propagates PPSE selection failure`() = runScenario(
        transceiveResult = RECORD_NOT_FOUND_RESPONSE,
    ) {
        val result = cardReader.readCard(transceiver)

        assertThat(result.exceptionOrNull()).isInstanceOf<ApduResponseError.Command>()
        assertThat(transceiver.transceiveCalls.awaitItem()).isEqualTo(SELECT_PPSE_REQUEST)
    }

    @Test
    fun `readCard returns parsed card data when parser succeeds`() = runScenario(
        parseResult = SCANNED_CARD_DATA,
        transceiveResults = listOf(
            apduSuccessResponse(tlv(tag = 0x4F, value = VISA_AID)),
            apduSuccessResponse(byteArrayOf()),
            apduSuccessResponse(tlv(tag = 0x57, value = TRACK_2_DATA)),
        ),
        transceiveResult = RECORD_NOT_FOUND_RESPONSE,
    ) {
        val result = cardReader.readCard(transceiver)

        assertThat(result.getOrNull()).isEqualTo(SCANNED_CARD_DATA)

        assertThat(transceiver.transceiveCalls.awaitItem()).isEqualTo(SELECT_PPSE_REQUEST)
        assertThat(transceiver.transceiveCalls.awaitItem()).isEqualTo(SELECT_VISA_APPLICATION_REQUEST)
        assertThat(transceiver.transceiveCalls.awaitItem()).isEqualTo(READ_RECORD_REQUESTS.first())

        assertThat(cardDataParser.parseCalls.awaitItem()).containsKey("57")
    }

    @Test
    fun `readCard stops probing when parser can parse records`() = runScenario(
        parseResult = SCANNED_CARD_DATA,
        transceiveResults = listOf(
            apduSuccessResponse(tlv(tag = 0x4F, value = VISA_AID)),
            apduSuccessResponse(byteArrayOf()),
            apduSuccessResponse(tlv(tag = 0x57, value = TRACK_2_DATA)),
        ),
        transceiveResult = RECORD_NOT_FOUND_RESPONSE,
    ) {
        val result = cardReader.readCard(transceiver)

        assertThat(result.getOrNull()).isEqualTo(SCANNED_CARD_DATA)

        assertThat(transceiver.transceiveCalls.awaitItem()).isEqualTo(SELECT_PPSE_REQUEST)
        assertThat(transceiver.transceiveCalls.awaitItem()).isEqualTo(SELECT_VISA_APPLICATION_REQUEST)
        assertThat(transceiver.transceiveCalls.awaitItem()).isEqualTo(READ_RECORD_REQUESTS.first())

        assertThat(cardDataParser.canParseCalls.awaitItem()).containsKey("57")
        assertThat(cardDataParser.parseCalls.awaitItem()).containsKey("57")

        cardDataParser.canParseCalls.ensureAllEventsConsumed()
    }

    @Test
    fun `readCard merges tlv records from successful read record commands`() = runScenario(
        transceiveResults = listOf(
            apduSuccessResponse(tlv(tag = 0x4F, value = VISA_AID)),
            apduSuccessResponse(byteArrayOf()),
            apduSuccessResponse(tlv(tag = 0x5A, value = PAN_DATA)),
            apduSuccessResponse(tlv(tag = 0x5F, tagContinuation = 0x24, value = EXPIRY_DATA)),
        ),
        transceiveResult = RECORD_NOT_FOUND_RESPONSE,
    ) {
        val result = cardReader.readCard(transceiver)

        assertThat(result.exceptionOrNull()).isInstanceOf<IllegalStateException>()

        assertThat(transceiver.transceiveCalls.awaitItem()).isEqualTo(SELECT_PPSE_REQUEST)
        assertThat(transceiver.transceiveCalls.awaitItem()).isEqualTo(SELECT_VISA_APPLICATION_REQUEST)
        assertThat(transceiver.transceiveCalls.awaitItem()).isEqualTo(READ_RECORD_REQUESTS.first())
        assertThat(transceiver.transceiveCalls.awaitItem()).isEqualTo(READ_RECORD_REQUESTS[1])

        assertThat(cardDataParser.canParseCalls.awaitItem()).containsKey("5A")
        assertThat(cardDataParser.canParseCalls.awaitItem()).containsKey("5F24")

        val parsedRecords = cardDataParser.parseCalls.awaitItem()
        assertThat(parsedRecords).containsKey("5A")
        assertThat(parsedRecords.getValue("5A").contentEquals(PAN_DATA)).isTrue()
        assertThat(parsedRecords).containsKey("5F24")
        assertThat(parsedRecords.getValue("5F24").contentEquals(EXPIRY_DATA)).isTrue()
    }

    private fun runScenario(
        transceiveResult: ByteArray = apduSuccessResponse(tlv(tag = 0x4F, value = VISA_AID)),
        transceiveResults: List<ByteArray> = emptyList(),
        parseResult: ScannedCardData? = null,
        block: suspend Scenario.() -> Unit,
    ) = runTest {
        val fakeTransceiver = FakeNfcTagTransceiver(
            transceiveResult = transceiveResult,
            transceiveResults = transceiveResults,
        )
        val fakeCardDataParser = FakeNfcCardDataParser(
            parseResult = parseResult,
        )
        val reader = ApduCardReader(
            workContext = UnconfinedTestDispatcher(testScheduler),
            cardDataParser = fakeCardDataParser,
        )

        Scenario(
            cardReader = reader,
            transceiver = fakeTransceiver,
            cardDataParser = fakeCardDataParser,
        ).apply { block() }

        fakeTransceiver.ensureAllEventsConsumed()
        fakeCardDataParser.ensureAllEventsConsumed()
    }

    private class Scenario(
        val cardReader: ApduCardReader,
        val transceiver: FakeNfcTagTransceiver,
        val cardDataParser: FakeNfcCardDataParser,
    ) {
        suspend fun assertReadRecordProbes(startingAtIndex: Int = 0) {
            for (index in startingAtIndex until READ_RECORD_REQUESTS.size) {
                assertThat(transceiver.transceiveCalls.awaitItem())
                    .isEqualTo(READ_RECORD_REQUESTS[index])
            }
        }
    }

    private companion object {
        const val MAX_RECORDS_PER_SFI = 8

        val SCANNED_CARD_DATA = ScannedCardData(
            cardNumber = "4111111111111111",
            expirationMonth = 12,
            expirationYear = 2025,
        )

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

        val PAN_DATA = byteArrayOf(
            0x41,
            0x11,
            0x11,
            0x11,
            0x11,
            0x11,
            0x11,
            0x11,
        )

        val EXPIRY_DATA = byteArrayOf(
            0x25,
            0x12,
            0x01,
        )

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
