package com.stripe.android.common.taptoadd.nfcdirect

/**
 * EMV contactless APDU command builders.
 *
 * Implements the minimal command set required for Card Not Present (CNP) card data reading:
 * - SELECT PPSE (Proximity Payment System Environment)
 * - SELECT AID (Application Identifier)
 * - GET PROCESSING OPTIONS
 * - READ RECORD
 *
 * Note: GENERATE AC is intentionally NOT included as this is for CNP use only.
 */
internal object EmvApduCommands {

    // APDU instruction codes
    private const val CLA_ISO = 0x00.toByte()
    private const val CLA_EMV = 0x80.toByte()
    private const val INS_SELECT = 0xA4.toByte()
    private const val INS_READ_RECORD = 0xB2.toByte()
    private const val INS_GPO = 0xA8.toByte()
    private const val INS_INTERNAL_AUTH = 0x88.toByte()

    // SELECT command parameters
    private const val P1_SELECT_BY_NAME = 0x04.toByte()
    private const val P2_SELECT_FIRST = 0x00.toByte()

    /**
     * PPSE name: "2PAY.SYS.DDF01"
     * Used for contactless payment applications discovery.
     */
    private val PPSE_NAME = "2PAY.SYS.DDF01".toByteArray(Charsets.US_ASCII)

    /**
     * Common payment application AIDs.
     */
    object Aids {
        val VISA = "A0000000031010".hexToByteArray()
        val MASTERCARD = "A0000000041010".hexToByteArray()
        val AMEX = "A00000002501".hexToByteArray()
        val DISCOVER = "A0000001523010".hexToByteArray()
        val JCB = "A0000000651010".hexToByteArray()
        val UNIONPAY = "A000000333010101".hexToByteArray()
    }

    /**
     * SELECT PPSE command.
     * First command in EMV contactless flow to discover available applications.
     *
     * Returns FCI (File Control Information) template with available AIDs.
     */
    val SELECT_PPSE: ByteArray = buildSelectCommand(PPSE_NAME)

    /**
     * Build SELECT command for a specific AID.
     *
     * @param aid Application Identifier to select
     * @return Complete APDU command bytes
     */
    fun selectAid(aid: ByteArray): ByteArray = buildSelectCommand(aid)

    /**
     * Build GET PROCESSING OPTIONS command.
     *
     * Initiates the EMV transaction and returns:
     * - AIP (Application Interchange Profile)
     * - AFL (Application File Locator) - tells us which records to read
     *
     * @param pdol PDOL (Processing Options Data Object List) data, or null if not required
     * @return Complete APDU command bytes
     */
    fun getProcessingOptions(pdol: ByteArray? = null): ByteArray {
        val data = if (pdol != null && pdol.isNotEmpty()) {
            // Construct PDOL data: tag 83 + length + PDOL values
            byteArrayOf(0x83.toByte(), pdol.size.toByte()) + pdol
        } else {
            // Empty PDOL: tag 83 + length 00
            byteArrayOf(0x83.toByte(), 0x00)
        }

        return byteArrayOf(
            CLA_EMV,           // CLA
            INS_GPO,           // INS: GET PROCESSING OPTIONS
            0x00,              // P1
            0x00,              // P2
            data.size.toByte() // Lc
        ) + data + byteArrayOf(0x00) // Le
    }

    /**
     * Build READ RECORD command.
     *
     * Reads a single record from the card based on AFL information.
     *
     * @param sfi Short File Identifier (1-30)
     * @param recordNumber Record number within the file (1-255)
     * @return Complete APDU command bytes
     */
    fun readRecord(sfi: Int, recordNumber: Int): ByteArray {
        require(sfi in 1..30) { "SFI must be between 1 and 30" }
        require(recordNumber in 1..255) { "Record number must be between 1 and 255" }

        // P2: bits 3-7 = SFI, bits 1-2 = 100 (record number in P1)
        val p2 = ((sfi shl 3) or 0x04).toByte()

        return byteArrayOf(
            CLA_ISO,              // CLA
            INS_READ_RECORD,      // INS: READ RECORD
            recordNumber.toByte(), // P1: record number
            p2,                    // P2: SFI reference
            0x00                   // Le: expect full record
        )
    }

