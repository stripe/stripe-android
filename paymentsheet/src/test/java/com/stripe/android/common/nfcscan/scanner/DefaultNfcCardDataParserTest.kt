package com.stripe.android.common.nfcscan.scanner

import com.google.common.truth.Truth.assertThat
import com.stripe.android.core.strings.resolvableString
import com.stripe.android.paymentsheet.R
import org.junit.Assert.assertThrows
import org.junit.Test

internal class DefaultNfcCardDataParserTest {
    private val parser = DefaultNfcCardDataParser()

    @Test
    fun `parse throws card data from Track 2 equivalent data when available, preferring it over PAN & expiry tags`() {
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
    fun `parse throws unsupported card error when Track 2 is missing field separator`() {
        val error = assertThrows(GenericNfcScanningError::class.java) {
            parser.parse(
                mapOf(
                    TAG_TRACK2 to hexToBytes("4111111111111111"),
                ),
            )
        }

        assertUnsupportedCardError(error)
    }

    @Test
    fun `parse throws unsupported card error when Track 2 expiry is truncated`() {
        val error = assertThrows(GenericNfcScanningError::class.java) {
            parser.parse(
                mapOf(
                    TAG_TRACK2 to hexToBytes("4111111111111111D25"),
                ),
            )
        }

        assertUnsupportedCardError(error)
    }

    @Test
    fun `parse throws unsupported card error when Track 2 expiry is not numeric`() {
        val error = assertThrows(GenericNfcScanningError::class.java) {
            parser.parse(
                mapOf(
                    TAG_TRACK2 to hexToBytes("4111111111111111DAA12100"),
                ),
            )
        }

        assertUnsupportedCardError(error)
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
    fun `parse throws unsupported card error when PAN tag is missing`() {
        val error = assertThrows(GenericNfcScanningError::class.java) {
            parser.parse(
                mapOf(
                    TAG_EXPIRY to byteArrayOf(0x25, 0x12, 0x01),
                ),
            )
        }

        assertUnsupportedCardError(error)
    }

    @Test
    fun `parse throws unsupported card error when expiry tag is missing`() {
        val error = assertThrows(GenericNfcScanningError::class.java) {
            parser.parse(
                mapOf(
                    TAG_PAN to hexToBytes("4111111111111111"),
                ),
            )
        }

        assertUnsupportedCardError(error)
    }

    @Test
    fun `parse throws unsupported card error when expiry data is too short`() {
        val error = assertThrows(GenericNfcScanningError::class.java) {
            parser.parse(
                mapOf(
                    TAG_PAN to hexToBytes("4111111111111111"),
                    TAG_EXPIRY to byteArrayOf(0x25, 0x12),
                ),
            )
        }

        assertUnsupportedCardError(error)
    }

    @Test
    fun `parse returns unsupported card error when no recognized tags are present`() {
        val error = assertThrows(GenericNfcScanningError::class.java) {
            parser.parse(emptyMap())
        }

        assertUnsupportedCardError(error)
    }

    @Test
    fun `parse returns mobile wallet error when AIP byte 2 bit 7 indicates a contactless mobile device`() {
        val error = assertThrows(GenericNfcScanningError::class.java) {
            parser.parse(
                mapOf(
                    TAG_TRACK2 to hexToBytes("4111111111111111D2512101"),
                    TAG_AIP to byteArrayOf(0x00, AIP_MOBILE_WALLET_BYTE_2),
                ),
            )
        }

        assertMobileWalletError(error)
    }

    @Test
    fun `parse returns mobile wallet error when AIP indicates a mobile device without card data tags`() {
        val error = assertThrows(GenericNfcScanningError::class.java) {
            parser.parse(
                mapOf(
                    TAG_AIP to byteArrayOf(0x00, AIP_MOBILE_WALLET_BYTE_2),
                ),
            )
        }

        assertMobileWalletError(error)
    }

    @Test
    fun `parse returns card data when AIP does not indicate a contactless mobile device`() {
        val result = parser.parse(
            mapOf(
                TAG_TRACK2 to hexToBytes("4111111111111111D2512101"),
                TAG_AIP to byteArrayOf(0x00, 0x00),
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
    fun `parse returns unsupported card error for tokenized credential without mobile wallet AIP`() {
        val error = assertThrows(GenericNfcScanningError::class.java) {
            parser.parse(
                mapOf(
                    TAG_TRACK2 to hexToBytes("4111111111111111D2512101"),
                    TAG_AIP to byteArrayOf(0x00, 0x00),
                    TAG_TOKEN_REQUESTOR_ID to hexToBytes("12345678901F"),
                ),
            )
        }

        assertUnsupportedCardError(error)
    }

    private fun assertUnsupportedCardError(error: GenericNfcScanningError) {
        assertThat(error.errorCode).isEqualTo("cardUnsupportedByNfc")
        assertThat(error.userMessage).isEqualTo(
            R.string.stripe_nfc_scan_unsupported_card.resolvableString,
        )
    }

    private fun assertMobileWalletError(error: GenericNfcScanningError) {
        assertThat(error.errorCode).isEqualTo("mobileWalletUnsupportedByNfc")
        assertThat(error.userMessage).isEqualTo(
            R.string.stripe_nfc_scan_error_mobile_wallet.resolvableString,
        )
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
        const val TAG_AIP = "82"
        const val TAG_TOKEN_REQUESTOR_ID = "9F19"

        const val AIP_MOBILE_WALLET_BYTE_2 = 0x40.toByte()
    }
}
