package com.stripe.android.common.nfcscan.scanner

import com.stripe.android.core.strings.ResolvableString
import com.stripe.android.core.strings.resolvableString
import com.stripe.android.paymentsheet.R
import javax.inject.Inject
import kotlin.collections.joinToString

internal interface NfcCardDataParser {
    fun parse(records: Map<String, ByteArray>): Result

    sealed interface Result {
        data class Success(val cardData: ScannedCardData) : Result

        data class Error(
            val analyticsValue: String,
            val userMessage: ResolvableString,
        ) : Result
    }
}

internal class DefaultNfcCardDataParser @Inject constructor() : NfcCardDataParser {
    override fun parse(records: Map<String, ByteArray>): NfcCardDataParser.Result {
        if (isMobileWallet(records)) {
            return NfcCardDataParser.Result.Error(
                analyticsValue = MOBILE_WALLET_UNSUPPORTED_ANALYTICS_VALUE,
                userMessage = R.string.stripe_nfc_scan_error_mobile_wallet.resolvableString,
            )
        }

        if (isTokenized(records)) {
            return unsupportedCard()
        }

        return when (val cardData = extractCardData(records)) {
            is ScannedCardData -> NfcCardDataParser.Result.Success(cardData)
            null -> unsupportedCard()
        }
    }

    private fun extractCardData(records: Map<String, ByteArray>): ScannedCardData? {
        // Tag 0x57 — Track 2 Equivalent Data. Preferred when present because it encodes PAN and
        // expiry together in the same format used on magnetic-stripe Track 2.
        records[TAG_TRACK2]?.let {
            return parseFromTrack2(it)
        }

        // Fall back to separate PAN and expiry tags when Track 2 is unavailable.
        val panBytes = records[TAG_PAN] ?: return null
        val pan = panBytes.toReadableString()
            // Cards pad odd-length numbers with trailing 'F'.
            .trimEnd(*PAN_TRAILING_CHARS)

        val expiryBytes = records[TAG_EXPIRY] ?: return null
        val (month, year) = parseExpiry(expiryBytes) ?: return null

        return ScannedCardData(
            cardNumber = pan,
            expirationMonth = month,
            expirationYear = year,
        )
    }

    /*
     * A mobile wallet (Apple Pay, Google Pay, etc.) is identified when any of:
     * - the Form Factor Indicator (0x9F6E) reports a non-card device, or
     * - the Token Requestor ID (0x9F19) matches a known wallet provider, or
     * - the Application Label (0x50) contains a wallet keyword.
     */
    private fun isMobileWallet(records: Map<String, ByteArray>): Boolean {
        val formFactorIsMobile = records[TAG_FORM_FACTOR_INDICATOR]?.let { isMobileFormFactor(it) } == true

        val knownWalletTrid = records[TAG_TOKEN_REQUESTOR_ID]?.let { bytes ->
            val trid = bytes.toReadableString().trimEnd(*PAN_TRAILING_CHARS)
            KNOWN_WALLET_TOKEN_REQUESTOR_IDS.any { trid.startsWith(it) }
        } == true

        val walletLabel = records[TAG_APPLICATION_LABEL]?.let { bytes ->
            val label = bytes.toAsciiString().uppercase()
            WALLET_LABEL_KEYWORDS.any { label.contains(it) }
        } == true

        return formFactorIsMobile || knownWalletTrid || walletLabel
    }

    private fun isTokenized(records: Map<String, ByteArray>): Boolean {
        /*
         * A tokenized (non-mobile-wallet) credential is identified by the presence of a Token Requestor
         * ID (0x9F19) or a Payment Account Reference (0x9F24).
         */
        return TAG_TOKEN_REQUESTOR_ID in records || TAG_PAYMENT_ACCOUNT_REFERENCE in records
    }

    private fun isMobileFormFactor(bytes: ByteArray): Boolean {
        if (bytes.isEmpty()) return false

        // Mastercard/Amex FFI: byte 1 low nibble encodes the device form factor; 0x0 is a card and
        // any other value is a mobile/wearable form factor.
        val formFactor = bytes[0].toSingleByte() and FORM_FACTOR_MASK
        if (formFactor != FORM_FACTOR_CARD) {
            return true
        }

        // Visa/Mastercard FFI: byte 2 bit 8 signals a network-connected (mobile) device.
        if (bytes.size >= FORM_FACTOR_MIN_LENGTH_FOR_CONNECTED_BIT) {
            val byteTwo = bytes[1].toSingleByte()
            if (byteTwo and NETWORK_CONNECTED_BIT != 0) {
                return true
            }
        }

        return false
    }

