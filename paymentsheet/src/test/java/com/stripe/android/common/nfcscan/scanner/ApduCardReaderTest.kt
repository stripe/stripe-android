package com.stripe.android.common.nfcscan.scanner

import com.google.common.truth.Truth.assertThat
import com.stripe.android.common.nfcscan.scanner.apdu.ApduResponseError
import com.stripe.android.common.nfcscan.scanner.apdu.FakePdolBuilder
import com.stripe.android.common.nfcscan.scanner.apdu.GetProcessingOptionsCommand
import com.stripe.android.common.nfcscan.scanner.apdu.SelectApplicationCommand
import com.stripe.android.common.nfcscan.scanner.apdu.SelectPpseCommand
import com.stripe.android.common.nfcscan.scanner.apdu.apduSuccessResponse
import com.stripe.android.common.nfcscan.scanner.apdu.tlv
import com.stripe.android.core.strings.resolvableString
import com.stripe.android.isInstanceOf
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadata
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadataFactory
import com.stripe.android.paymentsheet.R
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Test
import java.io.IOException

internal class ApduCardReaderTest {
    @Test
    fun `readCard runs PPSE, select application, GPO and AFL-directed read record commands`() = runScenario(
        transceiveResults = listOf(
            apduSuccessResponse(tlv(tag = 0x4F, value = VISA_AID)),
            apduSuccessResponse(EMPTY_PDOL_SELECT_RESPONSE),
            apduSuccessResponse(
                tlv(tag = 0x77, value = tlv(tag = 0x94.toByte(), value = AFL_SFI_1_RECORDS_1_TO_1)),
            ),
            apduSuccessResponse(tlv(tag = 0x57, value = TRACK_2_DATA)),
        ),
        parseResult = SCANNED_CARD_DATA,
    ) {
        val result = cardReader.readCard(transceiver)

        assertThat(result).isEqualTo(NfcCardReader.Result.Found(SCANNED_CARD_DATA))

        assertThat(transceiver.transceiveCalls.awaitItem()).isEqualTo(SELECT_PPSE_REQUEST)
        assertThat(transceiver.transceiveCalls.awaitItem()).isEqualTo(SELECT_VISA_APPLICATION_REQUEST)
        assertThat(transceiver.transceiveCalls.awaitItem()).isEqualTo(GPO_REQUEST)
        assertThat(transceiver.transceiveCalls.awaitItem()).isEqualTo(READ_RECORD_SFI_1_RECORD_1_REQUEST)

        assertThat(cardDataParser.parseCalls.awaitItem()).containsKey("57")
    }

    @Test
    fun `readCard skips pdol builder when PDOL tag is absent`() = runScenario(
        transceiveResults = listOf(
            apduSuccessResponse(tlv(tag = 0x4F, value = VISA_AID)),
            apduSuccessResponse(byteArrayOf()),
            apduSuccessResponse(
                tlv(tag = 0x77, value = tlv(tag = 0x57, value = TRACK_2_DATA)),
            ),
        ),
        parseResult = SCANNED_CARD_DATA,
    ) {
        val result = cardReader.readCard(transceiver)

        assertThat(result).isEqualTo(NfcCardReader.Result.Found(SCANNED_CARD_DATA))

        assertThat(transceiver.transceiveCalls.awaitItem()).isEqualTo(SELECT_PPSE_REQUEST)
        assertThat(transceiver.transceiveCalls.awaitItem()).isEqualTo(SELECT_VISA_APPLICATION_REQUEST)
        assertThat(transceiver.transceiveCalls.awaitItem()).isEqualTo(GPO_REQUEST)

        assertThat(cardDataParser.parseCalls.awaitItem()).containsKey("57")
    }

    @Test
    fun `readCard parses Track 2 from GPO response without AFL read record commands`() = runScenario(
        transceiveResults = listOf(
            apduSuccessResponse(tlv(tag = 0x4F, value = VISA_AID)),
            apduSuccessResponse(EMPTY_PDOL_SELECT_RESPONSE),
            apduSuccessResponse(
                tlv(tag = 0x77, value = tlv(tag = 0x57, value = TRACK_2_DATA)),
            ),
        ),
        parseResult = SCANNED_CARD_DATA,
    ) {
        val result = cardReader.readCard(transceiver)

        assertThat(result).isEqualTo(NfcCardReader.Result.Found(SCANNED_CARD_DATA))

        assertThat(transceiver.transceiveCalls.awaitItem()).isEqualTo(SELECT_PPSE_REQUEST)
        assertThat(transceiver.transceiveCalls.awaitItem()).isEqualTo(SELECT_VISA_APPLICATION_REQUEST)
        assertThat(transceiver.transceiveCalls.awaitItem()).isEqualTo(GPO_REQUEST)

        assertThat(cardDataParser.parseCalls.awaitItem()).containsKey("57")
    }

