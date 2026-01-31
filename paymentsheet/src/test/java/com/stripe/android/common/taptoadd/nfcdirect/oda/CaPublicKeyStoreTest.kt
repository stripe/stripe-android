package com.stripe.android.common.taptoadd.nfcdirect.oda

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class CaPublicKeyStoreTest {

    @Test
    fun `getKey returns Mastercard CA key for index 06`() {
        val key = CaPublicKeyStore.getKey("A000000004", "06")

        assertThat(key).isNotNull()
        assertThat(key!!.rid).isEqualTo("A000000004")
        assertThat(key.index).isEqualTo("06")
        assertThat(key.exponent).isEqualTo(byteArrayOf(0x03))
        assertThat(key.modulus.size).isGreaterThan(100) // Should be ~248 bytes (1984 bits)
    }

    @Test
    fun `getKey returns Mastercard CA key for index 05`() {
        val key = CaPublicKeyStore.getKey("A000000004", "05")

        assertThat(key).isNotNull()
        assertThat(key!!.rid).isEqualTo("A000000004")
        assertThat(key.index).isEqualTo("05")
    }

    @Test
    fun `getKey returns Visa CA key for index 09`() {
        val key = CaPublicKeyStore.getKey("A000000003", "09")

        assertThat(key).isNotNull()
        assertThat(key!!.rid).isEqualTo("A000000003")
        assertThat(key.index).isEqualTo("09")
    }

    @Test
    fun `getKey returns Amex CA key for index 0A`() {
        val key = CaPublicKeyStore.getKey("A000000025", "0A")

        assertThat(key).isNotNull()
        assertThat(key!!.rid).isEqualTo("A000000025")
        assertThat(key.index).isEqualTo("0A")
    }

    @Test
    fun `getKey returns Discover CA key for index 03`() {
        val key = CaPublicKeyStore.getKey("A000000152", "03")

        assertThat(key).isNotNull()
        assertThat(key!!.rid).isEqualTo("A000000152")
        assertThat(key.index).isEqualTo("03")
    }

    @Test
    fun `getKey returns null for unknown RID`() {
        val key = CaPublicKeyStore.getKey("A000009999", "01")

        assertThat(key).isNull()
    }

    @Test
    fun `getKey returns null for unknown index`() {
        val key = CaPublicKeyStore.getKey("A000000004", "FF")

        assertThat(key).isNull()
    }

    @Test
    fun `getKey is case insensitive for RID`() {
        val keyUpper = CaPublicKeyStore.getKey("A000000004", "06")
        val keyLower = CaPublicKeyStore.getKey("a000000004", "06")

        assertThat(keyUpper).isNotNull()
        assertThat(keyLower).isNotNull()
        assertThat(keyUpper).isEqualTo(keyLower)
    }

    @Test
    fun `getKey is case insensitive for index`() {
        val keyUpper = CaPublicKeyStore.getKey("A000000025", "0A")
        val keyLower = CaPublicKeyStore.getKey("A000000025", "0a")

        assertThat(keyUpper).isNotNull()
        assertThat(keyLower).isNotNull()
        assertThat(keyUpper).isEqualTo(keyLower)
    }

    @Test
    fun `getKey pads short index with leading zero`() {
        // Index "6" should be treated as "06"
        val keyWithoutPad = CaPublicKeyStore.getKey("A000000004", "6")
        val keyWithPad = CaPublicKeyStore.getKey("A000000004", "06")

        assertThat(keyWithoutPad).isEqualTo(keyWithPad)
    }

    @Test
    fun `getKey truncates long RID to 10 characters`() {
        // Extra characters at end should be ignored
        val keyNormal = CaPublicKeyStore.getKey("A000000004", "06")
        val keyLong = CaPublicKeyStore.getKey("A0000000041010", "06") // Full AID

        assertThat(keyNormal).isNotNull()
        assertThat(keyLong).isEqualTo(keyNormal)
    }

    @Test
    fun `CaPublicKey equals compares by RID and index`() {
        val key1 = CaPublicKeyStore.CaPublicKey(
            rid = "A000000004",
            index = "06",
            modulus = byteArrayOf(0x01, 0x02),
            exponent = byteArrayOf(0x03)
        )
        val key2 = CaPublicKeyStore.CaPublicKey(
            rid = "A000000004",
            index = "06",
            modulus = byteArrayOf(0x04, 0x05), // Different modulus
            exponent = byteArrayOf(0x03)
        )

        // Should be equal based on RID and index only
        assertThat(key1).isEqualTo(key2)
    }

    @Test
    fun `CaPublicKey hashCode is consistent`() {
        val key1 = CaPublicKeyStore.CaPublicKey(
            rid = "A000000004",
            index = "06",
            modulus = byteArrayOf(0x01),
            exponent = byteArrayOf(0x03)
        )
        val key2 = CaPublicKeyStore.CaPublicKey(
            rid = "A000000004",
            index = "06",
            modulus = byteArrayOf(0x02), // Different modulus
            exponent = byteArrayOf(0x03)
        )

        assertThat(key1.hashCode()).isEqualTo(key2.hashCode())
    }

    @Test
    fun `all stored keys have valid modulus`() {
        // Test that all keys have non-empty modulus
        val testCases = listOf(
            "A000000004" to "06", // Mastercard
            "A000000004" to "05",
            "A000000003" to "09", // Visa
            "A000000003" to "08",
            "A000000025" to "0A", // Amex
            "A000000152" to "03", // Discover
            "A000000152" to "04",
            "A000000152" to "05",
        )

        for ((rid, index) in testCases) {
            val key = CaPublicKeyStore.getKey(rid, index)
            assertThat(key).isNotNull()
            assertThat(key!!.modulus).isNotEmpty()
            assertThat(key.exponent).isNotEmpty()
        }
    }
}
