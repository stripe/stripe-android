package com.stripe.android.common.nfcscan.scanner

import com.google.common.truth.Truth.assertThat
import com.stripe.android.common.nfcscan.scanner.apdu.ApduResponseError
import com.stripe.android.common.nfcscan.scanner.apdu.GetProcessingOptionsCommand
import com.stripe.android.common.nfcscan.scanner.apdu.SelectApplicationCommand
import com.stripe.android.common.nfcscan.scanner.apdu.SelectPpseCommand
import com.stripe.android.common.nfcscan.scanner.apdu.apduSuccessResponse
import com.stripe.android.common.nfcscan.scanner.apdu.tlv
import com.stripe.android.core.strings.resolvableString
import com.stripe.android.isInstanceOf
import com.stripe.android.paymentsheet.R
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Test
import java.io.IOException

internal class ApduCardReaderTest {
    @Test
    fun `readCard selects PPSE then application then probes records`() = runScenario(
        transceiveResults = listOf(
            apduSuccessResponse(tlv(tag = 0x4F, value = VISA_AID)),
            apduSuccessResponse(byteArrayOf()),
            GPO_WITHOUT_AFL_RESPONSE,
        ),
        transceiveResult = RECORD_NOT_FOUND_RESPONSE,
        errorResult = PARSE_FAILURE_ERROR,
    ) {
        val result = cardReader.readCard(transceiver)

        assertThat(result).isEqualTo(PARSE_FAILURE_ERROR)
        assertThat(errorCreator.createCalls.awaitItem()).isInstanceOf<IllegalStateException>()

        assertThat(transceiver.transceiveCalls.awaitItem()).isEqualTo(SELECT_PPSE_REQUEST)
        assertThat(transceiver.transceiveCalls.awaitItem()).isEqualTo(SELECT_VISA_APPLICATION_REQUEST)
        assertThat(transceiver.transceiveCalls.awaitItem()).isEqualTo(GET_PROCESSING_OPTIONS_WITHOUT_PDOL_REQUEST)

        assertReadRecordProbes()

        assertThat(cardDataParser.parseCalls.awaitItem()).isNotNull()
    }

    @Test
    fun `readCard skips remaining records in SFI when read record returns file not found`() = runScenario(
        transceiveResults = listOf(
            apduSuccessResponse(tlv(tag = 0x4F, value = VISA_AID)),
            apduSuccessResponse(byteArrayOf()),
            GPO_WITHOUT_AFL_RESPONSE,
            FILE_NOT_FOUND_RESPONSE,
        ),
        transceiveResult = RECORD_NOT_FOUND_RESPONSE,
        errorResult = PARSE_FAILURE_ERROR,
    ) {
        val result = cardReader.readCard(transceiver)

        assertThat(result).isEqualTo(PARSE_FAILURE_ERROR)
        assertThat(errorCreator.createCalls.awaitItem()).isInstanceOf<IllegalStateException>()

        assertThat(transceiver.transceiveCalls.awaitItem()).isEqualTo(SELECT_PPSE_REQUEST)
        assertThat(transceiver.transceiveCalls.awaitItem()).isEqualTo(SELECT_VISA_APPLICATION_REQUEST)
        assertThat(transceiver.transceiveCalls.awaitItem()).isEqualTo(GET_PROCESSING_OPTIONS_WITHOUT_PDOL_REQUEST)

        assertThat(transceiver.transceiveCalls.awaitItem()).isEqualTo(READ_RECORD_REQUESTS.first())
        assertReadRecordProbes(startingAtIndex = MAX_RECORDS_PER_SFI)

        assertThat(cardDataParser.parseCalls.awaitItem()).isNotNull()
    }

