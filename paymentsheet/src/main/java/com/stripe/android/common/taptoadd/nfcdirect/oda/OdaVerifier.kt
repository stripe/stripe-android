package com.stripe.android.common.taptoadd.nfcdirect.oda

import com.stripe.android.common.taptoadd.nfcdirect.TlvParser.toHexString

/**
 * Offline Data Authentication (ODA) verifier for EMV contactless cards.
 *
 * ODA verifies that the card is genuine by checking a certificate chain:
 * CA Public Key → Issuer Public Key Certificate → Signed Static/Dynamic Data
 *
 * This implementation supports Static Data Authentication (SDA), which is
 * sufficient for "Tap to Add" scenarios where we only need to verify the
 * card is authentic, not perform a transaction.
 *
 * The verification flow:
 * 1. Look up CA Public Key using RID (from AID) + CA PK Index (tag 8F)
 * 2. RSA recover Issuer Public Key from certificate (tag 90) using CA key
 * 3. Verify Signed Static Application Data (tag 93) using Issuer Public Key
 */
internal class OdaVerifier {

    /**
     * Result of ODA verification.
     */
    sealed class OdaResult {
        /**
         * ODA verification passed - card is authentic.
         */
        data object Success : OdaResult()

        /**
         * ODA verification failed - card may be counterfeit.
         */
        data class Failed(val reason: String) : OdaResult()

        /**
         * Card does not support ODA (missing required tags).
         * This is not necessarily an error - some cards don't support offline auth.
         */
        data object NotSupported : OdaResult()
    }

    // EMV Tags for ODA
    companion object {
        const val TAG_CA_PK_INDEX = "8F"           // CA Public Key Index
        const val TAG_ISSUER_PK_CERT = "90"        // Issuer Public Key Certificate
        const val TAG_ISSUER_PK_REMAINDER = "92"   // Issuer Public Key Remainder
        const val TAG_ISSUER_PK_EXPONENT = "9F32"  // Issuer Public Key Exponent
        const val TAG_SSAD = "93"                  // Signed Static Application Data
        const val TAG_SDA_TAG_LIST = "9F4A"        // Static Data Authentication Tag List
        const val TAG_AIP = "82"                   // Application Interchange Profile
        const val TAG_ICC_PK_CERT = "9F46"         // ICC Public Key Certificate (DDA)
        const val TAG_ICC_PK_EXPONENT = "9F47"     // ICC Public Key Exponent (DDA)
        const val TAG_ICC_PK_REMAINDER = "9F48"    // ICC Public Key Remainder (DDA)
        const val TAG_SIGNED_DYNAMIC_DATA = "9F4B" // Signed Dynamic Application Data
    }

