package com.stripe.android.common.nfcscan.scanner.apdu

import com.stripe.android.common.nfcscan.scanner.NfcScanningError
import com.stripe.android.core.strings.ResolvableString
import com.stripe.android.core.strings.resolvableString
import com.stripe.android.paymentsheet.R

internal sealed class ApduResponseError(
    override val message: String?,
    override val cause: Throwable? = null,
) : NfcScanningError() {
    class TooShort : ApduResponseError(
        message = "APDU response is too short! Needs at least two bytes!"
    ) {
        override val errorCode: String = "apduResponseTooShort"
        override val userMessage: ResolvableString = R.string.stripe_something_went_wrong.resolvableString
    }

    data class Command(
        val apduCommand: ApduCommand<*>,
        val sw1: Byte,
        val sw2: Byte,
    ) : ApduResponseError(
        message = "APDU error: SW1=$sw1, SW2=$sw2"
    ) {
        override val errorCode: String by lazy {
            when {
                isDeclined() -> CARD_DECLINED_ERROR_CODE
                isUnsupported() -> UNSUPPORTED_CARD_ERROR_CODE
                else -> GENERAL_READ_ERROR_CODE
            }
        }

        override val userMessage: ResolvableString by lazy {
            when {
                isDeclined() -> R.string.stripe_nfc_scan_error_declined_card.resolvableString
                isUnsupported() -> R.string.stripe_nfc_scan_unsupported_card.resolvableString
                else -> R.string.stripe_nfc_try_again_error.resolvableString
            }
        }

        override val parameters: Map<String, String> by lazy {
            mapOf(
                SW1_PARAMETER to formatStatusWord(sw1),
                SW2_PARAMETER to formatStatusWord(sw2),
            )
        }

        private fun isDeclined(): Boolean {
            return sw1 == COMMAND_NOT_ALLOWED_SW1 && sw2 == CONDITIONS_OF_USE_NOT_SATISFIED_SW2
        }

        private fun isUnsupported(): Boolean {
            return when (apduCommand) {
                is SelectPpseCommand,
                is SelectApplicationCommand -> sw1 == PARAMETER_ERROR_SW1 && sw2 == FILE_NOT_FOUND_SW2
                is ReadRecordCommand -> false
                else -> false
            }
        }

        private fun formatStatusWord(statusWord: Byte): String {
            return String.format(STATUS_WORD_FORMAT, statusWord.toInt() and STATUS_WORD_MASK)
        }

        private companion object {
            const val UNSUPPORTED_CARD_ERROR_CODE = "cardUnsupportedByNfc"
            const val CARD_DECLINED_ERROR_CODE = "cardDeclinedByNfc"
            const val GENERAL_READ_ERROR_CODE = "nfcCardReadFailed"

            const val SW1_PARAMETER = "sw1"
            const val SW2_PARAMETER = "sw2"
            const val STATUS_WORD_FORMAT = "%02X"
            const val STATUS_WORD_MASK = 0xFF

            const val PARAMETER_ERROR_SW1 = 0x6A.toByte()
            const val FILE_NOT_FOUND_SW2 = 0x82.toByte()
            const val COMMAND_NOT_ALLOWED_SW1 = 0x69.toByte()
            const val CONDITIONS_OF_USE_NOT_SATISFIED_SW2 = 0x85.toByte()
        }
    }

    class Parsing(
        override val cause: Throwable?,
    ) : ApduResponseError(
        message = "Failed to parse response data!"
    ) {
        override val errorCode: String = "apduParsingIssue"
        override val userMessage: ResolvableString = R.string.stripe_nfc_scan_internal_error_message.resolvableString
    }

    class Invalid : ApduResponseError(
        message = "Invalid data in response!"
    ) {
        override val errorCode: String = "cardUnsupportedByNfc"
        override val userMessage: ResolvableString = R.string.stripe_nfc_scan_internal_error_message.resolvableString
    }
}