    @Test
    fun `readCard finds card data on later SFI when earlier SFI returns file not found`() = runScenario(
        parseResult = SCANNED_CARD_DATA,
        transceiveResults = listOf(
            apduSuccessResponse(tlv(tag = 0x4F, value = VISA_AID)),
            apduSuccessResponse(byteArrayOf()),
            GPO_WITHOUT_AFL_RESPONSE,
            FILE_NOT_FOUND_RESPONSE,
            apduSuccessResponse(tlv(tag = 0x57, value = TRACK_2_DATA)),
        ),
        transceiveResult = RECORD_NOT_FOUND_RESPONSE,
    ) {
        val result = cardReader.readCard(transceiver)

        assertThat(result).isEqualTo(NfcCardReader.Result.Found(SCANNED_CARD_DATA))

        assertThat(transceiver.transceiveCalls.awaitItem()).isEqualTo(SELECT_PPSE_REQUEST)
        assertThat(transceiver.transceiveCalls.awaitItem()).isEqualTo(SELECT_VISA_APPLICATION_REQUEST)
        assertThat(transceiver.transceiveCalls.awaitItem()).isEqualTo(GET_PROCESSING_OPTIONS_WITHOUT_PDOL_REQUEST)
        assertThat(transceiver.transceiveCalls.awaitItem()).isEqualTo(READ_RECORD_REQUESTS.first())
        assertThat(transceiver.transceiveCalls.awaitItem()).isEqualTo(READ_RECORD_REQUESTS[MAX_RECORDS_PER_SFI])

        assertThat(cardDataParser.canParseCalls.awaitItem()).containsKey("57")
        assertThat(cardDataParser.parseCalls.awaitItem()).containsKey("57")

        cardDataParser.canParseCalls.ensureAllEventsConsumed()
    }

    @Test
    fun `readCard continues probing when read record returns declined`() = runScenario(
        transceiveResults = listOf(
            apduSuccessResponse(tlv(tag = 0x4F, value = VISA_AID)),
            apduSuccessResponse(byteArrayOf()),
            GPO_WITHOUT_AFL_RESPONSE,
        ),
        transceiveResult = CARD_DECLINED_RESPONSE,
        errorResult = PARSE_FAILURE_ERROR,
    ) {
        val result = cardReader.readCard(transceiver)

        assertThat(result).isEqualTo(PARSE_FAILURE_ERROR)
        assertThat(errorCreator.createCalls.awaitItem()).isInstanceOf<IllegalStateException>()

        assertThat(transceiver.transceiveCalls.awaitItem()).isEqualTo(SELECT_PPSE_REQUEST)
        assertThat(transceiver.transceiveCalls.awaitItem()).isEqualTo(SELECT_VISA_APPLICATION_REQUEST)
        assertThat(transceiver.transceiveCalls.awaitItem()).isEqualTo(GET_PROCESSING_OPTIONS_WITHOUT_PDOL_REQUEST)

        assertReadRecordProbes()

        assertThat(cardDataParser.parseCalls.awaitItem()).isNotNull()
    }

    @Test
    fun `readCard propagates PPSE selection failure`() = runScenario(
        transceiveResults = emptyList(),
        transceiveResult = FILE_NOT_FOUND_RESPONSE,
        errorResult = UNSUPPORTED_CARD_ERROR,
    ) {
        val result = cardReader.readCard(transceiver)

        assertThat(result).isEqualTo(UNSUPPORTED_CARD_ERROR)
        assertThat(errorCreator.createCalls.awaitItem()).isEqualTo(
            ApduResponseError.Command(
                apduCommand = SelectPpseCommand,
                sw1 = 0x6A.toByte(),
                sw2 = 0x82.toByte(),
            ),
        )
        assertThat(transceiver.transceiveCalls.awaitItem()).isEqualTo(SELECT_PPSE_REQUEST)
    }

    @Test
    fun `readCard propagates SelectApplication failure`() = runScenario(
        transceiveResults = listOf(
            apduSuccessResponse(tlv(tag = 0x4F, value = VISA_AID)),
        ),
        transceiveResult = FILE_NOT_FOUND_RESPONSE,
        errorResult = UNSUPPORTED_CARD_ERROR,
    ) {
        val result = cardReader.readCard(transceiver)

        assertThat(result).isEqualTo(UNSUPPORTED_CARD_ERROR)

        val error = errorCreator.createCalls.awaitItem()

        assertThat(error).isInstanceOf<ApduResponseError.Command>()
        val commandError = error as ApduResponseError.Command

        assertThat(commandError.apduCommand).isInstanceOf<SelectApplicationCommand>()
        assertThat(commandError.sw1).isEqualTo(0x6A.toByte())
        assertThat(commandError.sw2).isEqualTo(0x82.toByte())

        assertThat(transceiver.transceiveCalls.awaitItem()).isEqualTo(SELECT_PPSE_REQUEST)
        assertThat(transceiver.transceiveCalls.awaitItem()).isEqualTo(SELECT_VISA_APPLICATION_REQUEST)
    }