    /**
     * Build INTERNAL AUTHENTICATE command for DDA verification.
     *
     * The card signs the provided data using its ICC private key.
     * Used for Dynamic Data Authentication (DDA).
     *
     * @param ddol DDOL data (Dynamic Data Authentication Data Object List)
     *             If null, sends unpredictable number only (4 bytes)
     * @return Complete APDU command bytes
     */
    fun internalAuthenticate(ddol: ByteArray? = null): ByteArray {
        val data = ddol ?: generateUnpredictableNumber()

        return byteArrayOf(
            CLA_ISO,           // CLA
            INS_INTERNAL_AUTH, // INS: INTERNAL AUTHENTICATE
            0x00,              // P1
            0x00,              // P2
            data.size.toByte() // Lc
        ) + data + byteArrayOf(0x00) // Data + Le
    }

    /**
     * Generate 4-byte unpredictable number for DDA.
     */
    private fun generateUnpredictableNumber(): ByteArray {
        return ByteArray(4).also { java.security.SecureRandom().nextBytes(it) }
    }

    /**
     * Parse AFL (Application File Locator) from GPO response.
     *
     * AFL format: sequence of 4-byte entries
     * - Byte 1: SFI (shifted left by 3)
     * - Byte 2: First record number
     * - Byte 3: Last record number
     * - Byte 4: Number of records involved in offline data authentication
     *
     * @return List of (sfi, firstRecord, lastRecord) tuples
     */
    fun parseAfl(afl: ByteArray): List<Triple<Int, Int, Int>> {
        val entries = mutableListOf<Triple<Int, Int, Int>>()

        if (afl.size % 4 != 0) return entries

        for (i in afl.indices step 4) {
            val sfi = (afl[i].toInt() and 0xFF) shr 3
            val firstRecord = afl[i + 1].toInt() and 0xFF
            val lastRecord = afl[i + 2].toInt() and 0xFF
            // Byte 4 is ODA count, not needed for card data reading

            entries.add(Triple(sfi, firstRecord, lastRecord))
        }

        return entries
    }

    /**
     * Check if APDU response indicates success.
     * SW1=90, SW2=00 indicates successful execution.
     */
    fun isSuccess(response: ByteArray): Boolean {
        if (response.size < 2) return false
        val sw1 = response[response.size - 2].toInt() and 0xFF
        val sw2 = response[response.size - 1].toInt() and 0xFF
        return sw1 == 0x90 && sw2 == 0x00
    }

    /**
     * Extract data from APDU response (removes status bytes).
     */
    fun getResponseData(response: ByteArray): ByteArray {
        if (response.size <= 2) return ByteArray(0)
        return response.copyOfRange(0, response.size - 2)
    }

    /**
     * Get status word from APDU response.
     */
    fun getStatusWord(response: ByteArray): Int {
        if (response.size < 2) return 0
        val sw1 = response[response.size - 2].toInt() and 0xFF
        val sw2 = response[response.size - 1].toInt() and 0xFF
        return (sw1 shl 8) or sw2
    }

    private fun buildSelectCommand(name: ByteArray): ByteArray {
        return byteArrayOf(
            CLA_ISO,           // CLA
            INS_SELECT,        // INS: SELECT
            P1_SELECT_BY_NAME, // P1: Select by name
            P2_SELECT_FIRST,   // P2: First or only occurrence
            name.size.toByte() // Lc: length of data
        ) + name + byteArrayOf(0x00) // Data + Le
    }

    private fun String.hexToByteArray(): ByteArray {
        return ByteArray(length / 2) { i ->
            substring(i * 2, i * 2 + 2).toInt(16).toByte()
        }
    }
}