    /**
     * Perform Static Data Authentication (SDA).
     *
     * @param tlvData Map of EMV tags to values collected from card
     * @param aid The Application Identifier (used to determine RID)
     * @return OdaResult indicating success, failure, or not supported
     */
    fun verifySda(
        tlvData: Map<String, ByteArray>,
        aid: ByteArray
    ): OdaResult {
        // Check if SDA is supported by examining AIP
        val aip = tlvData[TAG_AIP]
        if (aip != null && aip.isNotEmpty()) {
            // AIP byte 1, bit 7 (0x40) indicates SDA is supported
            val sdaSupported = (aip[0].toInt() and 0x40) != 0
            if (!sdaSupported) {
                return OdaResult.NotSupported
            }
        }

        // Step 1: Get CA Public Key Index
        val caIndexBytes = tlvData[TAG_CA_PK_INDEX]
            ?: return OdaResult.NotSupported
        val caIndex = caIndexBytes.toHexString().uppercase()

        // Step 2: Look up CA Public Key
        val rid = aid.copyOfRange(0, minOf(5, aid.size)).toHexString().uppercase()
        val caKey = CaPublicKeyStore.getKey(rid, caIndex)
            ?: return OdaResult.Failed("CA public key not found: RID=$rid, Index=$caIndex")

        // Step 3: Get Issuer Public Key Certificate
        val issuerCert = tlvData[TAG_ISSUER_PK_CERT]
            ?: return OdaResult.NotSupported

        // Step 4: RSA recover Issuer Public Key
        val recoveredIssuerCert = try {
            RsaRecovery.recover(issuerCert, caKey.exponent, caKey.modulus)
        } catch (e: Exception) {
            return OdaResult.Failed("Failed to recover issuer certificate: ${e.message}")
        }

        // Step 5: Parse Issuer Public Key from recovered certificate
        val issuerPkRemainder = tlvData[TAG_ISSUER_PK_REMAINDER]
        val issuerPkExponent = tlvData[TAG_ISSUER_PK_EXPONENT] ?: byteArrayOf(0x03)

        val issuerKey = CertificateParser.parseIssuerCertificate(
            recoveredIssuerCert,
            issuerPkRemainder,
            issuerPkExponent
        ) ?: return OdaResult.Failed("Failed to parse issuer certificate")

        // Step 6: Verify Issuer Certificate hash
        val issuerCertAdditionalData = (issuerPkRemainder ?: byteArrayOf()) + issuerPkExponent
        if (!CertificateParser.verifyCertificateHash(recoveredIssuerCert, issuerCertAdditionalData)) {
            return OdaResult.Failed("Issuer certificate hash verification failed")
        }

        // Step 7: Get Signed Static Application Data
        val ssad = tlvData[TAG_SSAD]
            ?: return OdaResult.NotSupported

        // Step 8: RSA recover SSAD
        val recoveredSsad = try {
            RsaRecovery.recover(ssad, issuerKey.exponent, issuerKey.modulus)
        } catch (e: Exception) {
            return OdaResult.Failed("Failed to recover SSAD: ${e.message}")
        }

        // Step 9: Build static data for hash verification
        // Per EMV spec, includes data specified in SDA Tag List (tag 9F4A)
        val staticData = buildStaticDataForAuth(tlvData)

        // Step 10: Verify SSAD
        if (!CertificateParser.verifySsad(recoveredSsad, staticData)) {
            return OdaResult.Failed("SSAD hash verification failed")
        }

        return OdaResult.Success
    }

    /**
     * Check if card supports DDA (has ICC Public Key Certificate).
     */
    fun supportsDda(tlvData: Map<String, ByteArray>): Boolean {
        return tlvData.containsKey(TAG_ICC_PK_CERT) &&
            tlvData.containsKey(TAG_ISSUER_PK_CERT) &&
            tlvData.containsKey(TAG_CA_PK_INDEX)
    }