    @Test
    fun `readCard gets processing options and reads AFL records`() = runScenario(
        parseResult = SCANNED_CARD_DATA,
        transceiveResults = listOf(
            apduSuccessResponse(tlv(tag = 0x4F, value = VISA_AID)),
            apduSuccessResponse(tlv(tag = 0x9F.toByte(), tagContinuation = 0x38, value = PDOL_WITH_TTQ)),
            GPO_WITH_AFL_RESPONSE,
            apduSuccessResponse(tlv(tag = 0x57, value = TRACK_2_DATA)),
        ),
        transceiveResult = RECORD_NOT_FOUND_RESPONSE,
    ) {
        val result = cardReader.readCard(transceiver)

        assertThat(result).isEqualTo(NfcCardReader.Result.Found(SCANNED_CARD_DATA))

        assertThat(transceiver.transceiveCalls.awaitItem()).isEqualTo(SELECT_PPSE_REQUEST)
        assertThat(transceiver.transceiveCalls.awaitItem()).isEqualTo(SELECT_VISA_APPLICATION_REQUEST)
        assertThat(transceiver.transceiveCalls.awaitItem()).isEqualTo(GET_PROCESSING_OPTIONS_WITH_TTQ_REQUEST)
        assertThat(transceiver.transceiveCalls.awaitItem()).isEqualTo(READ_SFI_2_RECORD_3_REQUEST)

        assertThat(cardDataParser.canParseCalls.awaitItem()).containsKey("57")
        assertThat(cardDataParser.parseCalls.awaitItem()).containsKey("57")

        cardDataParser.canParseCalls.ensureAllEventsConsumed()
    }

    @Test
    fun `readCard parses card data returned by get processing options`() = runScenario(
        parseResult = SCANNED_CARD_DATA,
        transceiveResults = listOf(
            apduSuccessResponse(tlv(tag = 0x4F, value = VISA_AID)),
            apduSuccessResponse(tlv(tag = 0x9F.toByte(), tagContinuation = 0x38, value = PDOL_WITH_TTQ)),
            GPO_WITH_AFL_AND_TRACK_2_RESPONSE,
            apduSuccessResponse(tlv(tag = 0x9F.toByte(), tagContinuation = 0x69, value = CARD_AUTH_DATA)),
        ),
        transceiveResult = RECORD_NOT_FOUND_RESPONSE,
    ) {
        val result = cardReader.readCard(transceiver)

        assertThat(result).isEqualTo(NfcCardReader.Result.Found(SCANNED_CARD_DATA))

        assertThat(transceiver.transceiveCalls.awaitItem()).isEqualTo(SELECT_PPSE_REQUEST)
        assertThat(transceiver.transceiveCalls.awaitItem()).isEqualTo(SELECT_VISA_APPLICATION_REQUEST)
        assertThat(transceiver.transceiveCalls.awaitItem()).isEqualTo(GET_PROCESSING_OPTIONS_WITH_TTQ_REQUEST)
        assertThat(transceiver.transceiveCalls.awaitItem()).isEqualTo(READ_SFI_2_RECORD_3_REQUEST)

        val recordsWithGpoData = cardDataParser.canParseCalls.awaitItem()
        assertThat(recordsWithGpoData).containsKey("57")
        assertThat(recordsWithGpoData).containsKey("9F69")

        val parsedRecords = cardDataParser.parseCalls.awaitItem()
        assertThat(parsedRecords).containsKey("57")
        assertThat(parsedRecords.getValue("57").contentEquals(TRACK_2_DATA)).isTrue()
    }

    @Test
    fun `readCard propagates get processing options failure`() = runScenario(
        transceiveResults = listOf(
            apduSuccessResponse(tlv(tag = 0x4F, value = VISA_AID)),
            apduSuccessResponse(byteArrayOf()),
        ),
        transceiveResult = CARD_DECLINED_RESPONSE,
        errorResult = PARSE_FAILURE_ERROR,
    ) {
        val result = cardReader.readCard(transceiver)

        assertThat(result).isEqualTo(PARSE_FAILURE_ERROR)

        val error = errorCreator.createCalls.awaitItem()

        assertThat(error).isInstanceOf<ApduResponseError.Command>()
        val commandError = error as ApduResponseError.Command

        assertThat(commandError.apduCommand).isInstanceOf<GetProcessingOptionsCommand>()
        assertThat(commandError.sw1).isEqualTo(0x69.toByte())
        assertThat(commandError.sw2).isEqualTo(0x85.toByte())

        assertThat(transceiver.transceiveCalls.awaitItem()).isEqualTo(SELECT_PPSE_REQUEST)
        assertThat(transceiver.transceiveCalls.awaitItem()).isEqualTo(SELECT_VISA_APPLICATION_REQUEST)
        assertThat(transceiver.transceiveCalls.awaitItem()).isEqualTo(GET_PROCESSING_OPTIONS_WITHOUT_PDOL_REQUEST)
    }

