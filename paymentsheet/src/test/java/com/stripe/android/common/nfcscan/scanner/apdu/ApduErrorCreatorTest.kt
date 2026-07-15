package com.stripe.android.common.nfcscan.scanner.apdu

import com.google.common.truth.Truth.assertThat
import com.stripe.android.common.nfcscan.scanner.NfcCardReader
import com.stripe.android.core.strings.resolvableString
import com.stripe.android.paymentsheet.R
import org.junit.Test
import java.io.IOException

internal class ApduErrorCreatorTest {
    private val errorMapper = ApduErrorCreator()

    @Test
    fun `create returns unsupported card error for SelectPpse file not found`() {
        val result = errorMapper.create(
            ApduResponseError.Command(
                apduCommand = SelectPpseCommand,
                sw1 = 0x6A.toByte(),
                sw2 = 0x82.toByte(),
            ),
        )

        assertThat(result.errorCode).isEqualTo("cardUnsupportedByNfc")
        assertThat(result.userMessage).isEqualTo(
            R.string.stripe_nfc_scan_unsupported_card.resolvableString,
        )
    }

    @Test
    fun `create returns unsupported card error for SelectApplication file not found`() {
        val result = errorMapper.create(
            ApduResponseError.Command(
                apduCommand = SelectApplicationCommand(ApplicationIdentifier("A0000000031010")),
                sw1 = 0x6A.toByte(),
                sw2 = 0x82.toByte(),
            ),
        )

        assertThat(result.errorCode).isEqualTo("cardUnsupportedByNfc")
        assertThat(result.userMessage).isEqualTo(
            R.string.stripe_nfc_scan_unsupported_card.resolvableString,
        )
    }

    @Test
    fun `create returns declined card error for conditions of use not satisfied`() {
        val result = errorMapper.create(
            ApduResponseError.Command(
                apduCommand = SelectApplicationCommand(ApplicationIdentifier("A0000000031010")),
                sw1 = 0x69.toByte(),
                sw2 = 0x85.toByte(),
            ),
        )

        assertThat(result.errorCode).isEqualTo("cardDeclinedByNfc")
        assertThat(result.userMessage).isEqualTo(
            R.string.stripe_nfc_scan_error_declined_card.resolvableString,
        )
    }

    @Test
    fun `create returns unsupported card error for invalid PPSE response`() {
        val result = errorMapper.create(
            ApduResponseError.Invalid(data = byteArrayOf(0x01)),
        )

        assertThat(result.errorCode).isEqualTo("cardUnsupportedByNfc")
        assertThat(result.userMessage).isEqualTo(
            R.string.stripe_nfc_scan_unsupported_card.resolvableString,
        )
    }

    @Test
    fun `create returns general failure for unknown status word`() {
        val result = errorMapper.create(
            ApduResponseError.Command(
                apduCommand = ReadRecordCommand(recordNumber = 1, shortFileIdentifier = 1),
                sw1 = 0x64.toByte(),
                sw2 = 0x00,
            ),
        )

        assertThat(result.errorCode).isEqualTo("nfcCardReadFailed")
        assertThat(result.userMessage).isEqualTo(
            R.string.stripe_tap_to_add_card_default_error_action.resolvableString,
        )
    }

    @Test
    fun `create returns transceiver security error for SecurityException`() {
        val result = errorMapper.create(SecurityException("NFC access denied"))

        assertThat(result).isEqualTo(
            NfcCardReader.Result.Error(
                errorCode = "nfcTransceiverSecurityError",
                userMessage = R.string.stripe_tap_to_add_card_default_error_action.resolvableString,
            ),
        )
    }

    @Test
    fun `create returns transceiver io error for IOException`() {
        val result = errorMapper.create(IOException("transceive failed"))

        assertThat(result).isEqualTo(
            NfcCardReader.Result.Error(
                errorCode = "nfcTransceiverIoError",
                userMessage = R.string.stripe_tap_to_add_card_default_error_action.resolvableString,
            ),
        )
    }

    @Test
    fun `create returns too short error for ApduResponseError TooShort`() {
        val result = errorMapper.create(ApduResponseError.TooShort())

        assertThat(result).isEqualTo(
            NfcCardReader.Result.Error(
                errorCode = "apduResponseTooShort",
                userMessage = R.string.stripe_something_went_wrong.resolvableString,
            ),
        )
    }

    @Test
    fun `create returns parsing error for ApduResponseError Parsing`() {
        val result = errorMapper.create(
            ApduResponseError.Parsing(
                data = byteArrayOf(0x01, 0x02),
                cause = IllegalArgumentException("invalid tlv"),
            ),
        )

        assertThat(result).isEqualTo(
            NfcCardReader.Result.Error(
                errorCode = "apduParsingCode",
                userMessage = R.string.stripe_something_went_wrong.resolvableString,
            ),
        )
    }

    @Test
    fun `create returns unknown error for unrecognized throwable`() {
        val result = errorMapper.create(RuntimeException("unexpected"))

        assertThat(result).isEqualTo(
            NfcCardReader.Result.Error(
                errorCode = "unknownNfcReaderError",
                userMessage = R.string.stripe_tap_to_add_card_default_error_action.resolvableString,
            ),
        )
    }

    @Test
    fun `create returns general failure for IllegalStateException`() {
        val result = errorMapper.create(IllegalStateException("Could not parse card data from NFC tag"))

        assertThat(result).isEqualTo(
            NfcCardReader.Result.Error(
                errorCode = "nfcCardReadFailed",
                userMessage = R.string.stripe_tap_to_add_card_default_error_action.resolvableString,
            ),
        )
    }
}