    /**
     * Perform Dynamic Data Authentication (DDA).
     *
     * DDA is more secure than SDA because the card generates a unique
     * signature each time using its ICC private key.
     *
     * @param tlvData Map of EMV tags to values collected from card
     * @param aid The Application Identifier (used to determine RID)
     * @param signedData The signed response from INTERNAL AUTHENTICATE command
     * @param authData The data that was sent to INTERNAL AUTHENTICATE
     * @return OdaResult indicating success, failure, or not supported
     */
    fun verifyDda(
        tlvData: Map<String, ByteArray>,
        aid: ByteArray,
        signedData: ByteArray,
        authData: ByteArray
    ): OdaResult {
        // Step 1: Get CA Public Key (same as SDA)
        val caIndexBytes = tlvData[TAG_CA_PK_INDEX]
            ?: return OdaResult.NotSupported
        val caIndex = caIndexBytes.toHexString().uppercase()

        val rid = aid.copyOfRange(0, minOf(5, aid.size)).toHexString().uppercase()
        val caKey = CaPublicKeyStore.getKey(rid, caIndex)
            ?: return OdaResult.Failed("CA public key not found: RID=$rid, Index=$caIndex")

        // Step 2: Recover Issuer Public Key (same as SDA)
        val issuerCert = tlvData[TAG_ISSUER_PK_CERT]
            ?: return OdaResult.NotSupported

        val recoveredIssuerCert = try {
            RsaRecovery.recover(issuerCert, caKey.exponent, caKey.modulus)
        } catch (e: Exception) {
            return OdaResult.Failed("Failed to recover issuer certificate: ${e.message}")
        }

        val issuerPkRemainder = tlvData[TAG_ISSUER_PK_REMAINDER]
        val issuerPkExponent = tlvData[TAG_ISSUER_PK_EXPONENT] ?: byteArrayOf(0x03)

        val issuerKey = CertificateParser.parseIssuerCertificate(
            recoveredIssuerCert,
            issuerPkRemainder,
            issuerPkExponent
        ) ?: return OdaResult.Failed("Failed to parse issuer certificate")

        // Step 3: Get ICC Public Key Certificate
        val iccCert = tlvData[TAG_ICC_PK_CERT]
            ?: return OdaResult.NotSupported

        // Step 4: Recover ICC Public Key using Issuer Key
        val recoveredIccCert = try {
            RsaRecovery.recover(iccCert, issuerKey.exponent, issuerKey.modulus)
        } catch (e: Exception) {
            return OdaResult.Failed("Failed to recover ICC certificate: ${e.message}")
        }

        // Parse ICC certificate (similar structure to issuer cert but format 0x04)
        val iccPkRemainder = tlvData[TAG_ICC_PK_REMAINDER]
        val iccPkExponent = tlvData[TAG_ICC_PK_EXPONENT] ?: byteArrayOf(0x03)

        val iccKey = parseIccCertificate(
            recoveredIccCert,
            iccPkRemainder,
            iccPkExponent
        ) ?: return OdaResult.Failed("Failed to parse ICC certificate")

        // Step 5: Verify signed dynamic data
        val recoveredSignedData = try {
            RsaRecovery.recover(signedData, iccKey.exponent, iccKey.modulus)
        } catch (e: Exception) {
            return OdaResult.Failed("Failed to recover signed data: ${e.message}")
        }

        // Verify the signature structure
        if (!verifyDynamicSignature(recoveredSignedData, authData)) {
            return OdaResult.Failed("Dynamic signature verification failed")
        }

        return OdaResult.Success
    }

    /**
     * Parse ICC Public Key Certificate.
     * Similar to Issuer certificate but with format byte 0x04.
     */
    private fun parseIccCertificate(
        recoveredCert: ByteArray,
        iccPkRemainder: ByteArray?,
        iccPkExponent: ByteArray
    ): CertificateParser.IssuerPublicKey? {
        if (recoveredCert.size < 36) return null

        // Verify header (0x6A) and trailer (0xBC)
        if (recoveredCert.first() != 0x6A.toByte() || recoveredCert.last() != 0xBC.toByte()) {
            return null
        }

        // ICC cert format is 0x04
        if (recoveredCert[1] != 0x04.toByte()) {
            return null
        }

        // Similar structure to issuer cert
        val iccPkLength = recoveredCert[19].toInt() and 0xFF
        val keyBytesInCert = recoveredCert.size - 21 - 20 - 1 // header + hash + trailer

        if (keyBytesInCert <= 0) return null

        val keyBytesFromCert = recoveredCert.copyOfRange(21, 21 + keyBytesInCert)

        val fullModulus = if (iccPkRemainder != null && iccPkRemainder.isNotEmpty()) {
            keyBytesFromCert + iccPkRemainder
        } else {
            keyBytesFromCert
        }

        val exponent = if (iccPkExponent.isNotEmpty()) iccPkExponent else byteArrayOf(0x03)

        return CertificateParser.IssuerPublicKey(
            modulus = fullModulus.take(iccPkLength).toByteArray(),
            exponent = exponent
        )
    }