    @Test
    fun `readCard returns parsed card data when parser succeeds`() = runScenario(
        parseResult = SCANNED_CARD_DATA,
        transceiveResults = listOf(
            apduSuccessResponse(tlv(tag = 0x4F, value = VISA_AID)),
            apduSuccessResponse(byteArrayOf()),
            GPO_WITHOUT_AFL_RESPONSE,
            apduSuccessResponse(tlv(tag = 0x57, value = TRACK_2_DATA)),
        ),
        transceiveResult = RECORD_NOT_FOUND_RESPONSE,
    ) {
        val result = cardReader.readCard(transceiver)

        assertThat(result).isEqualTo(NfcCardReader.Result.Found(SCANNED_CARD_DATA))

        assertThat(transceiver.transceiveCalls.awaitItem()).isEqualTo(SELECT_PPSE_REQUEST)
        assertThat(transceiver.transceiveCalls.awaitItem()).isEqualTo(SELECT_VISA_APPLICATION_REQUEST)
        assertThat(transceiver.transceiveCalls.awaitItem()).isEqualTo(GET_PROCESSING_OPTIONS_WITHOUT_PDOL_REQUEST)
        assertThat(transceiver.transceiveCalls.awaitItem()).isEqualTo(READ_RECORD_REQUESTS.first())

        assertThat(cardDataParser.parseCalls.awaitItem()).containsKey("57")
    }

