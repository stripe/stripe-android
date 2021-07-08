package com.stripe.android.paymentsheet.elements

import androidx.annotation.StringRes
import com.stripe.android.paymentsheet.ElementType
import com.stripe.android.paymentsheet.R
import java.util.Locale

internal class CountryConfig(val locale: Locale = Locale.getDefault()) : DropdownConfig {
    override val debugLabel = "country"

    @StringRes
    override val label = R.string.address_label_country

    override val elementType = ElementType.Country

    override fun getDisplayItems(): List<String> =
        CountryUtils.getOrderedCountries(locale).map { it.name }

    override fun convertFromRaw(rawValue: String) =
        CountryUtils.getCountryByCode(CountryCode.create(rawValue), Locale.getDefault())?.name
            ?: getDisplayItems()[0]

    override fun convertToRaw(it: String) =
        CountryUtils.getCountryCodeByName(it, Locale.getDefault())?.value
            ?: null
}