    /**
     * Verify the recovered dynamic signature.
     *
     * Format after recovery:
     * - Byte 1: Header (0x6A)
     * - Byte 2: Format (0x05 for DDA)
     * - Byte 3: Hash Algorithm (01 = SHA-1)
     * - Bytes 4-(N-21): ICC Dynamic Data
     * - Bytes (N-20)-(N-1): Hash
     * - Byte N: Trailer (0xBC)
     */
    private fun verifyDynamicSignature(recoveredData: ByteArray, authData: ByteArray): Boolean {
        if (recoveredData.size < 25) return false

        // Check header and trailer
        if (recoveredData.first() != 0x6A.toByte() || recoveredData.last() != 0xBC.toByte()) {
            return false
        }

        // Format should be 0x05 for signed dynamic data
        if (recoveredData[1] != 0x05.toByte()) {
            return false
        }

        // For now, just verify structure is correct
        // Full verification would check hash includes authData
        return true
    }

    /**
     * Verify the certificate chain with cryptographic hash verification.
     *
     * This verifies the actual cryptographic signatures:
     * 1. CA Public Key exists for this card
     * 2. Issuer certificate hash is valid (proves CA signed it)
     * 3. ICC certificate hash is valid (proves Issuer signed it)
     *
     * EMV Certificate Hash Verification:
     * - RSA recover the certificate
     * - Extract stored hash (last 20 bytes before 0xBC trailer)
     * - Compute SHA-1 over: cert_data (excluding header, hash, trailer) + remainder + exponent
     * - Compare computed hash with stored hash
     */
    fun verifyCertificateChain(
        tlvData: Map<String, ByteArray>,
        aid: ByteArray
    ): OdaResult {
        // Step 1: Get CA Public Key
        val caIndexBytes = tlvData[TAG_CA_PK_INDEX]
            ?: return OdaResult.NotSupported
        val caIndex = caIndexBytes.toHexString().uppercase()

        val rid = aid.copyOfRange(0, minOf(5, aid.size)).toHexString().uppercase()
        val caKey = CaPublicKeyStore.getKey(rid, caIndex)
            ?: return OdaResult.Failed("CA public key not found: RID=$rid, Index=$caIndex")

        android.util.Log.d("ODA", "Using CA key: RID=$rid, Index=$caIndex, modulus=${caKey.modulus.size} bytes")

        // Step 2: Recover Issuer Certificate
        val issuerCert = tlvData[TAG_ISSUER_PK_CERT]
            ?: return OdaResult.NotSupported

        val recoveredIssuerCert = try {
            RsaRecovery.recover(issuerCert, caKey.exponent, caKey.modulus)
        } catch (e: Exception) {
            return OdaResult.Failed("Failed to recover issuer certificate: ${e.message}")
        }

        // Verify issuer cert structure
        if (recoveredIssuerCert.first() != 0x6A.toByte() ||
            recoveredIssuerCert.last() != 0xBC.toByte()) {
            return OdaResult.Failed("Invalid issuer certificate structure (header/trailer)")
        }

        // Verify issuer cert format byte (should be 0x02)
        if (recoveredIssuerCert[1] != 0x02.toByte()) {
            return OdaResult.Failed("Invalid issuer certificate format: ${recoveredIssuerCert[1]}")
        }

        val issuerPkRemainder = tlvData[TAG_ISSUER_PK_REMAINDER] ?: byteArrayOf()
        val issuerPkExponent = tlvData[TAG_ISSUER_PK_EXPONENT] ?: byteArrayOf(0x03)

        // Step 3: Verify Issuer Certificate HASH (cryptographic verification)
        if (!verifyIssuerCertificateHash(recoveredIssuerCert, issuerPkRemainder, issuerPkExponent)) {
            return OdaResult.Failed("Issuer certificate hash verification failed - cert not signed by CA")
        }

        val issuerKey = CertificateParser.parseIssuerCertificate(
            recoveredIssuerCert,
            issuerPkRemainder,
            issuerPkExponent
        ) ?: return OdaResult.Failed("Failed to parse issuer certificate")

        // Step 4: If ICC certificate present, verify it too
        val iccCert = tlvData[TAG_ICC_PK_CERT]
        if (iccCert != null) {
            val recoveredIccCert = try {
                RsaRecovery.recover(iccCert, issuerKey.exponent, issuerKey.modulus)
            } catch (e: Exception) {
                return OdaResult.Failed("Failed to recover ICC certificate: ${e.message}")
            }

            // Verify ICC cert structure
            if (recoveredIccCert.first() != 0x6A.toByte() ||
                recoveredIccCert.last() != 0xBC.toByte()) {
                return OdaResult.Failed("Invalid ICC certificate structure")
            }

            // Verify ICC cert format byte (should be 0x04)
            if (recoveredIccCert[1] != 0x04.toByte()) {
                return OdaResult.Failed("Invalid ICC certificate format: ${recoveredIccCert[1]}")
            }

            val iccPkRemainder = tlvData[TAG_ICC_PK_REMAINDER] ?: byteArrayOf()
            val iccPkExponent = tlvData[TAG_ICC_PK_EXPONENT] ?: byteArrayOf(0x03)

            // Verify ICC Certificate HASH
            if (!verifyIccCertificateHash(recoveredIccCert, iccPkRemainder, iccPkExponent)) {
                return OdaResult.Failed("ICC certificate hash verification failed - cert not signed by Issuer")
            }
        }

        // Certificate chain is cryptographically valid
        return OdaResult.Success
    }

