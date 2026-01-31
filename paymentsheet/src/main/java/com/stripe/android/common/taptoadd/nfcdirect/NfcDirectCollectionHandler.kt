package com.stripe.android.common.taptoadd.nfcdirect

import android.nfc.tech.IsoDep
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import com.stripe.android.common.taptoadd.TapToAddCollectionHandler
import com.stripe.android.common.taptoadd.TapToAddCollectionHandler.CollectionState
import com.stripe.android.common.taptoadd.nfcdirect.TlvParser.toHexString
import com.stripe.android.common.taptoadd.nfcdirect.oda.OdaFailedException
import com.stripe.android.common.taptoadd.nfcdirect.oda.OdaVerifier
import com.stripe.android.core.strings.resolvableString
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadata
import com.stripe.android.model.CardBrand
import com.stripe.android.model.PaymentMethod
import java.io.IOException

/**
 * Collection handler that reads card data directly via NFC using EMV commands.
 *
 * Implements the minimal EMV contactless flow for Card Not Present (CNP):
 * 1. SELECT PPSE - Discover available payment applications
 * 2. SELECT AID - Select the payment application
 * 3. GET PROCESSING OPTIONS - Initialize transaction, get AFL
 * 4. READ RECORD - Read card data (PAN, expiry, name)
 *
 * Note: Does NOT perform GENERATE AC (cryptogram generation) as this is for CNP only.
 */
