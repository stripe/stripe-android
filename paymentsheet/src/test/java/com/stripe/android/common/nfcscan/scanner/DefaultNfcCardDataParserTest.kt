package com.stripe.android.common.nfcscan.scanner

import com.google.common.truth.Truth.assertThat
import com.stripe.android.core.strings.resolvableString
import com.stripe.android.paymentsheet.R
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
            NfcCardDataParser.Result.Success(
                ScannedCardData(
                    cardNumber = "4111111111111111",
                    expirationMonth = 12,
                    expirationYear = 2025,
                ),
            ),
        )
    }

    @Test
    fun `parse returns unsupported card when Track 2 is missing field separator`() {
        val result = parser.parse(
            mapOf(
                TAG_TRACK2 to hexToBytes("4111111111111111"),
            ),
        )

        assertThat(result).isEqualTo(unsupportedCardResult())
    }

    @Test
    fun `parse returns unsupported card when Track 2 expiry is truncated`() {
        val result = parser.parse(
            mapOf(
                TAG_TRACK2 to hexToBytes("4111111111111111D25"),
            ),
        )

        assertThat(result).isEqualTo(unsupportedCardResult())
    }

    @Test
    fun `parse returns unsupported card when Track 2 expiry is not numeric`() {
        val result = parser.parse(
            mapOf(
                TAG_TRACK2 to hexToBytes("4111111111111111DAA12100"),
            ),
        )

        assertThat(result).isEqualTo(unsupportedCardResult())
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
            NfcCardDataParser.Result.Success(
                ScannedCardData(
                    cardNumber = "4111111111111111",
                    expirationMonth = 12,
                    expirationYear = 2025,
                ),
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
            NfcCardDataParser.Result.Success(
                ScannedCardData(
                    cardNumber = "411111111111111",
                    expirationMonth = 12,
                    expirationYear = 2025,
                ),
            ),
        )
    }

    @Test
    fun `parse returns unsupported card when PAN tag is missing`() {
        val result = parser.parse(
            mapOf(
                TAG_EXPIRY to byteArrayOf(0x25, 0x12, 0x01),
            ),
        )

        assertThat(result).isEqualTo(unsupportedCardResult())
    }

    @Test
    fun `parse returns unsupported card when expiry tag is missing`() {
        val result = parser.parse(
            mapOf(
                TAG_PAN to hexToBytes("4111111111111111"),
            ),
        )

        assertThat(result).isEqualTo(unsupportedCardResult())
    }

    @Test
    fun `parse returns unsupported card when expiry data is too short`() {
        val result = parser.parse(
            mapOf(
                TAG_PAN to hexToBytes("4111111111111111"),
                TAG_EXPIRY to byteArrayOf(0x25, 0x12),
            ),
        )

        assertThat(result).isEqualTo(unsupportedCardResult())
    }

    @Test
    fun `parse returns unsupported card when no recognized tags are present`() {
        val result = parser.parse(emptyMap())

        assertThat(result).isEqualTo(unsupportedCardResult())
    }

    @Test
    fun `parse rejects tokenized card when Token Requestor ID is present`() {
        val result = parser.parse(
            mapOf(
                TAG_TRACK2 to hexToBytes("4111111111111111D2512101"),
                // Unknown Token Requestor ID that does not match a known wallet.
                TAG_TOKEN_REQUESTOR_ID to hexToBytes("1234567890"),
            ),
        )

        assertThat(result).isEqualTo(unsupportedCardResult())
    }

    @Test
    fun `parse rejects tokenized card when Payment Account Reference is present`() {
        val result = parser.parse(
            mapOf(
                TAG_TRACK2 to hexToBytes("4111111111111111D2512101"),
                TAG_PAYMENT_ACCOUNT_REFERENCE to hexToBytes("00112233445566778899AABB"),
            ),
        )

        assertThat(result).isEqualTo(unsupportedCardResult())
    }

    @Test
    fun `parse rejects mobile wallet when Form Factor Indicator reports a mobile device`() {
        val result = parser.parse(
            mapOf(
                TAG_TRACK2 to hexToBytes("4111111111111111D2512101"),
                // FFI byte 1 low nibble 0x1 => Mobile Network Operator secure element (mobile phone).
                TAG_FORM_FACTOR_INDICATOR to hexToBytes("0100000000"),
            ),
        )

        assertThat(result).isEqualTo(mobileWalletResult())
    }

    @Test
    fun `parse rejects mobile wallet when Form Factor Indicator reports a network connected device`() {
        val result = parser.parse(
            mapOf(
                TAG_TRACK2 to hexToBytes("4111111111111111D2512101"),
                // FFI byte 1 is a card (0x00) but byte 2 bit 8 signals a network-connected device.
                TAG_FORM_FACTOR_INDICATOR to hexToBytes("0080000000"),
            ),
        )

        assertThat(result).isEqualTo(mobileWalletResult())
    }

    @Test
    fun `parse rejects mobile wallet when Token Requestor ID matches a known wallet`() {
        val result = parser.parse(
            mapOf(
                TAG_TRACK2 to hexToBytes("4111111111111111D2512101"),
                // Apple Pay (Mastercard) Token Requestor ID 50110030273 (trailing nibble padding).
                TAG_TOKEN_REQUESTOR_ID to hexToBytes("501100302730"),
            ),
        )

        assertThat(result).isEqualTo(mobileWalletResult())
    }

    @Test
    fun `parse rejects mobile wallet when Application Label contains a wallet keyword`() {
        val result = parser.parse(
            mapOf(
                TAG_TRACK2 to hexToBytes("4111111111111111D2512101"),
                TAG_APPLICATION_LABEL to "Apple Pay".toByteArray(Charsets.US_ASCII),
            ),
        )

        assertThat(result).isEqualTo(mobileWalletResult())
    }

    @Test
    fun `parse prefers mobile wallet over generic tokenized when both signals are present`() {
        val result = parser.parse(
            mapOf(
                TAG_TRACK2 to hexToBytes("4111111111111111D2512101"),
                // Known wallet Token Requestor ID (mobile) plus a Payment Account Reference (tokenized).
                TAG_TOKEN_REQUESTOR_ID to hexToBytes("501100302730"),
                TAG_PAYMENT_ACCOUNT_REFERENCE to hexToBytes("00112233445566778899AABB"),
            ),
        )

        assertThat(result).isEqualTo(mobileWalletResult())
    }

    private fun unsupportedCardResult(): NfcCardDataParser.Result.Error {
        return NfcCardDataParser.Result.Error(
            analyticsValue = "cardUnsupportedByNfc",
            userMessage = R.string.stripe_nfc_scan_unsupported_card.resolvableString,
        )
    }

    private fun mobileWalletResult(): NfcCardDataParser.Result.Error {
        return NfcCardDataParser.Result.Error(
            analyticsValue = "mobileWalletUnsupportedByNfc",
            userMessage = R.string.stripe_nfc_scan_error_mobile_wallet.resolvableString,
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

        const val TAG_TOKEN_REQUESTOR_ID = "9F19"
        const val TAG_PAYMENT_ACCOUNT_REFERENCE = "9F24"
        const val TAG_FORM_FACTOR_INDICATOR = "9F6E"
        const val TAG_APPLICATION_LABEL = "50"
    }
}