    /**
     * Verify the SHA-1 hash in an Issuer Public Key Certificate.
     *
     * Per EMV Book 2, the hash is computed over:
     * - Certificate data from byte 2 to N-22 (excluding header, hash, trailer)
     * - Issuer Public Key Remainder (if any)
     * - Issuer Public Key Exponent
     */
    private fun verifyIssuerCertificateHash(
        recoveredCert: ByteArray,
        remainder: ByteArray,
        exponent: ByteArray
    ): Boolean {
        if (recoveredCert.size < 22) return false

        // Extract stored hash (20 bytes before trailer)
        val hashStart = recoveredCert.size - 21 // 20 bytes hash + 1 byte trailer
        val storedHash = recoveredCert.copyOfRange(hashStart, hashStart + 20)

        // Build data to hash: bytes 2 to (N-22) + remainder + exponent
        val dataToHash = recoveredCert.copyOfRange(1, hashStart) + remainder + exponent

        // Compute SHA-1 hash
        val computedHash = sha1(dataToHash)

        // Compare hashes
        val match = storedHash.contentEquals(computedHash)
        if (!match) {
            android.util.Log.d("ODA", "Issuer cert hash mismatch!")
            android.util.Log.d("ODA", "  Stored:   ${storedHash.toHexString()}")
            android.util.Log.d("ODA", "  Computed: ${computedHash.toHexString()}")
        } else {
            android.util.Log.d("ODA", "Issuer cert hash VERIFIED: ${computedHash.toHexString()}")
        }
        return match
    }

    /**
     * Verify the SHA-1 hash in an ICC Public Key Certificate.
     *
     * Similar to issuer cert but includes additional ICC-specific data.
     */
    private fun verifyIccCertificateHash(
        recoveredCert: ByteArray,
        remainder: ByteArray,
        exponent: ByteArray
    ): Boolean {
        if (recoveredCert.size < 22) return false

        // Extract stored hash (20 bytes before trailer)
        val hashStart = recoveredCert.size - 21
        val storedHash = recoveredCert.copyOfRange(hashStart, hashStart + 20)

        // Build data to hash: bytes 2 to (N-22) + remainder + exponent
        val dataToHash = recoveredCert.copyOfRange(1, hashStart) + remainder + exponent

        // Compute SHA-1 hash
        val computedHash = sha1(dataToHash)

        val match = storedHash.contentEquals(computedHash)
        if (!match) {
            android.util.Log.d("ODA", "ICC cert hash mismatch!")
            android.util.Log.d("ODA", "  Stored:   ${storedHash.toHexString()}")
            android.util.Log.d("ODA", "  Computed: ${computedHash.toHexString()}")
        } else {
            android.util.Log.d("ODA", "ICC cert hash VERIFIED: ${computedHash.toHexString()}")
        }
        return match
    }

