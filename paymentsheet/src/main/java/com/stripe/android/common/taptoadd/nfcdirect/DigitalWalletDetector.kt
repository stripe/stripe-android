package com.stripe.android.common.taptoadd.nfcdirect

import com.stripe.android.common.taptoadd.nfcdirect.TlvParser.toHexString

/**
 * Detects digital wallet taps (Apple Pay, Google Pay, Samsung Pay, etc.).
 *
 * Digital wallets use tokenized PANs (DPANs) instead of the actual card number.
 * For "Tap to Add" scenarios where we need the actual card number for CNP
 * transactions, we must reject wallet taps and prompt the user to tap
 * their physical card instead.
 *
 * Detection is based on EMV tags that indicate tokenized transactions:
 * - Mastercard: CVM Results (9F34) with specific patterns
 * - Visa: Card Transaction Qualifiers (C1) presence
 * - Amex: Enhanced Contactless Reader Capabilities (9F71)
 * - Discover: Issuer Application Data (9F10) with DPAN indicator
 */
internal class DigitalWalletDetector {

    companion object {
        // EMV tags used for wallet detection
        const val TAG_CVM_RESULTS = "9F34"         // Mastercard
        const val TAG_CTQ = "C1"                   // Visa Card Transaction Qualifiers
        const val TAG_ENHANCED_CAPABILITIES = "9F71" // Amex
        const val TAG_IAD = "9F10"                 // Issuer Application Data (Discover)
        const val TAG_AID = "4F"                   // Application Identifier
        const val TAG_DF_NAME = "84"               // DF Name

        // Mastercard DPAN indicator in CVM Results
        // When byte 1 bit 4 is set, indicates mobile payment
        private const val MC_MOBILE_CVM_MASK = 0x08

        // Discover DPAN indicator position in IAD
        private const val DISCOVER_DPAN_BYTE_INDEX = 0
        private const val DISCOVER_DPAN_INDICATOR = 0x01
    }

    /**
     * Check if the card tap is from a digital wallet.
     *
     * @param allTlvData Combined TLV data from GPO and READ RECORD responses
     * @param aid The selected application AID
     * @return true if this appears to be a digital wallet tap
     */
    fun isDigitalWallet(allTlvData: Map<String, ByteArray>, aid: ByteArray): Boolean {
        val scheme = detectScheme(aid)

        return when (scheme) {
            CardScheme.MASTERCARD -> checkMastercard(allTlvData)
            CardScheme.VISA -> checkVisa(allTlvData)
            CardScheme.AMEX -> checkAmex(allTlvData)
            CardScheme.DISCOVER -> checkDiscover(allTlvData)
            else -> false // Conservative: allow unknown schemes
        }
    }

    /**
     * Mastercard digital wallet detection.
     *
     * Checks CVM Results (9F34) for mobile payment indicators.
     * Mobile wallets typically use CDCVM (Consumer Device CVM) which sets
     * specific bits in the CVM Results.
     */
    private fun checkMastercard(tlv: Map<String, ByteArray>): Boolean {
        val cvmResults = tlv[TAG_CVM_RESULTS] ?: return false

        if (cvmResults.isEmpty()) return false

        // Check for mobile/CDCVM indicator
        val cvmType = cvmResults[0].toInt() and 0xFF
        if ((cvmType and MC_MOBILE_CVM_MASK) != 0) {
            return true
        }

        // Additional check: CVM performed on consumer device
        // Values 0x1F and 0x1E typically indicate mobile device verification
        if (cvmType == 0x1F || cvmType == 0x1E) {
            return true
        }

        return false
    }

    /**
     * Visa digital wallet detection.
     *
     * Visa digital wallets include the Card Transaction Qualifiers (CTQ) tag C1
     * in their responses. This tag is typically only present for tokenized
     * transactions.
     */
    private fun checkVisa(tlv: Map<String, ByteArray>): Boolean {
        // Presence of CTQ tag often indicates digital wallet
        val ctq = tlv[TAG_CTQ]
        if (ctq != null && ctq.isNotEmpty()) {
            // Bit 8 of byte 1 indicates online cryptogram required
            // Bit 7 indicates CVM required
            // Mobile wallets typically have both
            val byte1 = ctq[0].toInt() and 0xFF
            if ((byte1 and 0xC0) == 0xC0) {
                return true
            }
        }

        return false
    }

    /**
     * American Express digital wallet detection.
     *
     * Amex digital wallets include Enhanced Contactless Reader Capabilities (9F71)
     * with specific mobile payment indicators.
     */
    private fun checkAmex(tlv: Map<String, ByteArray>): Boolean {
        val enhancedCap = tlv[TAG_ENHANCED_CAPABILITIES] ?: return false

        if (enhancedCap.isEmpty()) return false

        // Check for mobile payment indicator in first byte
        val flags = enhancedCap[0].toInt() and 0xFF

        // Bit 1 typically indicates mobile payment capability
        return (flags and 0x01) != 0
    }

    /**
     * Discover digital wallet detection.
     *
     * Discover uses Issuer Application Data (IAD) tag 9F10 to indicate
     * whether the PAN is a Device PAN (DPAN) or the actual FPAN.
     */
    private fun checkDiscover(tlv: Map<String, ByteArray>): Boolean {
        val iad = tlv[TAG_IAD] ?: return false

        if (iad.size <= DISCOVER_DPAN_BYTE_INDEX) return false

        // Check DPAN indicator byte
        val indicator = iad[DISCOVER_DPAN_BYTE_INDEX].toInt() and 0xFF

        return (indicator and DISCOVER_DPAN_INDICATOR) != 0
    }

    private fun detectScheme(aid: ByteArray): CardScheme {
        val aidHex = aid.toHexString().uppercase()

        return when {
            aidHex.startsWith("A000000003") -> CardScheme.VISA
            aidHex.startsWith("A000000004") -> CardScheme.MASTERCARD
            aidHex.startsWith("A000000025") -> CardScheme.AMEX
            aidHex.startsWith("A000000152") -> CardScheme.DISCOVER
            else -> CardScheme.UNKNOWN
        }
    }

    private enum class CardScheme {
        VISA,
        MASTERCARD,
        AMEX,
        DISCOVER,
        UNKNOWN
    }
}

/**
 * Exception thrown when a digital wallet tap is detected.
 */
internal class DigitalWalletNotSupportedException(
    message: String = "Digital wallets are not supported. Please tap your physical card."
) : Exception(message)
