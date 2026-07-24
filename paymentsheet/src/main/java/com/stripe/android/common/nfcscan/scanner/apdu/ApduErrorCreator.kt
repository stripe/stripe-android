package com.stripe.android.common.nfcscan.scanner.apdu

import com.stripe.android.common.nfcscan.scanner.NfcCardReader
import com.stripe.android.core.strings.resolvableString
import com.stripe.android.paymentsheet.R
import java.io.IOException
import javax.inject.Inject

internal class ApduErrorCreator @Inject constructor() : NfcCardReader.ErrorCreator {
    override fun create(error: Throwable): NfcCardReader.Result.Error {
        val defaultAction = R.string.stripe_tap_to_add_card_default_error_action.resolvableString

        return when (error) {
            is SecurityException -> NfcCardReader.Result.Error(
                errorCode = TRANSCEIVER_SECURITY_ERROR_CODE,
                userMessage = defaultAction,
            )
            is IOException -> NfcCardReader.Result.Error(
                errorCode = TRANSCEIVER_IO_ERROR_CODE,
                userMessage = defaultAction,
            )
            is ApduResponseError.TooShort -> NfcCardReader.Result.Error(
                errorCode = APDU_RESPONSE_TOO_SHORT_ERROR_CODE,
                userMessage = R.string.stripe_something_went_wrong.resolvableString,
            )
            is ApduResponseError.Parsing -> NfcCardReader.Result.Error(
                errorCode = APDU_RESPONSE_PARSING_ERROR_CODE,
                userMessage = R.string.stripe_something_went_wrong.resolvableString,
            )
            is ApduResponseError.Invalid -> unsupportedCard()
            is ApduResponseError.Command -> mapCommandError(error)
            is IllegalStateException -> generalFailure()
            else -> NfcCardReader.Result.Error(
                errorCode = UNKNOWN_ERROR_CODE,
                userMessage = defaultAction
            )
        }
    }

    private fun mapCommandError(error: ApduResponseError.Command): NfcCardReader.Result.Error {
        return when {
            isDeclined(error) -> declined()
            isUnsupported(error) -> unsupportedCard()
            else -> generalFailure()
        }
    }

    private fun isDeclined(error: ApduResponseError.Command): Boolean {
        return error.sw1 == COMMAND_NOT_ALLOWED_SW1 &&
            error.sw2 == CONDITIONS_OF_USE_NOT_SATISFIED_SW2
    }

    private fun isUnsupported(error: ApduResponseError.Command): Boolean {
        return when (error.apduCommand) {
            is SelectPpseCommand,
            is SelectApplicationCommand -> error.sw1 == PARAMETER_ERROR_SW1 && error.sw2 == FILE_NOT_FOUND_SW2
            is ReadRecordCommand -> false
            else -> false
        }
    }

    private fun unsupportedCard(): NfcCardReader.Result.Error {
        return NfcCardReader.Result.Error(
            errorCode = UNSUPPORTED_CARD_ERROR_CODE,
            userMessage = R.string.stripe_nfc_scan_unsupported_card.resolvableString,
        )
    }

    private fun declined(): NfcCardReader.Result.Error {
        return NfcCardReader.Result.Error(
            errorCode = CARD_DECLINED_ERROR_CODE,
            userMessage = R.string.stripe_nfc_scan_error_declined_card.resolvableString,
        )
    }

    private fun generalFailure(): NfcCardReader.Result.Error {
        return NfcCardReader.Result.Error(
            errorCode = GENERAL_READ_ERROR_CODE,
            userMessage = R.string.stripe_tap_to_add_card_default_error_action.resolvableString,
        )
    }

    private companion object {
        const val TRANSCEIVER_SECURITY_ERROR_CODE = "nfcTransceiverSecurityError"
        const val TRANSCEIVER_IO_ERROR_CODE = "nfcTransceiverIoError"
        const val UNKNOWN_ERROR_CODE = "unknownNfcReaderError"

        const val APDU_RESPONSE_TOO_SHORT_ERROR_CODE = "apduResponseTooShort"
        const val APDU_RESPONSE_PARSING_ERROR_CODE = "apduParsingCode"

        const val UNSUPPORTED_CARD_ERROR_CODE = "cardUnsupportedByNfc"
        const val CARD_DECLINED_ERROR_CODE = "cardDeclinedByNfc"
        const val GENERAL_READ_ERROR_CODE = "nfcCardReadFailed"

        const val PARAMETER_ERROR_SW1 = 0x6A.toByte()
        const val FILE_NOT_FOUND_SW2 = 0x82.toByte()
        const val COMMAND_NOT_ALLOWED_SW1 = 0x69.toByte()
        const val CONDITIONS_OF_USE_NOT_SATISFIED_SW2 = 0x85.toByte()
    }
}