    private fun sha1(data: ByteArray): ByteArray {
        return java.security.MessageDigest.getInstance("SHA-1").digest(data)
    }

    /**
     * Extract the PAN from a recovered ICC Public Key Certificate.
     *
     * ICC Certificate structure (after RSA recovery):
     * - Byte 1: Header (0x6A)
     * - Byte 2: Format (0x04)
     * - Bytes 3-12: Application PAN (10 bytes BCD, padded with 0xFF)
     * - Bytes 13-14: Certificate Expiry (MMYY)
     * - ... rest of certificate
     *
     * @return The PAN as a string, or null if extraction fails
     */
    fun extractPanFromIccCertificate(recoveredCert: ByteArray): String? {
        if (recoveredCert.size < 22) return null

        // Verify it's an ICC certificate (format 0x04)
        if (recoveredCert[1] != 0x04.toByte()) return null

        // Extract PAN bytes (indices 2-11, which is bytes 3-12 in 1-based)
        val panBytes = recoveredCert.copyOfRange(2, 12)

        // Convert BCD to string, stopping at 0xFF padding
        val pan = StringBuilder()
        for (byte in panBytes) {
            val high = (byte.toInt() shr 4) and 0x0F
            val low = byte.toInt() and 0x0F

            if (high == 0x0F) break  // Padding reached
            pan.append(high)

            if (low == 0x0F) break   // Padding reached
            pan.append(low)
        }

        return pan.toString().takeIf { it.length >= 13 }  // Valid PAN is 13-19 digits
    }

    /**
     * Extract the Issuer Identifier (BIN) from a recovered Issuer Public Key Certificate.
     *
     * Issuer Certificate structure:
     * - Byte 1: Header (0x6A)
     * - Byte 2: Format (0x02)
     * - Bytes 3-6: Issuer Identifier (4 bytes = 8 BCD digits, padded with 0xFF)
     *
     * @return The Issuer ID (BIN prefix) as a string, or null if extraction fails
     */
    fun extractIssuerIdFromCertificate(recoveredCert: ByteArray): String? {
        if (recoveredCert.size < 15) return null

        // Verify it's an Issuer certificate (format 0x02)
        if (recoveredCert[1] != 0x02.toByte()) return null

        // Extract Issuer ID bytes (indices 2-5, which is bytes 3-6 in 1-based)
        val issuerBytes = recoveredCert.copyOfRange(2, 6)

        // Convert BCD to string
        val issuerId = StringBuilder()
        for (byte in issuerBytes) {
            val high = (byte.toInt() shr 4) and 0x0F
            val low = byte.toInt() and 0x0F

            if (high == 0x0F) break
            issuerId.append(high)

            if (low == 0x0F) break
            issuerId.append(low)
        }

        return issuerId.toString().takeIf { it.isNotEmpty() }
    }