    @Test
    fun `readCard builds pdol data when PDOL template is available`() = runScenario(
        transceiveResults = listOf(
            apduSuccessResponse(tlv(tag = 0x4F, value = VISA_AID)),
            apduSuccessResponse(
                tlv(tag = 0x9F.toByte(), tagContinuation = 0x38, value = PDOL_TEMPLATE),
            ),
            apduSuccessResponse(
                tlv(tag = 0x77, value = tlv(tag = 0x57, value = TRACK_2_DATA)),
            ),
        ),
        parseResult = SCANNED_CARD_DATA,
    ) {
        val result = cardReader.readCard(transceiver)

        assertThat(result).isEqualTo(NfcCardReader.Result.Found(SCANNED_CARD_DATA))

        assertThat(transceiver.transceiveCalls.awaitItem()).isEqualTo(SELECT_PPSE_REQUEST)
        assertThat(transceiver.transceiveCalls.awaitItem()).isEqualTo(SELECT_VISA_APPLICATION_REQUEST)

        val call = pdolBuilder.fromTemplateCalls.awaitItem()
        assertThat(call.paymentMethodMetadata).isEqualTo(paymentMethodMetadata)
        assertThat(call.template.contentEquals(PDOL_TEMPLATE)).isTrue()

        assertThat(transceiver.transceiveCalls.awaitItem()).isEqualTo(GPO_REQUEST)

        assertThat(cardDataParser.parseCalls.awaitItem()).containsKey("57")
    }

    @Test
    fun `readCard merges tlv records from AFL directed read record commands`() = runScenario(
        transceiveResults = listOf(
            apduSuccessResponse(tlv(tag = 0x4F, value = VISA_AID)),
            apduSuccessResponse(EMPTY_PDOL_SELECT_RESPONSE),
            apduSuccessResponse(
                tlv(tag = 0x77, value = tlv(tag = 0x94.toByte(), value = AFL_SFI_1_RECORDS_1_TO_2)),
            ),
            apduSuccessResponse(tlv(tag = 0x5A, value = PAN_DATA)),
            apduSuccessResponse(tlv(tag = 0x5F, tagContinuation = 0x24, value = EXPIRY_DATA)),
        ),
        parseResult = SCANNED_CARD_DATA,
    ) {
        val result = cardReader.readCard(transceiver)

        assertThat(result).isEqualTo(NfcCardReader.Result.Found(SCANNED_CARD_DATA))

        assertThat(transceiver.transceiveCalls.awaitItem()).isEqualTo(SELECT_PPSE_REQUEST)
        assertThat(transceiver.transceiveCalls.awaitItem()).isEqualTo(SELECT_VISA_APPLICATION_REQUEST)
        assertThat(transceiver.transceiveCalls.awaitItem()).isEqualTo(GPO_REQUEST)
        assertThat(transceiver.transceiveCalls.awaitItem()).isEqualTo(READ_RECORD_SFI_1_RECORD_1_REQUEST)
        assertThat(transceiver.transceiveCalls.awaitItem()).isEqualTo(READ_RECORD_SFI_1_RECORD_2_REQUEST)

        val parsedRecords = cardDataParser.parseCalls.awaitItem()
        assertThat(parsedRecords).containsKey("5A")
        assertThat(parsedRecords).containsKey("5F24")
    }

    @Test
    fun `readCard returns parser error when card data cannot be parsed`() = runScenario(
        transceiveResults = listOf(
            apduSuccessResponse(tlv(tag = 0x4F, value = VISA_AID)),
            apduSuccessResponse(EMPTY_PDOL_SELECT_RESPONSE),
            apduSuccessResponse(tlv(tag = 0x77, value = byteArrayOf())),
        ),
        parseError = UNSUPPORTED_CARD_ERROR,
    ) {
        val result = cardReader.readCard(transceiver)

        assertThat(result).isInstanceOf<NfcCardReader.Result.Error>()
        val readerError = result as NfcCardReader.Result.Error
        assertThat(readerError.error).isSameInstanceAs(UNSUPPORTED_CARD_ERROR)

        assertThat(transceiver.transceiveCalls.awaitItem()).isEqualTo(SELECT_PPSE_REQUEST)
        assertThat(transceiver.transceiveCalls.awaitItem()).isEqualTo(SELECT_VISA_APPLICATION_REQUEST)
        assertThat(transceiver.transceiveCalls.awaitItem()).isEqualTo(GPO_REQUEST)

        assertThat(cardDataParser.parseCalls.awaitItem()).isNotNull()
    }

