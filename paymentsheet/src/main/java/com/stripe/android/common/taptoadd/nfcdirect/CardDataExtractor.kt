package com.stripe.android.common.taptoadd.nfcdirect

import com.stripe.android.common.taptoadd.nfcdirect.TlvParser.toHexString

/**
 * Extracts card data from EMV TLV responses.
 *
 * Handles extraction of:
 * - PAN (Primary Account Number)
 * - Expiration date
 * - Cardholder name
 * - Application Identifier (AID)
 * - Card scheme/brand detection
 */
internal class CardDataExtractor {

    /**
     * Extracted card data result.
     */
    data class CardData(
        val pan: String,
        val expiryMonth: Int,
        val expiryYear: Int,
        val cardholderName: String?,
        val aid: ByteArray,
        val scheme: CardScheme
    ) {
        val last4: String get() = pan.takeLast(4)

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other !is CardData) return false
            return pan == other.pan && expiryMonth == other.expiryMonth &&
                expiryYear == other.expiryYear && aid.contentEquals(other.aid)
        }

        override fun hashCode(): Int {
            var result = pan.hashCode()
            result = 31 * result + expiryMonth
            result = 31 * result + expiryYear
            result = 31 * result + aid.contentHashCode()
            return result
        }
    }

    /**
     * Supported card schemes.
     */
    enum class CardScheme(val code: String) {
        VISA("visa"),
        MASTERCARD("mastercard"),
        AMEX("amex"),
        DISCOVER("discover"),
        JCB("jcb"),
        UNIONPAY("unionpay"),
        UNKNOWN("unknown")
    }

    // EMV Tags
    companion object {
        const val TAG_AID = "4F"              // Application Identifier (DF Name)
        const val TAG_APP_LABEL = "50"        // Application Label
        const val TAG_TRACK2 = "57"           // Track 2 Equivalent Data
        const val TAG_PAN = "5A"              // Application PAN
        const val TAG_CARDHOLDER_NAME = "5F20" // Cardholder Name
        const val TAG_EXPIRY = "5F24"         // Application Expiration Date
        const val TAG_PAN_SEQUENCE = "5F34"   // PAN Sequence Number
        const val TAG_FCI_TEMPLATE = "6F"     // FCI Template
        const val TAG_DF_NAME = "84"          // DF Name (AID in FCI)
        const val TAG_FCI_PROP = "A5"         // FCI Proprietary Template
        const val TAG_AFL = "94"              // Application File Locator
        const val TAG_AIP = "82"              // Application Interchange Profile
        const val TAG_PDOL = "9F38"           // PDOL
        const val TAG_RESPONSE_FORMAT1 = "80" // Response Message Template Format 1
        const val TAG_RESPONSE_FORMAT2 = "77" // Response Message Template Format 2
        const val TAG_DIRECTORY_ENTRY = "61"  // Application Template
        const val TAG_ADF_NAME = "4F"         // ADF Name (in directory entry)
    }

    /**
     * Extract AID from PPSE response.
     *
     * PPSE response contains directory entries with available AIDs.
     * We prefer known payment AIDs in order: Visa, MC, Amex, Discover, JCB.
     */
    fun extractAid(ppseResponse: ByteArray): ByteArray? {
        val tlv = TlvParser.parse(EmvApduCommands.getResponseData(ppseResponse))

        // Try to find AID directly
        tlv[TAG_AID]?.let { return it }
        tlv[TAG_DF_NAME]?.let { return it }
        tlv[TAG_ADF_NAME]?.let { return it }

        // Look for known AIDs in the response
        return findPreferredAid(tlv)
    }

    /**
     * Extract PDOL (Processing Options Data Object List) from SELECT AID response.
     *
     * PDOL tells us what data the card needs for GPO command.
     * For simple card reading, we can often send empty PDOL values.
     */
    fun extractPdol(selectResponse: ByteArray): ByteArray? {
        val tlv = TlvParser.parse(EmvApduCommands.getResponseData(selectResponse))
        return tlv[TAG_PDOL]
    }

    /**
     * Extract AFL from GPO response.
     *
     * AFL tells us which records to read to get card data.
     */
    fun extractAfl(gpoResponse: ByteArray): ByteArray? {
        val responseData = EmvApduCommands.getResponseData(gpoResponse)
        val tlv = TlvParser.parse(responseData)

        // Try format 2 (tag 77) first - contains AFL directly
        tlv[TAG_AFL]?.let { return it }

        // Format 1 (tag 80): AIP (2 bytes) + AFL
        // Note: For some qVSDC cards, this might be AIP + Track 2 instead of AFL
        tlv[TAG_RESPONSE_FORMAT1]?.let { format1Data ->
            if (format1Data.size > 2) {
                val possibleAfl = format1Data.copyOfRange(2, format1Data.size)
                // AFL entries are always 4 bytes each
                if (possibleAfl.size % 4 == 0 && possibleAfl.isNotEmpty()) {
                    // Check if first byte looks like a valid SFI (bits 3-7 set, shifted left)
                    val firstByte = possibleAfl[0].toInt() and 0xFF
                    if (firstByte in 0x08..0xF8 && (firstByte and 0x07) == 0) {
                        return possibleAfl
                    }
                }
            }
        }

        return null
    }

    /**
     * Extract Track 2 data from GPO Format 1 response if present.
     * Some Visa qVSDC cards return Track 2 equivalent in GPO response.
     */
    fun extractTrack2FromGpo(gpoResponse: ByteArray): ByteArray? {
        val responseData = EmvApduCommands.getResponseData(gpoResponse)
        val tlv = TlvParser.parse(responseData)

        // Format 1 (tag 80) might contain AIP (2 bytes) + Track 2 equivalent
        tlv[TAG_RESPONSE_FORMAT1]?.let { format1Data ->
            if (format1Data.size > 2) {
                val dataAfterAip = format1Data.copyOfRange(2, format1Data.size)
                // Track 2 starts with PAN which starts with 3, 4, 5, or 6
                if (dataAfterAip.isNotEmpty()) {
                    val firstNibble = (dataAfterAip[0].toInt() and 0xF0) shr 4
                    if (firstNibble in 3..6) {
                        // Looks like Track 2 data (PAN starts with valid digit)
                        return dataAfterAip
                    }
                }
            }
        }

        return null
    }

    /**
     * Extract card data from all collected record data.
     *
     * @param records Concatenated record data from READ RECORD commands
     * @param aid The selected AID for scheme detection
     */
    fun extract(records: ByteArray, aid: ByteArray): CardData {
        val tlv = TlvParser.parse(records)

        // Extract PAN - try direct tag first, then Track 2
        val pan = extractPan(tlv)
            ?: throw CardDataExtractionException("PAN not found in card data")

        // Extract expiry date
        val (month, year) = extractExpiry(tlv)
            ?: throw CardDataExtractionException("Expiry date not found in card data")

        // Extract cardholder name (optional)
        val cardholderName = extractCardholderName(tlv)

        // Detect scheme from AID
        val scheme = detectScheme(aid)

        return CardData(
            pan = pan,
            expiryMonth = month,
            expiryYear = year,
            cardholderName = cardholderName,
            aid = aid,
            scheme = scheme
        )
    }

    /**
     * Extract card data from pre-parsed TLV map.
     * This is useful when TLV data has been accumulated from multiple responses
     * (GPO + READ RECORDs).
     *
     * @param tlv Pre-parsed TLV data map
     * @param aid The selected AID for scheme detection
     */
    fun extractFromTlvMap(tlv: Map<String, ByteArray>, aid: ByteArray): CardData {
        // Extract PAN - try direct tag first, then Track 2
        val pan = extractPan(tlv)
            ?: throw CardDataExtractionException(
                "PAN not found in card data. Available tags: ${tlv.keys.joinToString()}"
            )

        // Extract expiry date
        val (month, year) = extractExpiry(tlv)
            ?: throw CardDataExtractionException(
                "Expiry date not found in card data. Available tags: ${tlv.keys.joinToString()}"
            )

        // Extract cardholder name (optional)
        val cardholderName = extractCardholderName(tlv)

        // Detect scheme from AID
        val scheme = detectScheme(aid)

        return CardData(
            pan = pan,
            expiryMonth = month,
            expiryYear = year,
            cardholderName = cardholderName,
            aid = aid,
            scheme = scheme
        )
    }

    /**
     * Extract PAN from TLV data.
     */
    private fun extractPan(tlv: Map<String, ByteArray>): String? {
        // Try tag 5A (Application PAN)
        tlv[TAG_PAN]?.let { panBytes ->
            return decodeBcdPan(panBytes)
        }

        // Fallback to Track 2 data
        tlv[TAG_TRACK2]?.let { track2 ->
            return extractPanFromTrack2(track2)
        }

        return null
    }

    /**
     * Extract expiry date from TLV data.
     *
     * @return Pair of (month, year) where year is 4-digit
     */
    private fun extractExpiry(tlv: Map<String, ByteArray>): Pair<Int, Int>? {
        // Try tag 5F24 (YYMMDD format in BCD)
        tlv[TAG_EXPIRY]?.let { expiryBytes ->
            if (expiryBytes.size >= 2) {
                val yy = decodeBcd(expiryBytes[0])
                val mm = decodeBcd(expiryBytes[1])
                // Convert 2-digit year to 4-digit (assume 20xx)
                val year = 2000 + yy
                return mm to year
            }
        }

        // Fallback to Track 2 data
        tlv[TAG_TRACK2]?.let { track2 ->
            return extractExpiryFromTrack2(track2)
        }

        return null
    }

    /**
     * Extract cardholder name from TLV data.
     */
    private fun extractCardholderName(tlv: Map<String, ByteArray>): String? {
        return tlv[TAG_CARDHOLDER_NAME]?.let { nameBytes ->
            // Name is ASCII encoded, trim trailing spaces and slash
            String(nameBytes, Charsets.US_ASCII)
                .trim()
                .trimEnd('/')
                .takeIf { it.isNotEmpty() }
        }
    }

    /**
     * Decode BCD-encoded PAN.
     * PAN is encoded as packed BCD, with 'F' padding at end if odd length.
     */
    private fun decodeBcdPan(data: ByteArray): String {
        val sb = StringBuilder()
        for (byte in data) {
            val high = (byte.toInt() shr 4) and 0x0F
            val low = byte.toInt() and 0x0F
            if (high < 10) sb.append(high)
            if (low < 10) sb.append(low) // Skip 'F' padding
        }
        return sb.toString()
    }

    /**
     * Decode single BCD byte to integer.
     */
    private fun decodeBcd(byte: Byte): Int {
        val high = (byte.toInt() shr 4) and 0x0F
        val low = byte.toInt() and 0x0F
        return high * 10 + low
    }

    /**
     * Extract PAN from Track 2 equivalent data.
     * Format: PAN + 'D' separator + Expiry + Service Code + Discretionary Data
     */
    private fun extractPanFromTrack2(track2: ByteArray): String? {
        val hexString = track2.toHexString()
        val separatorIndex = hexString.indexOf('D')
        if (separatorIndex == -1) return null
        return hexString.substring(0, separatorIndex)
    }

    /**
     * Extract expiry from Track 2 equivalent data.
     */
    private fun extractExpiryFromTrack2(track2: ByteArray): Pair<Int, Int>? {
        val hexString = track2.toHexString()
        val separatorIndex = hexString.indexOf('D')
        if (separatorIndex == -1 || separatorIndex + 5 > hexString.length) return null

        // Expiry is YYMM format after separator
        val expiryStr = hexString.substring(separatorIndex + 1, separatorIndex + 5)
        return try {
            val yy = expiryStr.substring(0, 2).toInt()
            val mm = expiryStr.substring(2, 4).toInt()
            val year = 2000 + yy
            mm to year
        } catch (e: NumberFormatException) {
            null
        }
    }

    /**
     * Detect card scheme from AID.
     */
    private fun detectScheme(aid: ByteArray): CardScheme {
        val aidHex = aid.toHexString().uppercase()

        return when {
            aidHex.startsWith("A000000003") -> CardScheme.VISA
            aidHex.startsWith("A000000004") -> CardScheme.MASTERCARD
            aidHex.startsWith("A000000025") -> CardScheme.AMEX
            aidHex.startsWith("A000000152") -> CardScheme.DISCOVER
            aidHex.startsWith("A000000065") -> CardScheme.JCB
            aidHex.startsWith("A000000333") -> CardScheme.UNIONPAY
            else -> CardScheme.UNKNOWN
        }
    }

    /**
     * Find preferred AID from PPSE response.
     * Looks for common payment AIDs in priority order.
     */
    private fun findPreferredAid(tlv: Map<String, ByteArray>): ByteArray? {
        // Check each tag value for known AID patterns
        for ((_, value) in tlv) {
            val hex = value.toHexString().uppercase()
            when {
                hex.startsWith("A000000003") -> return value // Visa
                hex.startsWith("A000000004") -> return value // Mastercard
                hex.startsWith("A000000025") -> return value // Amex
                hex.startsWith("A000000152") -> return value // Discover
            }
        }
        return null
    }
}

/**
 * Exception thrown when card data extraction fails.
 */
internal class CardDataExtractionException(message: String) : Exception(message)
