package com.stripe.android.common.nfcscan.scanner

import javax.inject.Inject
import kotlin.collections.joinToString

internal interface NfcCardDataParser {
    fun parse(records: Map<String, ByteArray>): ScannedCardData?
}

internal class DefaultNfcCardDataParser @Inject constructor() : NfcCardDataParser {
    override fun parse(records: Map<String, ByteArray>): ScannedCardData? {
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

    private fun ByteArray.toReadableString(): String {
        return joinToString("") { "%02X".format(it) }
    }

    private fun Byte.toSingleByte() = toInt() and BYTE_MASK
    private fun Int.toTwoDigitNumber() =
        (this shr FIRST_DIGIT_SHIFT) * FIRST_DIGIT_MULTIPLIER + (this and LAST_DIGIT_MASK)

    private companion object {
        const val TAG_TRACK2 = "57"
        const val TAG_PAN = "5A"
        const val TAG_EXPIRY = "5F24"

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

        val PAN_TRAILING_CHARS = charArrayOf('F', 'f')
    }
}