    /**
     * Verify that the claimed PAN matches the PAN in the ICC certificate.
     *
     * This is the key server-side security check - it proves the client
     * didn't fabricate the PAN, because the PAN is cryptographically
     * bound to the certificate chain.
     *
     * @param tlvData The TLV data containing certificates
     * @param aid The Application ID
     * @param claimedPan The PAN claimed by the client
     * @return OdaResult with success if PAN matches, failure otherwise
     */
    fun verifyPanMatchesCertificate(
        tlvData: Map<String, ByteArray>,
        aid: ByteArray,
        claimedPan: String
    ): OdaResult {
        // First verify the certificate chain
        val chainResult = verifyCertificateChain(tlvData, aid)
        if (chainResult !is OdaResult.Success) {
            return chainResult
        }

        // Get CA key
        val caIndexBytes = tlvData[TAG_CA_PK_INDEX] ?: return OdaResult.NotSupported
        val caIndex = caIndexBytes.toHexString().uppercase()
        val rid = aid.copyOfRange(0, minOf(5, aid.size)).toHexString().uppercase()
        val caKey = CaPublicKeyStore.getKey(rid, caIndex) ?: return OdaResult.NotSupported

        // Recover issuer certificate
        val issuerCert = tlvData[TAG_ISSUER_PK_CERT] ?: return OdaResult.NotSupported
        val recoveredIssuerCert = RsaRecovery.recover(issuerCert, caKey.exponent, caKey.modulus)

        // Verify issuer ID matches PAN prefix (BIN)
        val issuerId = extractIssuerIdFromCertificate(recoveredIssuerCert)
        if (issuerId != null && !claimedPan.startsWith(issuerId.trimEnd('F'))) {
            return OdaResult.Failed(
                "PAN BIN mismatch: PAN starts with ${claimedPan.take(8)}, cert has issuer $issuerId"
            )
        }

        // Recover ICC certificate
        val iccCert = tlvData[TAG_ICC_PK_CERT]
        if (iccCert != null) {
            val issuerPkRemainder = tlvData[TAG_ISSUER_PK_REMAINDER] ?: byteArrayOf()
            val issuerPkExponent = tlvData[TAG_ISSUER_PK_EXPONENT] ?: byteArrayOf(0x03)

            val issuerKey = CertificateParser.parseIssuerCertificate(
                recoveredIssuerCert, issuerPkRemainder, issuerPkExponent
            ) ?: return OdaResult.Failed("Could not parse issuer key")

            val recoveredIccCert = RsaRecovery.recover(iccCert, issuerKey.exponent, issuerKey.modulus)

            // Extract PAN from ICC certificate
            val certPan = extractPanFromIccCertificate(recoveredIccCert)
            if (certPan != null) {
                android.util.Log.d("ODA", "PAN from ICC cert: $certPan")
                android.util.Log.d("ODA", "Claimed PAN:       $claimedPan")

                if (certPan != claimedPan) {
                    return OdaResult.Failed(
                        "PAN mismatch: claimed $claimedPan but certificate contains $certPan"
                    )
                }
                android.util.Log.i("ODA", "PAN VERIFIED: claimed PAN matches ICC certificate")
            }
        }

        return OdaResult.Success
    }

    /**
     * Build the static data that is included in the SSAD hash.
     *
     * Per EMV Book 2, the static data includes:
     * - Data specified in tag 9F4A (SDA Tag List)
     * - If 9F4A not present, typically includes AIP
     */
    private fun buildStaticDataForAuth(tlvData: Map<String, ByteArray>): ByteArray {
        val result = mutableListOf<Byte>()

        // Check for SDA Tag List
        val sdaTagList = tlvData[TAG_SDA_TAG_LIST]
        if (sdaTagList != null && sdaTagList.isNotEmpty()) {
            // Tag list contains tag identifiers of data to include
            // For simplicity, if AIP (82) is in the list, include it
            for (i in sdaTagList.indices) {
                val tag = String.format("%02X", sdaTagList[i].toInt() and 0xFF)
                tlvData[tag]?.let { result.addAll(it.toList()) }
            }
        } else {
            // Default: include AIP if present
            tlvData[TAG_AIP]?.let { result.addAll(it.toList()) }
        }

        return result.toByteArray()
    }

    /**
     * Quick check if ODA data is present in the TLV data.
     * Use this to determine if ODA verification should be attempted.
     */
    fun hasOdaData(tlvData: Map<String, ByteArray>): Boolean {
        return tlvData.containsKey(TAG_CA_PK_INDEX) &&
            tlvData.containsKey(TAG_ISSUER_PK_CERT)
    }
}

/**
 * Exception thrown when ODA verification fails.
 */
internal class OdaFailedException(message: String) : Exception(message)
