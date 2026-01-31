package com.stripe.android.common.taptoadd.nfcdirect.oda

import java.math.BigInteger

/**
 * RSA recovery operations for EMV Offline Data Authentication.
 *
 * EMV uses RSA for certificate verification. The "recovery" operation is:
 * recovered_data = encrypted_data ^ exponent mod modulus
 *
 * This is used to:
 * 1. Recover Issuer Public Key from Issuer PK Certificate using CA Public Key
 * 2. Recover ICC Public Key from ICC PK Certificate using Issuer Public Key (for DDA)
 * 3. Verify Signed Static Application Data using Issuer Public Key (for SDA)
 */
internal object RsaRecovery {

    /**
     * Perform RSA recovery: data^exponent mod modulus
     *
     * @param data The encrypted/signed data to recover
     * @param exponent The public exponent (usually 3 or 65537)
     * @param modulus The public modulus
     * @return The recovered data, sized to match the modulus length
     */
    fun recover(
        data: ByteArray,
        exponent: ByteArray,
        modulus: ByteArray
    ): ByteArray {
        require(data.isNotEmpty()) { "Data cannot be empty" }
        require(exponent.isNotEmpty()) { "Exponent cannot be empty" }
        require(modulus.isNotEmpty()) { "Modulus cannot be empty" }

        val dataBigInt = BigInteger(1, data)
        val expBigInt = BigInteger(1, exponent)
        val modBigInt = BigInteger(1, modulus)

        val result = dataBigInt.modPow(expBigInt, modBigInt)

        return toByteArrayWithLength(result, modulus.size)
    }

    /**
     * Convert BigInteger to byte array with specific length.
     * Pads with leading zeros or trims leading zero byte if necessary.
     */
    private fun toByteArrayWithLength(value: BigInteger, length: Int): ByteArray {
        val bytes = value.toByteArray()

        return when {
            // BigInteger adds a leading zero for positive numbers to avoid sign confusion
            bytes.size == length + 1 && bytes[0] == 0.toByte() -> {
                bytes.copyOfRange(1, bytes.size)
            }
            // Result is shorter, pad with leading zeros
            bytes.size < length -> {
                ByteArray(length - bytes.size) + bytes
            }
            // Result matches expected length
            bytes.size == length -> bytes
            // Result is longer (shouldn't happen with valid inputs)
            else -> bytes.copyOfRange(bytes.size - length, bytes.size)
        }
    }
}
