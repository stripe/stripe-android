package com.stripe.android.common.taptoadd.nfcdirect.oda

import com.google.common.truth.Truth.assertThat
import org.junit.Test
import java.math.BigInteger

class RsaRecoveryTest {

    @Test
    fun `recover with small numbers`() {
        // Simple test: 2^3 mod 5 = 3
        val data = byteArrayOf(0x02)
        val exponent = byteArrayOf(0x03)
        val modulus = byteArrayOf(0x05)

        val result = RsaRecovery.recover(data, exponent, modulus)

        assertThat(result).isEqualTo(byteArrayOf(0x03))
    }

    @Test
    fun `recover with exponent 3`() {
        // Common EMV exponent is 3
        // Test: 4^3 mod 7 = 64 mod 7 = 1
        val data = byteArrayOf(0x04)
        val exponent = byteArrayOf(0x03)
        val modulus = byteArrayOf(0x07)

        val result = RsaRecovery.recover(data, exponent, modulus)

        assertThat(result).isEqualTo(byteArrayOf(0x01))
    }

    @Test
    fun `recover with exponent 65537`() {
        // Some CA keys use exponent 65537 (0x010001)
        // 2^65537 mod 13 = 2 (since 2^12 ≡ 1 mod 13, and 65537 mod 12 = 1)
        val data = byteArrayOf(0x02)
        val exponent = byteArrayOf(0x01, 0x00, 0x01)
        val modulus = byteArrayOf(0x0D)

        val result = RsaRecovery.recover(data, exponent, modulus)

        assertThat(result).isEqualTo(byteArrayOf(0x02))
    }

    @Test
    fun `recover maintains modulus length`() {
        // Result should be padded to match modulus length
        val data = byteArrayOf(0x02)
        val exponent = byteArrayOf(0x03)
        val modulus = byteArrayOf(0x00, 0x00, 0x05) // 3 bytes

        val result = RsaRecovery.recover(data, exponent, modulus)

        assertThat(result.size).isEqualTo(3)
        assertThat(result[2]).isEqualTo(0x03.toByte())
    }

    @Test
    fun `recover with multi-byte values`() {
        // Test with larger values typical of EMV
        // 256^3 mod 1000000007 = 16777216
        val data = byteArrayOf(0x01, 0x00) // 256
        val exponent = byteArrayOf(0x03)
        val modulus = byteArrayOf(0x3B, 0x9A.toByte(), 0xCA.toByte(), 0x07) // 1000000007

        val result = RsaRecovery.recover(data, exponent, modulus)

        // Convert result to BigInteger to verify
        val resultBigInt = BigInteger(1, result)
        assertThat(resultBigInt.toLong()).isEqualTo(16777216L)
    }

    @Test
    fun `recover with typical EMV sizes`() {
        // Create a test with realistic RSA sizes (though smaller than actual EMV keys)
        // Use 64-bit modulus for testing
        val modulus = ByteArray(8) { 0xFF.toByte() }
        modulus[0] = 0x7F.toByte() // Make it positive and less than max

        val data = ByteArray(8) { 0x00 }
        data[7] = 0x02 // Small value

        val exponent = byteArrayOf(0x03)

        val result = RsaRecovery.recover(data, exponent, modulus)

        // Just verify it doesn't throw and returns correct size
        assertThat(result.size).isEqualTo(8)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `recover throws on empty data`() {
        RsaRecovery.recover(byteArrayOf(), byteArrayOf(0x03), byteArrayOf(0x05))
    }

    @Test(expected = IllegalArgumentException::class)
    fun `recover throws on empty exponent`() {
        RsaRecovery.recover(byteArrayOf(0x02), byteArrayOf(), byteArrayOf(0x05))
    }

    @Test(expected = IllegalArgumentException::class)
    fun `recover throws on empty modulus`() {
        RsaRecovery.recover(byteArrayOf(0x02), byteArrayOf(0x03), byteArrayOf())
    }

    @Test
    fun `recover result fits in modulus size even when BigInteger adds leading zero`() {
        // BigInteger may add a leading zero byte for positive numbers
        // This test ensures we handle that correctly
        val data = byteArrayOf(0x7F) // Result will be positive
        val exponent = byteArrayOf(0x02) // Square
        val modulus = byteArrayOf(0x00, 0xFF.toByte()) // 2 bytes

        val result = RsaRecovery.recover(data, exponent, modulus)

        assertThat(result.size).isEqualTo(2)
    }

    @Test
    fun `verify RSA identity with known test vector`() {
        // Using a simple test: encrypt then decrypt should give original
        // For RSA: (m^e)^d mod n = m where e*d ≡ 1 mod φ(n)
        // Using small primes: p=11, q=13, n=143, φ(n)=120, e=7, d=103
        val n = byteArrayOf(0x00, 0x8F.toByte()) // 143
        val e = byteArrayOf(0x07)

        val m = byteArrayOf(0x00, 0x42) // 66, must be < n

        // m^e mod n
        val encrypted = RsaRecovery.recover(m, e, n)

        // The encrypted value should be different from original
        val encryptedBigInt = BigInteger(1, encrypted)
        assertThat(encryptedBigInt.toLong()).isNotEqualTo(66L)

        // Using d=103 to decrypt: (m^e)^d mod n = m
        val d = byteArrayOf(0x67) // 103
        val decrypted = RsaRecovery.recover(encrypted, d, n)
        val decryptedBigInt = BigInteger(1, decrypted)
        assertThat(decryptedBigInt.toLong()).isEqualTo(66L)
    }
}