    private fun parseFromTrack2(bytes: ByteArray): ScannedCardData? {
        val hex = bytes.toReadableString()

        // Get the separator between the PAN and expiration date
        val sep = hex.indexOf(TRACK2_SEPARATOR).takeIf { it >= 0 } ?: return null

        val pan = hex.substring(0, sep)

        if (hex.length < sep + TRACK2_EXPIRY_MONTH_END_OFFSET) return null

        val year = hex.substring(
            startIndex = sep + TRACK2_EXPIRY_YEAR_START_OFFSET,
            endIndex = sep + TRACK2_EXPIRY_MONTH_START_OFFSET,
        ).toIntOrNull() ?: return null

        val month = hex.substring(
            sep + TRACK2_EXPIRY_MONTH_START_OFFSET,
            sep + TRACK2_EXPIRY_MONTH_END_OFFSET,
        ).toIntOrNull() ?: return null

        return ScannedCardData(
            cardNumber = pan,
            expirationMonth = month,
            expirationYear = EXPIRATION_YEAR_START + year,
        )
    }

    private fun parseExpiry(bytes: ByteArray): Pair<Int, Int>? {
        if (bytes.size < EXPIRATION_DATE_MIN_LENGTH) return null

        val yearBcd = bytes[0].toSingleByte()
        val monthBcd = bytes[1].toSingleByte()

        val year = EXPIRATION_YEAR_START + yearBcd.toTwoDigitNumber()
        val month = monthBcd.toTwoDigitNumber()

        return month to year
    }

    private fun unsupportedCard(): NfcCardDataParser.Result.Error {
        return NfcCardDataParser.Result.Error(
            analyticsValue = CARD_UNSUPPORTED_ANALYTICS_VALUE,
            userMessage = R.string.stripe_nfc_scan_unsupported_card.resolvableString,
        )
    }

    private fun ByteArray.toReadableString(): String {
        return joinToString("") { "%02X".format(it) }
    }

    private fun ByteArray.toAsciiString(): String {
        return map { (it.toInt() and BYTE_MASK).toChar() }.joinToString("")
    }

    private fun Byte.toSingleByte() = toInt() and BYTE_MASK
    private fun Int.toTwoDigitNumber() =
        (this shr FIRST_DIGIT_SHIFT) * FIRST_DIGIT_MULTIPLIER + (this and LAST_DIGIT_MASK)

    private companion object {
        const val MOBILE_WALLET_UNSUPPORTED_ANALYTICS_VALUE = "mobileWalletUnsupportedByNfc"
        const val CARD_UNSUPPORTED_ANALYTICS_VALUE = "cardUnsupportedByNfc"

        const val TAG_TRACK2 = "57"
        const val TAG_PAN = "5A"
        const val TAG_EXPIRY = "5F24"

        const val TAG_TOKEN_REQUESTOR_ID = "9F19"
        const val TAG_PAYMENT_ACCOUNT_REFERENCE = "9F24"
        const val TAG_FORM_FACTOR_INDICATOR = "9F6E"
        const val TAG_APPLICATION_LABEL = "50"

        // Leading digits of known wallet Token Requestor IDs (tag 0x9F19).
        val KNOWN_WALLET_TOKEN_REQUESTOR_IDS = listOf(
            "50110030273", // Apple Pay (Mastercard)
            "50120834693", // Android/Google Pay (Mastercard)
            "50139059239", // Samsung Pay (Mastercard)
            "40010030273", // Apple Pay (Visa)
            "40010075001", // Google Pay (Visa)
        )

        val WALLET_LABEL_KEYWORDS = listOf(
            "APPLE PAY",
            "GOOGLE PAY",
            "ANDROID PAY",
            "SAMSUNG PAY",
            "GARMIN PAY",
            "FITBIT PAY",
        )

        const val TRACK2_SEPARATOR = 'D'
        const val EXPIRATION_YEAR_START = 2000

        const val TRACK2_EXPIRY_YEAR_START_OFFSET = 1
        const val TRACK2_EXPIRY_MONTH_START_OFFSET = 3
        const val TRACK2_EXPIRY_MONTH_END_OFFSET = 5

        const val EXPIRATION_DATE_MIN_LENGTH = 3
        const val BYTE_MASK = 0xFF
        const val LAST_DIGIT_MASK = 0x0F
        const val FIRST_DIGIT_SHIFT = 4
        const val FIRST_DIGIT_MULTIPLIER = 10

        const val FORM_FACTOR_MASK = 0x0F
        const val FORM_FACTOR_CARD = 0x00
        const val FORM_FACTOR_MIN_LENGTH_FOR_CONNECTED_BIT = 2
        const val NETWORK_CONNECTED_BIT = 0x80

        val PAN_TRAILING_CHARS = charArrayOf('F', 'f')
    }
}
