package com.stripe.android.ui.core.elements

import com.google.common.truth.Truth.assertThat
import com.stripe.android.ui.core.R
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.util.Locale

@RunWith(RobolectricTestRunner::class)
class KlarnaHelperTest {
    @Test
    fun `klarna has correct header string`() {
        val testMap = mapOf(
            "AT" to R.string.stripe_klarna_buy_now_pay_later,
            "BE" to R.string.stripe_klarna_buy_now_pay_later,
            "DK" to R.string.stripe_klarna_pay_later,
            "FI" to R.string.stripe_klarna_pay_later,
            "FR" to R.string.stripe_klarna_pay_later,
            "DE" to R.string.stripe_klarna_buy_now_pay_later,
            "IT" to R.string.stripe_klarna_buy_now_pay_later,
            "NL" to R.string.stripe_klarna_buy_now_pay_later,
            "NO" to R.string.stripe_klarna_pay_later,
            "ES" to R.string.stripe_klarna_buy_now_pay_later,
            "SE" to R.string.stripe_klarna_buy_now_pay_later,
            "GB" to R.string.stripe_klarna_pay_later,
            "US" to R.string.stripe_klarna_pay_later,
            "GR" to R.string.stripe_klarna_pay_later,
            "IE" to R.string.stripe_klarna_pay_later,
            "PT" to R.string.stripe_klarna_buy_now_pay_later,
            "AU" to R.string.stripe_klarna_buy_now_pay_later,
            "CA" to R.string.stripe_klarna_buy_now_pay_later,
            "CZ" to R.string.stripe_klarna_pay_later,
            "NZ" to R.string.stripe_klarna_pay_later,
            "PL" to R.string.stripe_klarna_buy_now_pay_later,
            "CH" to R.string.stripe_klarna_buy_now_pay_later,
        )

        for (entry in testMap) {
            assertThat(KlarnaHelper.getKlarnaHeader(Locale("", entry.key))).isEqualTo(entry.value)
        }
    }

    @Test
    fun `non supported klarna currency returns empty list`() {
        assertThat(KlarnaHelper.getAllowedCountriesForCurrency("")).isEmpty()
        assertThat(KlarnaHelper.getAllowedCountriesForCurrency(null)).isEmpty()
        assertThat(KlarnaHelper.getAllowedCountriesForCurrency("ARS")).isEmpty()
    }

    @Test
    fun `supported klarna currency returns correct countries`() {
        assertThat(KlarnaHelper.getAllowedCountriesForCurrency("eur"))
            .containsExactly("AT", "FI", "DE", "NL", "BE", "ES", "IT", "FR", "GR", "IE", "PT")

        assertThat(KlarnaHelper.getAllowedCountriesForCurrency("dkk")).containsExactly("DK")
        assertThat(KlarnaHelper.getAllowedCountriesForCurrency("nok")).containsExactly("NO")
        assertThat(KlarnaHelper.getAllowedCountriesForCurrency("sek")).containsExactly("SE")
        assertThat(KlarnaHelper.getAllowedCountriesForCurrency("gbp")).containsExactly("GB")
        assertThat(KlarnaHelper.getAllowedCountriesForCurrency("usd")).containsExactly("US")
        assertThat(KlarnaHelper.getAllowedCountriesForCurrency("aud")).containsExactly("AU")
        assertThat(KlarnaHelper.getAllowedCountriesForCurrency("cad")).containsExactly("CA")
        assertThat(KlarnaHelper.getAllowedCountriesForCurrency("czk")).containsExactly("CZ")
        assertThat(KlarnaHelper.getAllowedCountriesForCurrency("nzd")).containsExactly("NZ")
        assertThat(KlarnaHelper.getAllowedCountriesForCurrency("pln")).containsExactly("PL")
        assertThat(KlarnaHelper.getAllowedCountriesForCurrency("chf")).containsExactly("CH")
    }
}
