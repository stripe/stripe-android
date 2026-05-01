package com.stripe.android.payments.samsungpay

import com.google.common.truth.Truth.assertThat
import org.json.JSONException
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class SamsungPayCredentialParserTest {

    @Test
    fun `parse valid credential extracts all fields`() {
        val credential = """
            {
              "3DS": {
                "data": "YWJjMTIz",
                "type": "S",
                "version": "100"
              },
              "payment_card_brand": "VI",
              "payment_last4_dpan": "1234",
              "payment_last4_fpan": "5678",
              "payment_currency_type": "USD"
            }
        """.trimIndent()

        val result = SamsungPayCredentialParser.parse(credential)

        assertThat(result.rawCredential).isEqualTo(credential)
        assertThat(result.cryptogram).isEqualTo("YWJjMTIz")
        assertThat(result.cryptogramType).isEqualTo("S")
        assertThat(result.version).isEqualTo("100")
        assertThat(result.cardBrand).isEqualTo("VI")
        assertThat(result.last4Dpan).isEqualTo("1234")
        assertThat(result.last4Fpan).isEqualTo("5678")
        assertThat(result.currencyType).isEqualTo("USD")
    }

    @Test
    fun `parse Mastercard credential`() {
        val credential = """
            {
              "3DS": {
                "data": "eHl6Nzg5",
                "type": "S",
                "version": "200"
              },
              "payment_card_brand": "MC",
              "payment_last4_dpan": "9999",
              "payment_last4_fpan": "8888",
              "payment_currency_type": "EUR"
            }
        """.trimIndent()

        val result = SamsungPayCredentialParser.parse(credential)

        assertThat(result.cryptogram).isEqualTo("eHl6Nzg5")
        assertThat(result.cardBrand).isEqualTo("MC")
        assertThat(result.currencyType).isEqualTo("EUR")
    }

    @Test(expected = JSONException::class)
    fun `parse invalid JSON throws JSONException`() {
        SamsungPayCredentialParser.parse("not json")
    }

    @Test(expected = JSONException::class)
    fun `parse missing 3DS object throws JSONException`() {
        val credential = """
            {
              "payment_card_brand": "VI",
              "payment_last4_dpan": "1234",
              "payment_last4_fpan": "5678",
              "payment_currency_type": "USD"
            }
        """.trimIndent()

        SamsungPayCredentialParser.parse(credential)
    }

    @Test(expected = JSONException::class)
    fun `parse missing cryptogram data throws JSONException`() {
        val credential = """
            {
              "3DS": {
                "type": "S",
                "version": "100"
              },
              "payment_card_brand": "VI",
              "payment_last4_dpan": "1234",
              "payment_last4_fpan": "5678",
              "payment_currency_type": "USD"
            }
        """.trimIndent()

        SamsungPayCredentialParser.parse(credential)
    }
}
