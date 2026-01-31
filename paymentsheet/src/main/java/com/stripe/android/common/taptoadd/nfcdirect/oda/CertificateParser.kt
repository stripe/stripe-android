package com.stripe.android.common.taptoadd.nfcdirect.oda

import java.security.MessageDigest

/**
 * Parser for EMV certificate structures.
 *
 * EMV uses a specific certificate format for Issuer and ICC public keys.
 * After RSA recovery, the certificate data has a defined structure that
 * must be parsed to extract the public key components.
 *
 * Issuer Public Key Certificate Format (after recovery):
 * - Byte 1: Header (0x6A)
 * - Byte 2: Certificate Format (0x02)
 * - Bytes 3-6: Issuer Identifier
 * - Bytes 7-8: Certificate Expiration Date (MMYY)
 * - Bytes 9-11: Certificate Serial Number
 * - Byte 12: Hash Algorithm Indicator (0x01 = SHA-1)
 * - Byte 13: Issuer Public Key Algorithm Indicator
 * - Byte 14: Issuer Public Key Length
 * - Byte 15: Issuer Public Key Exponent Length
 * - Bytes 16-(N-21): Issuer Public Key (leftmost bytes)
 * - Bytes (N-20)-(N-1): Hash Result (20 bytes for SHA-1)
 * - Byte N: Trailer (0xBC)
 */
internal object CertificateParser {

    /**
     * Parsed issuer public key data.
     */
    data class IssuerPublicKey(
        val modulus: ByteArray,
        val exponent: ByteArray
    ) {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other !is IssuerPublicKey) return false
            return modulus.contentEquals(other.modulus) && exponent.contentEquals(other.exponent)
        }

        override fun hashCode(): Int {
            var result = modulus.contentHashCode()
            result = 31 * result + exponent.contentHashCode()
            return result
        }
    }

    // Certificate format constants
    private const val HEADER = 0x6A.toByte()
    private const val TRAILER = 0xBC.toByte()
    private const val ISSUER_CERT_FORMAT = 0x02.toByte()
    private const val HASH_ALGORITHM_SHA1 = 0x01.toByte()
    private const val SHA1_HASH_LENGTH = 20

    /**
     * Parse recovered issuer certificate to extract public key.
     *
     * @param recoveredCert The RSA-recovered certificate data
     * @param issuerPkRemainder Additional modulus bytes from tag 92 (if any)
     * @param issuerPkExponent Exponent from tag 9F32
     * @return The parsed issuer public key, or null if parsing fails
     */
    fun parseIssuerCertificate(
        recoveredCert: ByteArray,
        issuerPkRemainder: ByteArray?,
        issuerPkExponent: ByteArray
    ): IssuerPublicKey? {
        if (recoveredCert.size < 36) {
            return null // Certificate too short
        }

        // Verify header and trailer
        if (recoveredCert.first() != HEADER || recoveredCert.last() != TRAILER) {
            return null
        }

        // Verify certificate format
        if (recoveredCert[1] != ISSUER_CERT_FORMAT) {
            return null
        }

        // Extract key length info
        val issuerPkLength = recoveredCert[13].toInt() and 0xFF
        val issuerPkExponentLength = recoveredCert[14].toInt() and 0xFF

        // Calculate how many bytes of the key are in the certificate
        // Certificate structure: 15 header bytes + key + 20 hash + 1 trailer
        val keyBytesInCert = recoveredCert.size - 15 - SHA1_HASH_LENGTH - 1

        if (keyBytesInCert <= 0) {
            return null
        }

        // Extract key bytes from certificate
        val keyBytesFromCert = recoveredCert.copyOfRange(15, 15 + keyBytesInCert)

        // Combine with remainder if present
        val fullModulus = if (issuerPkRemainder != null && issuerPkRemainder.isNotEmpty()) {
            keyBytesFromCert + issuerPkRemainder
        } else {
            keyBytesFromCert
        }

        // Use provided exponent or default to 0x03
        val exponent = if (issuerPkExponent.isNotEmpty()) {
            issuerPkExponent
        } else {
            byteArrayOf(0x03)
        }

        return IssuerPublicKey(
            modulus = fullModulus.take(issuerPkLength).toByteArray(),
            exponent = exponent
        )
    }

    /**
     * Verify the hash in a recovered certificate.
     *
     * The hash covers specific bytes from the certificate and additional data
     * that must be provided separately.
     *
     * @param recoveredCert The RSA-recovered certificate
     * @param additionalData Additional data included in hash (e.g., remainder, exponent)
     * @return True if hash verification passes
     */
    fun verifyCertificateHash(
        recoveredCert: ByteArray,
        additionalData: ByteArray
    ): Boolean {
        if (recoveredCert.size < SHA1_HASH_LENGTH + 2) {
            return false
        }

        // Extract stored hash (last 20 bytes before trailer)
        val hashStart = recoveredCert.size - SHA1_HASH_LENGTH - 1
        val storedHash = recoveredCert.copyOfRange(hashStart, hashStart + SHA1_HASH_LENGTH)

        // Calculate hash over certificate data (excluding header, hash, and trailer)
        // Hash = SHA-1(Format || Issuer ID || Cert Exp || Cert Serial || Hash Algo ||
        //              PK Algo || PK Length || PK Exp Length || PK || Remainder || Exponent)
        val dataToHash = recoveredCert.copyOfRange(1, hashStart) + additionalData

        val calculatedHash = sha1(dataToHash)

        return storedHash.contentEquals(calculatedHash)
    }

    /**
     * Verify Signed Static Application Data (SSAD).
     *
     * After RSA recovery, SSAD has format:
     * - Byte 1: Header (0x6A)
     * - Byte 2: Data Format (0x03 for SDA)
     * - Byte 3: Hash Algorithm (0x01 = SHA-1)
     * - Bytes 4-(N-21): Padding (0xBB) + Static Data
     * - Bytes (N-20)-(N-1): Hash
     * - Byte N: Trailer (0xBC)
     *
     * @param recoveredSsad The RSA-recovered SSAD
     * @param staticDataForHash The static application data to verify
     * @return True if SSAD verification passes
     */
    fun verifySsad(
        recoveredSsad: ByteArray,
        staticDataForHash: ByteArray
    ): Boolean {
        if (recoveredSsad.size < SHA1_HASH_LENGTH + 4) {
            return false
        }

        // Verify header and trailer
        if (recoveredSsad.first() != HEADER || recoveredSsad.last() != TRAILER) {
            return false
        }

        // Verify data format (0x03 for signed static data)
        if (recoveredSsad[1] != 0x03.toByte()) {
            return false
        }

        // Extract stored hash
        val hashStart = recoveredSsad.size - SHA1_HASH_LENGTH - 1
        val storedHash = recoveredSsad.copyOfRange(hashStart, hashStart + SHA1_HASH_LENGTH)

        // Hash covers: Format || Hash Algo || Pad || Static Data to Auth
        val dataToHash = recoveredSsad.copyOfRange(1, hashStart) + staticDataForHash

        val calculatedHash = sha1(dataToHash)

        return storedHash.contentEquals(calculatedHash)
    }

    private fun sha1(data: ByteArray): ByteArray {
        return MessageDigest.getInstance("SHA-1").digest(data)
    }
}
