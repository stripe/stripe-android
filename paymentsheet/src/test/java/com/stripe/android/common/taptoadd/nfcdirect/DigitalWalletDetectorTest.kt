package com.stripe.android.common.taptoadd.nfcdirect

import com.google.common.truth.Truth.assertThat
import com.stripe.android.common.taptoadd.nfcdirect.TlvParser.hexToByteArray
import org.junit.Test

class DigitalWalletDetectorTest {

    private val detector = DigitalWalletDetector()

    @Test
    fun `detects Mastercard digital wallet from CVM Results`() {
        // Mastercard AID
        val aid = "A0000000041010".hexToByteArray()

        // CVM Results (9F34) with mobile/CDCVM indicator (byte 1 = 0x1F)
        val tlvData = mapOf(
            "9F34" to byteArrayOf(0x1F, 0x00, 0x00)
        )

        assertThat(detector.isDigitalWallet(tlvData, aid)).isTrue()
    }

    @Test
    fun `does not detect physical Mastercard`() {
        val aid = "A0000000041010".hexToByteArray()

        // CVM Results without mobile indicator
        val tlvData = mapOf(
            "9F34" to byteArrayOf(0x02, 0x00, 0x00) // PIN verified
        )

        assertThat(detector.isDigitalWallet(tlvData, aid)).isFalse()
    }

    @Test
    fun `detects Visa digital wallet from CTQ`() {
        // Visa AID
        val aid = "A0000000031010".hexToByteArray()

        // Card Transaction Qualifiers (C1) with mobile indicators
        val tlvData = mapOf(
            "C1" to byteArrayOf(0xC0.toByte(), 0x00) // Both bits 7 and 8 set
        )

        assertThat(detector.isDigitalWallet(tlvData, aid)).isTrue()
    }

    @Test
    fun `does not detect physical Visa card`() {
        val aid = "A0000000031010".hexToByteArray()

        // CTQ without mobile indicators or no CTQ at all
        val tlvData = mapOf(
            "C1" to byteArrayOf(0x00, 0x00)
        )

        assertThat(detector.isDigitalWallet(tlvData, aid)).isFalse()
    }

    @Test
    fun `does not detect Visa without CTQ tag`() {
        val aid = "A0000000031010".hexToByteArray()

        // No CTQ tag present (physical card)
        val tlvData = emptyMap<String, ByteArray>()

        assertThat(detector.isDigitalWallet(tlvData, aid)).isFalse()
    }

    @Test
    fun `detects Amex digital wallet from enhanced capabilities`() {
        // Amex AID
        val aid = "A00000002501".hexToByteArray()

        // Enhanced Contactless Reader Capabilities (9F71) with mobile indicator
        val tlvData = mapOf(
            "9F71" to byteArrayOf(0x01, 0x00, 0x00)
        )

        assertThat(detector.isDigitalWallet(tlvData, aid)).isTrue()
    }

    @Test
    fun `does not detect physical Amex card`() {
        val aid = "A00000002501".hexToByteArray()

        // No mobile indicator
        val tlvData = mapOf(
            "9F71" to byteArrayOf(0x00, 0x00, 0x00)
        )

        assertThat(detector.isDigitalWallet(tlvData, aid)).isFalse()
    }

    @Test
    fun `detects Discover digital wallet from IAD`() {
        // Discover AID
        val aid = "A0000001523010".hexToByteArray()

        // Issuer Application Data (9F10) with DPAN indicator
        val tlvData = mapOf(
            "9F10" to byteArrayOf(0x01, 0x00, 0x00, 0x00)
        )

        assertThat(detector.isDigitalWallet(tlvData, aid)).isTrue()
    }

    @Test
    fun `does not detect physical Discover card`() {
        val aid = "A0000001523010".hexToByteArray()

        // IAD without DPAN indicator
        val tlvData = mapOf(
            "9F10" to byteArrayOf(0x00, 0x00, 0x00, 0x00)
        )

        assertThat(detector.isDigitalWallet(tlvData, aid)).isFalse()
    }

    @Test
    fun `returns false for unknown scheme`() {
        val aid = "A000000099".hexToByteArray() // Unknown AID

        val tlvData = mapOf(
            "9F34" to byteArrayOf(0x1F, 0x00, 0x00)
        )

        // Should return false (conservative, allow unknown schemes)
        assertThat(detector.isDigitalWallet(tlvData, aid)).isFalse()
    }

    @Test
    fun `handles empty TLV data`() {
        val aid = "A0000000041010".hexToByteArray()

        assertThat(detector.isDigitalWallet(emptyMap(), aid)).isFalse()
    }

    @Test
    fun `Mastercard CVM byte with bit 4 set indicates mobile`() {
        val aid = "A0000000041010".hexToByteArray()

        // Bit 4 (0x08) set indicates mobile
        val tlvData = mapOf(
            "9F34" to byteArrayOf(0x08, 0x00, 0x00)
        )

        assertThat(detector.isDigitalWallet(tlvData, aid)).isTrue()
    }

    @Test
    fun `Mastercard CVM 0x1E indicates mobile`() {
        val aid = "A0000000041010".hexToByteArray()

        val tlvData = mapOf(
            "9F34" to byteArrayOf(0x1E, 0x00, 0x00)
        )

        assertThat(detector.isDigitalWallet(tlvData, aid)).isTrue()
    }
}