    @Test
    fun `readCard propagates PPSE selection failure`() = runScenario(
        transceiveResults = emptyList(),
        transceiveResult = FILE_NOT_FOUND_RESPONSE,
    ) {
        val result = cardReader.readCard(transceiver)

        assertThat(result).isInstanceOf<NfcCardReader.Result.Error>()
        val readerError = result as NfcCardReader.Result.Error
        assertThat(readerError.error).isEqualTo(
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
    ) {
        val result = cardReader.readCard(transceiver)

        assertThat(result).isInstanceOf<NfcCardReader.Result.Error>()
        val readerError = result as NfcCardReader.Result.Error

        assertThat(readerError.error).isInstanceOf<ApduResponseError.Command>()
        val commandError = readerError.error as ApduResponseError.Command

        assertThat(commandError.apduCommand).isInstanceOf<SelectApplicationCommand>()
        assertThat(commandError.sw1).isEqualTo(0x6A.toByte())
        assertThat(commandError.sw2).isEqualTo(0x82.toByte())

        assertThat(transceiver.transceiveCalls.awaitItem()).isEqualTo(SELECT_PPSE_REQUEST)
        assertThat(transceiver.transceiveCalls.awaitItem()).isEqualTo(SELECT_VISA_APPLICATION_REQUEST)
    }

    @Test
    fun `readCard propagates GetProcessingOptions failure`() = runScenario(
        transceiveResults = listOf(
            apduSuccessResponse(tlv(tag = 0x4F, value = VISA_AID)),
            apduSuccessResponse(
                tlv(tag = 0x9F.toByte(), tagContinuation = 0x38, value = PDOL_TEMPLATE),
            ),
        ),
        transceiveResult = FILE_NOT_FOUND_RESPONSE,
    ) {
        val result = cardReader.readCard(transceiver)

        assertThat(result).isInstanceOf<NfcCardReader.Result.Error>()
        val readerError = result as NfcCardReader.Result.Error
        val commandError = readerError.error as ApduResponseError.Command
        assertThat(commandError.errorCode).isEqualTo("nfcCardReadFailed")
        assertThat(commandError.apduCommand).isInstanceOf<GetProcessingOptionsCommand>()
        assertThat(commandError.sw1).isEqualTo(0x6A.toByte())
        assertThat(commandError.sw2).isEqualTo(0x82.toByte())

        assertThat(transceiver.transceiveCalls.awaitItem()).isEqualTo(SELECT_PPSE_REQUEST)
        assertThat(transceiver.transceiveCalls.awaitItem()).isEqualTo(SELECT_VISA_APPLICATION_REQUEST)

        val call = pdolBuilder.fromTemplateCalls.awaitItem()
        assertThat(call.paymentMethodMetadata).isEqualTo(paymentMethodMetadata)
        assertThat(call.template.contentEquals(PDOL_TEMPLATE)).isTrue()

        assertThat(transceiver.transceiveCalls.awaitItem()).isEqualTo(GPO_REQUEST)
    }

    @Test
    fun `readCard returns transceiver io error when open fails`() = runScenario(
        openException = IOException("open failed"),
    ) {
        val result = cardReader.readCard(transceiver)

        assertThat(result).isInstanceOf<NfcCardReader.Result.Error>()
        val readerError = result as NfcCardReader.Result.Error
        val scanningError = readerError.error as GenericNfcScanningError
        assertThat(scanningError.errorCode).isEqualTo("nfcTransceiverIoError")
        assertThat(scanningError.userMessage).isEqualTo(
            R.string.stripe_tap_to_add_card_default_error_action.resolvableString,
        )
    }

    @Test
    fun `readCard returns transceiver io error when transceive fails`() = runScenario(
        transceiveException = IOException("transceive failed"),
    ) {
        val result = cardReader.readCard(transceiver)

        assertThat(result).isInstanceOf<NfcCardReader.Result.Error>()
        val readerError = result as NfcCardReader.Result.Error
        val scanningError = readerError.error as GenericNfcScanningError
        assertThat(scanningError.errorCode).isEqualTo("nfcTransceiverIoError")
        assertThat(scanningError.userMessage).isEqualTo(
            R.string.stripe_tap_to_add_card_default_error_action.resolvableString,
        )

        assertThat(transceiver.transceiveCalls.awaitItem()).isEqualTo(SELECT_PPSE_REQUEST)
    }

    @Test
    fun `readCard returns transceiver security error when open fails with SecurityException`() = runScenario(
        openException = SecurityException("NFC access denied"),
    ) {
        val result = cardReader.readCard(transceiver)

        assertThat(result).isInstanceOf<NfcCardReader.Result.Error>()
        val readerError = result as NfcCardReader.Result.Error
        val scanningError = readerError.error as GenericNfcScanningError
        assertThat(scanningError.errorCode).isEqualTo("nfcTransceiverSecurityError")
        assertThat(scanningError.userMessage).isEqualTo(
            R.string.stripe_tap_to_add_card_default_error_action.resolvableString,
        )
    }

    @Test
    fun `readCard passes through unrecognized throwable`() = runScenario(
        openException = RuntimeException("unexpected"),
    ) {
        val result = cardReader.readCard(transceiver)

        assertThat(result).isInstanceOf<NfcCardReader.Result.Error>()
        val readerError = result as NfcCardReader.Result.Error
        assertThat(readerError.error).isInstanceOf<RuntimeException>()
    }

    private fun runScenario(
        transceiveResult: ByteArray = apduSuccessResponse(tlv(tag = 0x4F, value = VISA_AID)),
        transceiveResults: List<ByteArray> = listOf(
            apduSuccessResponse(tlv(tag = 0x4F, value = VISA_AID))
        ),
        paymentMethodMetadata: PaymentMethodMetadata = PaymentMethodMetadataFactory.create(),
        pdolData: ByteArray = byteArrayOf(),
        parseResult: ScannedCardData = SCANNED_CARD_DATA,
        parseError: NfcScanningError? = null,
        openException: Throwable? = null,
        transceiveException: Throwable? = null,
        block: suspend Scenario.() -> Unit,
    ) = runTest {
        val fakeTransceiver = FakeNfcTagTransceiver(
            transceiveResult = transceiveResult,
            transceiveResults = transceiveResults,
            openException = openException,
            transceiveException = transceiveException,
        )
        val fakeCardDataParser = FakeNfcCardDataParser(
            parseResult = parseResult,
            parseError = parseError,
        )
        val fakePdolBuilder = FakePdolBuilder(
            pdolData = pdolData,
        )
        val reader = ApduCardReader(
            workContext = UnconfinedTestDispatcher(testScheduler),
            paymentMethodMetadata = paymentMethodMetadata,
            pdolBuilder = fakePdolBuilder,
            cardDataParser = fakeCardDataParser,
        )

        Scenario(
            cardReader = reader,
            transceiver = fakeTransceiver,
            cardDataParser = fakeCardDataParser,
            pdolBuilder = fakePdolBuilder,
            paymentMethodMetadata = paymentMethodMetadata,
        ).apply { block() }

        fakeTransceiver.openCalls.awaitItem()
        fakeTransceiver.closeCalls.awaitItem()
        fakeTransceiver.ensureAllEventsConsumed()
        fakeCardDataParser.ensureAllEventsConsumed()
        fakePdolBuilder.ensureAllEventsConsumed()
    }

    private class Scenario(
        val cardReader: ApduCardReader,
        val transceiver: FakeNfcTagTransceiver,
        val cardDataParser: FakeNfcCardDataParser,
        val pdolBuilder: FakePdolBuilder,
        val paymentMethodMetadata: PaymentMethodMetadata,
    )

    private companion object {
        val UNSUPPORTED_CARD_ERROR = GenericNfcScanningError(
            errorCode = "cardUnsupportedByNfc",
            userMessage = R.string.stripe_nfc_scan_unsupported_card.resolvableString,
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

        val AFL_SFI_1_RECORDS_1_TO_1 = byteArrayOf(0x08, 0x01, 0x01, 0x00)
        val AFL_SFI_1_RECORDS_1_TO_2 = byteArrayOf(0x08, 0x01, 0x02, 0x00)

        val READ_RECORD_SFI_1_RECORD_1_REQUEST = byteArrayOf(
            0x00,
            0xB2.toByte(),
            0x01,
            0x0C,
            0x00,
        )

        val READ_RECORD_SFI_1_RECORD_2_REQUEST = byteArrayOf(
            0x00,
            0xB2.toByte(),
            0x02,
            0x0C,
            0x00,
        )

        val EMPTY_PDOL_SELECT_RESPONSE = tlv(
            tag = 0x9F.toByte(),
            tagContinuation = 0x38,
            value = byteArrayOf(),
        )

        val PDOL_TEMPLATE = byteArrayOf(
            0x9F.toByte(), 0x66, 0x04,
        )

        val GPO_REQUEST = byteArrayOf(
            0x80.toByte(),
            0xA8.toByte(),
            0x00,
            0x00,
            0x02,
            0x83.toByte(),
            0x00,
            0x00,
        )

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
