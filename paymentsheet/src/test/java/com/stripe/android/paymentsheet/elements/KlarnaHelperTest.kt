package com.stripe.android.paymentsheet.elements

import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.util.Locale
import com.google.common.truth.Truth.assertThat
import com.stripe.android.paymentsheet.R

@RunWith(RobolectricTestRunner::class)
class KlarnaHelperTest {
    @Test
    fun `klarna has correct header string`() {
        val testMap = mapOf(
            "AT" to R.string.stripe_paymentsheet_klarna_buy_now_pay_later,
            "BE" to R.string.stripe_paymentsheet_klarna_buy_now_pay_later,
            "DK" to R.string.stripe_paymentsheet_klarna_pay_later,
            "FI" to R.string.stripe_paymentsheet_klarna_pay_later,
            "FR" to R.string.stripe_paymentsheet_klarna_pay_later,
            "DE" to R.string.stripe_paymentsheet_klarna_buy_now_pay_later,
            "IT" to R.string.stripe_paymentsheet_klarna_buy_now_pay_later,
            "NL" to R.string.stripe_paymentsheet_klarna_buy_now_pay_later,
            "NO" to R.string.stripe_paymentsheet_klarna_pay_later,
            "ES" to R.string.stripe_paymentsheet_klarna_buy_now_pay_later,
            "SE" to R.string.stripe_paymentsheet_klarna_buy_now_pay_later,
            "GB" to R.string.stripe_paymentsheet_klarna_pay_later,
            "US" to R.string.stripe_paymentsheet_klarna_pay_later,
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
            .containsExactly("AT", "FI", "DE", "NL", "BE", "ES", "IT", "FR")

        assertThat(KlarnaHelper.getAllowedCountriesForCurrency("dkk")).containsExactly("DK")
        assertThat(KlarnaHelper.getAllowedCountriesForCurrency("nok")).containsExactly("NO")
        assertThat(KlarnaHelper.getAllowedCountriesForCurrency("sek")).containsExactly("SE")
        assertThat(KlarnaHelper.getAllowedCountriesForCurrency("gbp")).containsExactly("GB")
        assertThat(KlarnaHelper.getAllowedCountriesForCurrency("usd")).containsExactly("US")
    }
}