    @Test
    fun `readCard stops probing when parser can parse records`() = runScenario(
        parseResult = SCANNED_CARD_DATA,
        transceiveResults = listOf(
            apduSuccessResponse(tlv(tag = 0x4F, value = VISA_AID)),
            apduSuccessResponse(byteArrayOf()),
            GPO_WITHOUT_AFL_RESPONSE,
            apduSuccessResponse(tlv(tag = 0x57, value = TRACK_2_DATA)),
        ),
        transceiveResult = RECORD_NOT_FOUND_RESPONSE,
    ) {
        val result = cardReader.readCard(transceiver)

        assertThat(result).isEqualTo(NfcCardReader.Result.Found(SCANNED_CARD_DATA))

        assertThat(transceiver.transceiveCalls.awaitItem()).isEqualTo(SELECT_PPSE_REQUEST)
        assertThat(transceiver.transceiveCalls.awaitItem()).isEqualTo(SELECT_VISA_APPLICATION_REQUEST)
        assertThat(transceiver.transceiveCalls.awaitItem()).isEqualTo(GET_PROCESSING_OPTIONS_WITHOUT_PDOL_REQUEST)
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
            GPO_WITHOUT_AFL_RESPONSE,
            apduSuccessResponse(tlv(tag = 0x5A, value = PAN_DATA)),
            apduSuccessResponse(tlv(tag = 0x5F, tagContinuation = 0x24, value = EXPIRY_DATA)),
        ),
        transceiveResult = RECORD_NOT_FOUND_RESPONSE,
        errorResult = PARSE_FAILURE_ERROR,
    ) {
        val result = cardReader.readCard(transceiver)

        assertThat(result).isEqualTo(PARSE_FAILURE_ERROR)
        assertThat(errorCreator.createCalls.awaitItem())
            .isInstanceOf<IllegalStateException>()

        assertThat(transceiver.transceiveCalls.awaitItem()).isEqualTo(SELECT_PPSE_REQUEST)
        assertThat(transceiver.transceiveCalls.awaitItem()).isEqualTo(SELECT_VISA_APPLICATION_REQUEST)
        assertThat(transceiver.transceiveCalls.awaitItem()).isEqualTo(GET_PROCESSING_OPTIONS_WITHOUT_PDOL_REQUEST)
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

    @Test
    fun `readCard returns transceiver io error when open fails`() = runScenario(
        openException = IOException("open failed"),
        errorResult = TRANSCEIVER_IO_ERROR,
    ) {
        val result = cardReader.readCard(transceiver)

        assertThat(result).isEqualTo(TRANSCEIVER_IO_ERROR)
        val error = errorCreator.createCalls.awaitItem()
        assertThat(error).isInstanceOf<IOException>()
        assertThat(error.message).isEqualTo("open failed")
    }

    private fun runScenario(
        transceiveResult: ByteArray = apduSuccessResponse(tlv(tag = 0x4F, value = VISA_AID)),
        transceiveResults: List<ByteArray> = listOf(
            apduSuccessResponse(tlv(tag = 0x4F, value = VISA_AID))
        ),
        parseResult: ScannedCardData? = null,
        openException: Throwable? = null,
        errorResult: NfcCardReader.Result.Error = PARSE_FAILURE_ERROR,
        block: suspend Scenario.() -> Unit,
    ) = runTest {
        val fakeTransceiver = FakeNfcTagTransceiver(
            transceiveResult = transceiveResult,
            transceiveResults = transceiveResults,
            openException = openException,
        )
        val fakeCardDataParser = FakeNfcCardDataParser(
            parseResult = parseResult,
        )
        val fakeErrorCreator = FakeNfcCardReaderErrorCreator(
            result = errorResult,
        )
        val reader = ApduCardReader(
            workContext = UnconfinedTestDispatcher(testScheduler),
            errorMapper = fakeErrorCreator,
            cardDataParser = fakeCardDataParser,
        )

        Scenario(
            cardReader = reader,
            transceiver = fakeTransceiver,
            cardDataParser = fakeCardDataParser,
            errorCreator = fakeErrorCreator,
        ).apply { block() }

        fakeTransceiver.openCalls.awaitItem()
        fakeTransceiver.closeCalls.awaitItem()
        fakeTransceiver.ensureAllEventsConsumed()
        fakeCardDataParser.ensureAllEventsConsumed()
        fakeErrorCreator.ensureAllEventsConsumed()
    }

    private class Scenario(
        val cardReader: ApduCardReader,
        val transceiver: FakeNfcTagTransceiver,
        val cardDataParser: FakeNfcCardDataParser,
        val errorCreator: FakeNfcCardReaderErrorCreator,
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

        val PARSE_FAILURE_ERROR = NfcCardReader.Result.Error(
            errorCode = "nfcCardReadFailed",
            userMessage = R.string.stripe_tap_to_add_card_default_error_action.resolvableString,
        )

        val UNSUPPORTED_CARD_ERROR = NfcCardReader.Result.Error(
            errorCode = "cardUnsupportedByNfc",
            userMessage = R.string.stripe_nfc_scan_unsupported_card.resolvableString,
        )

        val TRANSCEIVER_IO_ERROR = NfcCardReader.Result.Error(
            errorCode = "nfcTransceiverIoError",
            userMessage = R.string.stripe_tap_to_add_card_default_error_action.resolvableString,
        )

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
        val CARD_AUTH_DATA = byteArrayOf(
            0x12,
            0x34,
            0x56,
            0x78,
            0x90.toByte(),
            0x12,
            0x34,
        )

        val READ_RECORD_REQUESTS = buildList {
            for (sfi in 1..8) {
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
        val FILE_NOT_FOUND_RESPONSE = byteArrayOf(0x6A.toByte(), 0x82.toByte())
        val CARD_DECLINED_RESPONSE = byteArrayOf(0x69.toByte(), 0x85.toByte())
        val GPO_WITHOUT_AFL_RESPONSE = apduSuccessResponse(
            tlv(tag = 0x80.toByte(), value = byteArrayOf(0x00, 0x00)),
        )
        val PDOL_WITH_TTQ = byteArrayOf(0x9F.toByte(), 0x66, 0x04)
        val AFL_SFI_2_RECORD_3 = byteArrayOf(0x10, 0x03, 0x03, 0x00)
        val GPO_WITH_AFL_RESPONSE = apduSuccessResponse(
            tlv(tag = 0x77, value = tlv(tag = 0x94.toByte(), value = AFL_SFI_2_RECORD_3)),
        )
        val GPO_WITH_AFL_AND_TRACK_2_RESPONSE = apduSuccessResponse(
            tlv(
                tag = 0x77,
                value = tlv(tag = 0x94.toByte(), value = AFL_SFI_2_RECORD_3) +
                    tlv(tag = 0x57, value = TRACK_2_DATA),
            ),
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
        val GET_PROCESSING_OPTIONS_WITHOUT_PDOL_REQUEST = byteArrayOf(
            0x80.toByte(),
            0xA8.toByte(),
            0x00,
            0x00,
            0x02,
            0x83.toByte(),
            0x00,
            0x00,
        )
        val GET_PROCESSING_OPTIONS_WITH_TTQ_REQUEST = byteArrayOf(
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
        )
        val READ_SFI_2_RECORD_3_REQUEST = byteArrayOf(
            0x00,
            0xB2.toByte(),
            0x03,
            0x14,
            0x00,
        )
    }
}
