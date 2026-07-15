package com.stripe.android.common.nfcscan.scanner

import com.google.common.truth.Truth.assertThat
import org.junit.Test

internal class DefaultNfcCardDataParserTest {
    private val parser = DefaultNfcCardDataParser()

    @Test
    fun `parse returns card data from Track 2 equivalent data when available, preferring it over PAN & expiry tags`() {
        val result = parser.parse(
            mapOf(
                TAG_TRACK2 to hexToBytes("4111111111111111D2512101"),
                TAG_PAN to hexToBytes("5555555555554444F"),
                TAG_EXPIRY to byteArrayOf(0x30, 0x06, 0x01),
            ),
        )

        assertThat(result).isEqualTo(
            ScannedCardData(
                cardNumber = "4111111111111111",
                expirationMonth = 12,
                expirationYear = 2025,
            ),
        )
    }

    @Test
    fun `parse returns null when Track 2 is missing field separator`() {
        val result = parser.parse(
            mapOf(
                TAG_TRACK2 to hexToBytes("4111111111111111"),
            ),
        )

        assertThat(result).isNull()
    }

    @Test
    fun `parse returns null when Track 2 expiry is truncated`() {
        val result = parser.parse(
            mapOf(
                TAG_TRACK2 to hexToBytes("4111111111111111D25"),
            ),
        )

        assertThat(result).isNull()
    }

    @Test
    fun `parse returns null when Track 2 expiry is not numeric`() {
        val result = parser.parse(
            mapOf(
                TAG_TRACK2 to hexToBytes("4111111111111111DAA12100"),
            ),
        )

        assertThat(result).isNull()
    }

    @Test
    fun `parse returns card data from separate PAN and expiry tags`() {
        val result = parser.parse(
            mapOf(
                TAG_PAN to hexToBytes("4111111111111111"),
                TAG_EXPIRY to byteArrayOf(0x25, 0x12, 0x01),
            ),
        )

        assertThat(result).isEqualTo(
            ScannedCardData(
                cardNumber = "4111111111111111",
                expirationMonth = 12,
                expirationYear = 2025,
            ),
        )
    }

    @Test
    fun `parse trims trailing F padding from PAN`() {
        val result = parser.parse(
            mapOf(
                TAG_PAN to hexToBytes("411111111111111F"),
                TAG_EXPIRY to byteArrayOf(0x25, 0x12, 0x01),
            ),
        )

        assertThat(result).isEqualTo(
            ScannedCardData(
                cardNumber = "411111111111111",
                expirationMonth = 12,
                expirationYear = 2025,
            ),
        )
    }

    @Test
    fun `parse returns null when PAN tag is missing`() {
        val result = parser.parse(
            mapOf(
                TAG_EXPIRY to byteArrayOf(0x25, 0x12, 0x01),
            ),
        )

        assertThat(result).isNull()
    }

    @Test
    fun `parse returns null when expiry tag is missing`() {
        val result = parser.parse(
            mapOf(
                TAG_PAN to hexToBytes("4111111111111111"),
            ),
        )

        assertThat(result).isNull()
    }

    @Test
    fun `parse returns null when expiry data is too short`() {
        val result = parser.parse(
            mapOf(
                TAG_PAN to hexToBytes("4111111111111111"),
                TAG_EXPIRY to byteArrayOf(0x25, 0x12),
            ),
        )

        assertThat(result).isNull()
    }

    @Test
    fun `parse returns null when no recognized tags are present`() {
        val result = parser.parse(emptyMap())

        assertThat(result).isNull()
    }

    @Test
    fun `canParse returns true when Track 2 tag is present`() {
        assertThat(
            parser.canParse(
                mapOf(
                    TAG_TRACK2 to hexToBytes("4111111111111111D2512101"),
                ),
            ),
        ).isTrue()
    }

    @Test
    fun `canParse returns true when both PAN and expiry tags are present`() {
        assertThat(
            parser.canParse(
                mapOf(
                    TAG_PAN to hexToBytes("4111111111111111"),
                    TAG_EXPIRY to byteArrayOf(0x25, 0x12, 0x01),
                ),
            ),
        ).isTrue()
    }

    @Test
    fun `canParse returns false when only PAN tag is present`() {
        assertThat(
            parser.canParse(
                mapOf(
                    TAG_PAN to hexToBytes("4111111111111111"),
                ),
            ),
        ).isFalse()
    }

    @Test
    fun `canParse returns false when only expiry tag is present`() {
        assertThat(
            parser.canParse(
                mapOf(
                    TAG_EXPIRY to byteArrayOf(0x25, 0x12, 0x01),
                ),
            ),
        ).isFalse()
    }

    @Test
    fun `canParse returns false when no recognized tags are present`() {
        assertThat(parser.canParse(emptyMap())).isFalse()
    }

    private fun hexToBytes(hex: String): ByteArray {
        return hex.chunked(2)
            .map { it.toInt(16).toByte() }
            .toByteArray()
    }

    private companion object {
        const val TAG_TRACK2 = "57"
        const val TAG_PAN = "5A"
        const val TAG_EXPIRY = "5F24"
    }
}
