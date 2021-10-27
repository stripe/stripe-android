package com.stripe.android.paymentsheet.elements

import android.content.res.Resources
import com.stripe.android.model.PaymentMethod
import com.stripe.android.paymentsheet.R
import com.stripe.android.paymentsheet.forms.FormFieldEntry
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import java.io.InputStream
import java.util.Locale

internal data class SimpleTextHeaderElement(
    override val identifier: IdentifierSpec,
    private val paymentMethodType: PaymentMethod.Type?,
    override val controller: Controller? = null,
) : FormElement() {
    override fun getFormFieldValueFlow(): Flow<List<Pair<IdentifierSpec, FormFieldEntry>>> =
        MutableStateFlow(emptyList())

    fun getLabel(resources: Resources): String {
        return when (paymentMethodType) {
            PaymentMethod.Type.Klarna -> getKlarnaLabel(resources)
            else -> ""
        }
    }

    private fun getKlarnaLabel(resources: Resources): String {
        val locale = Locale.getDefault()
        val buyNowCountries =
            parseKlarnaCurrencyMap(
                resources.assets?.open("klarnaHeaderLocaleMap.json")
            )?.get("buy_now")
        val isBuyNow = buyNowCountries?.contains(locale.country) ?: false

        return if (isBuyNow) {
            resources.getString(R.string.stripe_paymentsheet_klarna_buy_now_pay_later)
        } else {
            resources.getString(R.string.stripe_paymentsheet_klarna_pay_later)
        }
    }

    private fun parseKlarnaCurrencyMap(inputStream: InputStream?): Map<String, Set<String>>? {
        val format = Json { ignoreUnknownKeys = true }
        return getJsonStringFromInputStream(inputStream)?.let {
            format.decodeFromString<Map<String, Set<String>>>(it)
        }
    }

    private fun getJsonStringFromInputStream(inputStream: InputStream?) =
        inputStream?.bufferedReader().use { it?.readText() }
}