@RequiresApi(Build.VERSION_CODES.KITKAT)
internal class NfcDirectCollectionHandler(
    private val connectionManager: NfcDirectConnectionManager,
    private val cardDataExtractor: CardDataExtractor = CardDataExtractor(),
    private val digitalWalletDetector: DigitalWalletDetector = DigitalWalletDetector(),
    private val odaVerifier: OdaVerifier = OdaVerifier(),
) : TapToAddCollectionHandler {

    companion object {
        private const val TAG = "NfcDirectCollection"
        // Status words for record not found conditions
        private const val SW_RECORD_NOT_FOUND = 0x6A83
        private const val SW_REFERENCED_DATA_NOT_FOUND = 0x6A88
    }

    override suspend fun collect(metadata: PaymentMethodMetadata): CollectionState {
        return try {
            // Wait for card connection
            if (!connectionManager.isConnected) {
                connectionManager.connect()
                connectionManager.awaitConnection().onFailure { throw it }
            }

            val isoDep = connectionManager.getCurrentIsoDep()
                ?: connectionManager.awaitTag()

            // Perform EMV transaction
            val cardData = performEmvTransaction(isoDep)

            // Create PaymentMethod from card data
            val paymentMethod = createPaymentMethod(cardData)

            CollectionState.Collected(paymentMethod)
        } catch (e: DigitalWalletNotSupportedException) {
            CollectionState.FailedCollection(
                error = e,
                displayMessage = "Digital wallets are not supported. Please tap your physical card.".resolvableString
            )
        } catch (e: CardDataExtractionException) {
            CollectionState.FailedCollection(
                error = e,
                displayMessage = "Could not read card data. Please try again.".resolvableString
            )
        } catch (e: EmvTransactionException) {
            CollectionState.FailedCollection(
                error = e,
                displayMessage = e.userMessage.resolvableString
            )
        } catch (e: OdaFailedException) {
            CollectionState.FailedCollection(
                error = e,
                displayMessage = "Card verification failed. This card may not be authentic.".resolvableString
            )
        } catch (e: IOException) {
            CollectionState.FailedCollection(
                error = e,
                displayMessage = "Card communication error. Please hold card steady and try again.".resolvableString
            )
        } catch (e: Exception) {
            CollectionState.FailedCollection(
                error = e,
                displayMessage = "An unexpected error occurred. Please try again.".resolvableString
            )
        } finally {
            connectionManager.disconnect()
        }
    }

    /**
     * Perform the EMV contactless transaction to read card data.
     */
    private fun performEmvTransaction(isoDep: IsoDep): CardDataExtractor.CardData {
        // Collect all TLV data for digital wallet detection
        val allTlvData = mutableMapOf<String, ByteArray>()

        // Step 1: SELECT PPSE
        val ppseResponse = transceive(isoDep, EmvApduCommands.SELECT_PPSE, "SELECT PPSE")
        val ppseData = EmvApduCommands.getResponseData(ppseResponse)
        allTlvData.putAll(TlvParser.parse(ppseData))

        // Extract AID from PPSE response
        val aid = cardDataExtractor.extractAid(ppseResponse)
            ?: throw EmvTransactionException(
                "No supported payment application found on card",
                "This card is not supported. Please try a different card."
            )

        // Step 2: SELECT AID
        val selectAidResponse = transceive(isoDep, EmvApduCommands.selectAid(aid), "SELECT AID")
        val selectAidData = EmvApduCommands.getResponseData(selectAidResponse)
        allTlvData.putAll(TlvParser.parse(selectAidData))

        // Extract PDOL for GPO command (may be null if not required by card)
        val pdol = cardDataExtractor.extractPdol(selectAidResponse)
        val pdolData = buildPdolData(pdol)

        // Step 3: GET PROCESSING OPTIONS
        val gpoResponse = try {
            transceive(
                isoDep,
                EmvApduCommands.getProcessingOptions(pdolData),
                "GET PROCESSING OPTIONS"
            )
        } catch (e: EmvTransactionException) {
            // Add PDOL context for debugging
            val pdolInfo = if (pdol != null) {
                val elements = parseDol(pdol)
                "PDOL requested: ${elements.joinToString { "${it.first}(${it.second})" }}"
            } else {
                "No PDOL requested"
            }
            throw EmvTransactionException(
                "${e.message}. $pdolInfo",
                e.userMessage,
                e.statusWord
            )
        }
        val gpoData = EmvApduCommands.getResponseData(gpoResponse)
        allTlvData.putAll(TlvParser.parse(gpoData))

        // Check for Track 2 in GPO Format 1 response (Visa qVSDC)
        cardDataExtractor.extractTrack2FromGpo(gpoResponse)?.let { track2 ->
            allTlvData[CardDataExtractor.TAG_TRACK2] = track2
        }

        // Check for digital wallet BEFORE reading full card data
        if (digitalWalletDetector.isDigitalWallet(allTlvData, aid)) {
            throw DigitalWalletNotSupportedException()
        }

        // Extract AFL (Application File Locator)
        val afl = cardDataExtractor.extractAfl(gpoResponse)
        Log.d(TAG, "AFL extracted: ${afl?.let { it.toHexString() } ?: "null"}")

        // Step 4: READ RECORD(s) if AFL is present
        if (afl != null) {
            readAllRecords(isoDep, afl, allTlvData)
        } else {
            Log.d(TAG, "No AFL - card may have returned all data in GPO (qVSDC mode)")
            // Try reading standard SFI locations for ODA data even without AFL
            tryReadStandardRecords(isoDep, allTlvData)
        }

        // Step 5: Perform Offline Data Authentication (ODA) if supported
        verifyOda(isoDep, allTlvData, aid)

        // Extract card data from accumulated TLV data (GPO + all records)
        return cardDataExtractor.extractFromTlvMap(allTlvData, aid)
    }

    /**
     * Read all records specified by the AFL.
     */
    private fun readAllRecords(
        isoDep: IsoDep,
        afl: ByteArray,
        allTlvData: MutableMap<String, ByteArray>
    ): ByteArray {
        val aflEntries = EmvApduCommands.parseAfl(afl)
        val recordData = mutableListOf<Byte>()

        Log.d(TAG, "AFL: ${afl.size} bytes, ${aflEntries.size} entries")
        for ((index, entry) in aflEntries.withIndex()) {
            val (sfi, firstRecord, lastRecord) = entry
            // 4th byte of AFL entry is ODA record count
            val odaCount = if (afl.size > index * 4 + 3) afl[index * 4 + 3].toInt() and 0xFF else 0
            Log.d(TAG, "AFL entry: SFI=$sfi, records=$firstRecord-$lastRecord, odaRecords=$odaCount")
        }

        for ((sfi, firstRecord, lastRecord) in aflEntries) {
            for (recordNum in firstRecord..lastRecord) {
                try {
                    val readRecordCmd = EmvApduCommands.readRecord(sfi, recordNum)
                    val response = transceive(isoDep, readRecordCmd, "READ RECORD $sfi:$recordNum")
                    val data = EmvApduCommands.getResponseData(response)

                    // Parse and log tags found in this record
                    val recordTlv = TlvParser.parse(data)
                    Log.d(TAG, "Record $sfi:$recordNum tags: ${recordTlv.keys.joinToString()}")

                    // Add to all TLV data for any additional checks
                    allTlvData.putAll(recordTlv)

                    // Accumulate record data
                    recordData.addAll(data.toList())
                } catch (e: EmvTransactionException) {
                    // Some records may not exist, continue to next
                    Log.d(TAG, "Record $sfi:$recordNum failed: ${e.statusWord}")
                    if (e.statusWord != SW_RECORD_NOT_FOUND && e.statusWord != SW_REFERENCED_DATA_NOT_FOUND) {
                        throw e
                    }
                }
            }
        }

        return recordData.toByteArray()
    }

    /**
     * Try reading standard record locations when AFL is not provided.
     *
     * Some cards in quick mode don't return AFL but still have ODA data
     * stored in standard SFI locations. This attempts to read those records.
     *
     * Standard locations for ODA data:
     * - SFI 1: Usually contains card data and sometimes ODA certs
     * - SFI 2: Often contains issuer certificates
     * - SFI 3: May contain additional certificates
     */
    private fun tryReadStandardRecords(
        isoDep: IsoDep,
        allTlvData: MutableMap<String, ByteArray>
    ) {
        val standardSfis = listOf(1, 2, 3)
        val maxRecords = 4

        for (sfi in standardSfis) {
            for (recordNum in 1..maxRecords) {
                try {
                    val readRecordCmd = EmvApduCommands.readRecord(sfi, recordNum)
                    val response = isoDep.transceive(readRecordCmd)

                    if (EmvApduCommands.isSuccess(response)) {
                        val data = EmvApduCommands.getResponseData(response)
                        val recordTlv = TlvParser.parse(data)
                        Log.d(TAG, "Standard record $sfi:$recordNum tags: ${recordTlv.keys.joinToString()}")
                        allTlvData.putAll(recordTlv)
                    }
                } catch (e: Exception) {
                    // Record doesn't exist or error reading, continue
                }
            }
        }
    }

    /**
     * Verify card authenticity using Offline Data Authentication (ODA).
     *
     * This performs either:
     * - DDA (Dynamic Data Authentication) if card supports it (has 9F46)
     * - SDA (Static Data Authentication) if card has 93 tag
     *
     * @throws OdaFailedException if verification fails
     */
    private fun verifyOda(isoDep: IsoDep, tlvData: Map<String, ByteArray>, aid: ByteArray) {
        // Check if ODA data is present
        if (!odaVerifier.hasOdaData(tlvData)) {
            // Log what ODA tags we found (or didn't find)
            val odaTags = listOf("8F", "90", "92", "93", "9F32", "9F46", "9F47", "9F48")
            val presentOdaTags = odaTags.filter { tlvData.containsKey(it) }
            Log.d(TAG, "ODA: Missing required data. Present ODA tags: $presentOdaTags, all tags: ${tlvData.keys.sorted()}")
            return
        }

        // Try DDA first (more secure), then fall back to SDA
        val result = if (odaVerifier.supportsDda(tlvData)) {
            Log.d(TAG, "ODA: Card supports DDA, performing INTERNAL AUTHENTICATE")
            performDda(isoDep, tlvData, aid)
        } else {
            Log.d(TAG, "ODA: Attempting SDA verification")
            odaVerifier.verifySda(tlvData, aid)
        }

        when (result) {
            is OdaVerifier.OdaResult.Success -> {
                Log.i(TAG, "ODA: Verification successful - card is authentic")
            }
            is OdaVerifier.OdaResult.NotSupported -> {
                Log.w(TAG, "ODA: Card does not support offline authentication")
                // Not an error - some cards don't support offline auth
            }
            is OdaVerifier.OdaResult.Failed -> {
                Log.e(TAG, "ODA: Verification failed - ${result.reason}")
                throw OdaFailedException(result.reason)
            }
        }
    }

    /**
     * Perform Dynamic Data Authentication (DDA).
     *
     * Tries multiple approaches:
     * 1. fDDA - Check if signed data is already in GPO response (tag 9F4B)
     * 2. Traditional DDA - Send INTERNAL AUTHENTICATE command
     * 3. Certificate verification - Verify the certificate chain is valid
     */
    private fun performDda(
        isoDep: IsoDep,
        tlvData: Map<String, ByteArray>,
        aid: ByteArray
    ): OdaVerifier.OdaResult {
        // Check for fDDA - signed data already in response (Visa qVSDC)
        val fddaSignedData = tlvData["9F4B"]
        if (fddaSignedData != null) {
            Log.d(TAG, "DDA: Found fDDA signed data (9F4B), ${fddaSignedData.size} bytes")
            // For fDDA, the auth data is derived from transaction data
            val authData = buildFddaAuthData(tlvData)
            return odaVerifier.verifyDda(tlvData, aid, fddaSignedData, authData)
        }

        // Try traditional INTERNAL AUTHENTICATE
        val authData = ByteArray(4).also { java.security.SecureRandom().nextBytes(it) }

        try {
            val authCommand = EmvApduCommands.internalAuthenticate(authData)
            val response = isoDep.transceive(authCommand)

            if (EmvApduCommands.isSuccess(response)) {
                val signedData = EmvApduCommands.getResponseData(response)
                Log.d(TAG, "DDA: Got ${signedData.size} bytes from INTERNAL AUTHENTICATE")
                return odaVerifier.verifyDda(tlvData, aid, signedData, authData)
            }

            val sw = EmvApduCommands.getStatusWord(response)
            Log.d(TAG, "INTERNAL AUTHENTICATE not supported: ${String.format("%04X", sw)}")
        } catch (e: Exception) {
            Log.d(TAG, "INTERNAL AUTHENTICATE failed: ${e.message}")
        }

        // Contactless cards often don't support INTERNAL AUTHENTICATE
        // but we can still verify the certificate chain is valid
        Log.d(TAG, "DDA: Verifying certificate chain only (contactless mode)")
        return verifyCertificateChain(tlvData, aid)
    }

    /**
     * Build authentication data for fDDA verification.
     * Includes unpredictable number and transaction data.
     */
    private fun buildFddaAuthData(tlvData: Map<String, ByteArray>): ByteArray {
        val result = mutableListOf<Byte>()
        // Include unpredictable number from terminal (9F37)
        tlvData["9F37"]?.let { result.addAll(it.toList()) }
        // Include amount (9F02)
        tlvData["9F02"]?.let { result.addAll(it.toList()) }
        // Include currency (5F2A)
        tlvData["5F2A"]?.let { result.addAll(it.toList()) }
        return if (result.isEmpty()) {
            ByteArray(4).also { java.security.SecureRandom().nextBytes(it) }
        } else {
            result.toByteArray()
        }
    }

    /**
     * Verify the certificate chain without dynamic signature.
     *
     * This verifies:
     * 1. CA Public Key is valid for this card's RID
     * 2. Issuer certificate can be recovered and parsed
     * 3. ICC certificate can be recovered and parsed
     *
     * This provides assurance the card has valid certificates from the
     * payment network, even if we can't perform dynamic authentication.
     */
    private fun verifyCertificateChain(
        tlvData: Map<String, ByteArray>,
        aid: ByteArray
    ): OdaVerifier.OdaResult {
        return odaVerifier.verifyCertificateChain(tlvData, aid)
    }

    /**
     * Build PDOL data from PDOL template.
     *
     * The PDOL specifies what data objects the card needs for GET PROCESSING OPTIONS.
     * We provide sensible default values for common EMV tags.
     */
    private fun buildPdolData(pdol: ByteArray?): ByteArray? {
        if (pdol == null || pdol.isEmpty()) return null

        // Parse PDOL to get tag-length pairs
        val pdolElements = parseDol(pdol)

        // Build response with appropriate values for each tag
        val result = mutableListOf<Byte>()
        for ((tag, length) in pdolElements) {
            val value = getDefaultValueForTag(tag, length)
            result.addAll(value.toList())
        }

        return result.toByteArray()
    }

    /**
     * Get default value for a PDOL tag.
     *
     * Common PDOL tags and their meanings:
     * - 9F66: Terminal Transaction Qualifiers (TTQ) - critical for contactless
     * - 9F02: Amount Authorized
     * - 9F03: Amount Other
     * - 9F1A: Terminal Country Code
     * - 5F2A: Transaction Currency Code
     * - 9A: Transaction Date
     * - 9C: Transaction Type
     * - 9F37: Unpredictable Number
     *
     * Amex Expresspay specific:
     * - 9F6C: Card Transaction Qualifiers (CTQ)
     * - 9F7A: VLP Terminal Support Indicator
     * - 9F6E: Enhanced Contactless Reader Capabilities
     */
    private fun getDefaultValueForTag(tag: String, length: Int): ByteArray {
        return when (tag.uppercase()) {
            // Terminal Transaction Qualifiers (TTQ) - 4 bytes
            // Byte 1:
            //   Bit 8: MSD supported = 0
            //   Bit 7: qVSDC supported = 1
            //   Bit 6: EMV mode supported = 1
            //   Bit 5: EMV contact chip supported = 0
            //   Bit 4: Offline-only reader = 0
            //   Bit 3: Online PIN supported = 0
            //   Bit 2: Signature supported = 0
            //   Bit 1: ODA for online auth supported = 1
            // Byte 2:
            //   Bit 8: Online cryptogram required = 1
            //   Bit 7: CVM required = 0
            //   Bit 6: Contact chip offline PIN supported = 0
            // This tells the card we support contactless EMV and want online auth
            "9F66" -> byteArrayOf(0x36, 0x80.toByte(), 0x00, 0x00).take(length).toByteArray()

            // Amount Authorized - typically 6 bytes BCD
            // Use small amount (1.00) to avoid CVM requirements
            "9F02" -> byteArrayOf(0x00, 0x00, 0x00, 0x00, 0x01, 0x00).take(length).toByteArray()

            // Amount Other - 6 bytes BCD (cashback, etc.)
            "9F03" -> ByteArray(length)

            // Terminal Country Code - 2 bytes (840 = USA)
            "9F1A" -> byteArrayOf(0x08, 0x40).take(length).toByteArray()

            // Transaction Currency Code - 2 bytes (840 = USD)
            "5F2A" -> byteArrayOf(0x08, 0x40).take(length).toByteArray()

            // Transaction Date - 3 bytes YYMMDD BCD
            "9A" -> getCurrentDateBcd().take(length).toByteArray()

            // Transaction Type - 1 byte (00 = purchase)
            "9C" -> byteArrayOf(0x00).take(length).toByteArray()

            // Unpredictable Number - 4 bytes random
            "9F37" -> generateUnpredictableNumber(length)

            // Terminal Type - 1 byte (22 = attended, online only)
            "9F35" -> byteArrayOf(0x22).take(length).toByteArray()

            // Terminal Capabilities - 3 bytes
            "9F33" -> byteArrayOf(0xE0.toByte(), 0xF0.toByte(), 0xC8.toByte()).take(length).toByteArray()

            // Additional Terminal Capabilities - 5 bytes
            "9F40" -> byteArrayOf(0x60, 0x00, 0x00, 0x00, 0x00).take(length).toByteArray()

            // === Amex Expresspay specific tags ===

            // Card Transaction Qualifiers (CTQ) - 2 bytes (Amex)
            // Byte 1: Online PIN required = 0, Signature required = 0
            // Byte 2: Go online = 1, Switch interface = 0
            "9F6C" -> byteArrayOf(0x00, 0x80.toByte()).take(length).toByteArray()

            // VLP Terminal Support Indicator - 1 byte (Amex)
            // 01 = VLP (contactless) supported
            "9F7A" -> byteArrayOf(0x01).take(length).toByteArray()

            // Enhanced Contactless Reader Capabilities - 4 bytes (Amex)
            // Indicates terminal supports contactless EMV
            "9F6E" -> byteArrayOf(
                0xD8.toByte(), // Contactless supported, online capable
                0x40,          // Support for EMV mode
                0x00,
                0x00
            ).take(length).toByteArray()

            // Mobile Support Indicator - 1 byte (Amex)
            // 01 = Mobile/NFC supported
            "9F7E" -> byteArrayOf(0x01).take(length).toByteArray()

            // Transaction Time - 3 bytes HHMMSS BCD
            "9F21" -> getCurrentTimeBcd().take(length).toByteArray()

            // Point of Service Entry Mode - 1 byte
            // 07 = Contactless chip
            "9F39" -> byteArrayOf(0x07).take(length).toByteArray()

            // Default: return zeros for unknown tags
            else -> ByteArray(length)
        }
    }

    private fun getCurrentDateBcd(): ByteArray {
        val calendar = java.util.Calendar.getInstance()
        val year = calendar.get(java.util.Calendar.YEAR) % 100
        val month = calendar.get(java.util.Calendar.MONTH) + 1
        val day = calendar.get(java.util.Calendar.DAY_OF_MONTH)
        return byteArrayOf(
            ((year / 10) shl 4 or (year % 10)).toByte(),
            ((month / 10) shl 4 or (month % 10)).toByte(),
            ((day / 10) shl 4 or (day % 10)).toByte()
        )
    }

    private fun getCurrentTimeBcd(): ByteArray {
        val calendar = java.util.Calendar.getInstance()
        val hour = calendar.get(java.util.Calendar.HOUR_OF_DAY)
        val minute = calendar.get(java.util.Calendar.MINUTE)
        val second = calendar.get(java.util.Calendar.SECOND)
        return byteArrayOf(
            ((hour / 10) shl 4 or (hour % 10)).toByte(),
            ((minute / 10) shl 4 or (minute % 10)).toByte(),
            ((second / 10) shl 4 or (second % 10)).toByte()
        )
    }

    private fun generateUnpredictableNumber(length: Int): ByteArray {
        return ByteArray(length).also { java.security.SecureRandom().nextBytes(it) }
    }

    /**
     * Parse a Data Object List (DOL) to get tag-length pairs.
     */
    private fun parseDol(dol: ByteArray): List<Pair<String, Int>> {
        val result = mutableListOf<Pair<String, Int>>()
        var offset = 0

        while (offset < dol.size) {
            // Parse tag (1 or 2 bytes)
            val firstByte = dol[offset].toInt() and 0xFF
            val tagLength = if ((firstByte and 0x1F) == 0x1F) 2 else 1
            val tag = dol.copyOfRange(offset, offset + tagLength)
                .joinToString("") { String.format("%02X", it) }
            offset += tagLength

            // Parse length
            if (offset >= dol.size) break
            val length = dol[offset].toInt() and 0xFF
            offset++

            result.add(tag to length)
        }

        return result
    }

    /**
     * Send APDU command and verify response.
     */
    private fun transceive(isoDep: IsoDep, command: ByteArray, commandName: String): ByteArray {
        val response = isoDep.transceive(command)

        if (!EmvApduCommands.isSuccess(response)) {
            val sw = EmvApduCommands.getStatusWord(response)
            throw EmvTransactionException(
                "$commandName failed with status ${String.format("%04X", sw)}",
                "Card communication error. Please try again.",
                sw
            )
        }

        return response
    }

    /**
     * Create PaymentMethod from extracted card data.
     *
     * Note: For this POC, we generate a local ID. In production, this should
     * call the Stripe API to create a real PaymentMethod. However, that would
     * require additional work since:
     * 1. We don't have CVC from NFC (not stored on chip)
     * 2. Raw PAN handling requires careful PCI consideration
     * 3. May need a server-side component for secure tokenization
     */
    private fun createPaymentMethod(cardData: CardDataExtractor.CardData): PaymentMethod {
        // Generate a local ID for POC purposes
        // Format: pm_nfc_<random>
        val localId = "pm_nfc_${java.util.UUID.randomUUID().toString().replace("-", "").take(24)}"

        return PaymentMethod.Builder()
            .setId(localId)
            .setCode(PaymentMethod.Type.Card.code)
            .setType(PaymentMethod.Type.Card)
            .setCard(
                PaymentMethod.Card(
                    last4 = cardData.last4,
                    brand = cardData.scheme.toCardBrand(),
                    expiryMonth = cardData.expiryMonth,
                    expiryYear = cardData.expiryYear,
                )
            )
            .setCreated(System.currentTimeMillis() / 1000)
            .setLiveMode(false)
            .build()
    }

    private fun CardDataExtractor.CardScheme.toCardBrand(): CardBrand {
        return when (this) {
            CardDataExtractor.CardScheme.VISA -> CardBrand.Visa
            CardDataExtractor.CardScheme.MASTERCARD -> CardBrand.MasterCard
            CardDataExtractor.CardScheme.AMEX -> CardBrand.AmericanExpress
            CardDataExtractor.CardScheme.DISCOVER -> CardBrand.Discover
            CardDataExtractor.CardScheme.JCB -> CardBrand.JCB
            CardDataExtractor.CardScheme.UNIONPAY -> CardBrand.UnionPay
            CardDataExtractor.CardScheme.UNKNOWN -> CardBrand.Unknown
        }
    }
}

/**
 * Exception for EMV transaction errors.
 */
internal class EmvTransactionException(
    message: String,
    val userMessage: String,
    val statusWord: Int = 0
) : Exception(message)
