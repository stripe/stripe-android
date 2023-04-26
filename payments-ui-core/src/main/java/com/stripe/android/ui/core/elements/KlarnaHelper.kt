package com.stripe.android.ui.core.elements

import androidx.annotation.RestrictTo
import com.stripe.android.ui.core.R
import java.util.Locale

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
object KlarnaHelper {
    private val currencyToAllowedCountries = mapOf(
        "eur" to setOf("AT", "FI", "DE", "NL", "BE", "ES", "IT", "FR", "GR", "IE", "PT"),
        "dkk" to setOf("DK"),
        "nok" to setOf("NO"),
        "sek" to setOf("SE"),
        "gbp" to setOf("GB"),
        "usd" to setOf("US"),
        "aud" to setOf("AU"),
        "cad" to setOf("CA"),
        "czk" to setOf("CZ"),
        "nzd" to setOf("NZ"),
        "pln" to setOf("PL"),
        "chf" to setOf("CH"),
    )

    private val buyNowCountries = setOf("AT", "BE", "DE", "IT", "NL", "ES", "SE", "CA", "AU", "PL", "PT", "CH")

    fun getAllowedCountriesForCurrency(currencyCode: String?) =
        currencyToAllowedCountries[currencyCode].orEmpty()

    fun getKlarnaHeader(locale: Locale = Locale.getDefault()) =
        if (buyNowCountries.contains(locale.country)) {
            R.string.stripe_klarna_buy_now_pay_later
        } else {
            R.string.stripe_klarna_pay_later
        }
}
