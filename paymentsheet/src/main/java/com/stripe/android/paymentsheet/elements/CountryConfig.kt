package com.stripe.android.paymentsheet.elements

import androidx.annotation.StringRes
import com.stripe.android.paymentsheet.ElementType
import com.stripe.android.paymentsheet.R
import java.util.Locale

/**
 * This is the configuration for a country dropdown.
 *
 * @property onlyShowCountryCodes: a list of country code that should be shown.  If empty all
 * countries will be shown.
 * @property locale: this is the locale used to display the country names.
 */
internal class CountryConfig(
    val onlyShowCountryCodes: Set<String> = emptySet(),
    val locale: Locale = Locale.getDefault()
) : DropdownConfig {
    override val debugLabel = "country"

    @StringRes
    override val label = R.string.address_label_country

    override val elementType = ElementType.Country

    override fun getDisplayItems(): List<String> = CountryUtils.getOrderedCountries(locale)
        .filter {
            onlyShowCountryCodes.isEmpty() ||
                (onlyShowCountryCodes.isNotEmpty() && onlyShowCountryCodes.contains(it.code.value))
        }.map { it.name }

    override fun convertFromRaw(rawValue: String) =
        CountryUtils.getCountryByCode(CountryCode.create(rawValue), Locale.getDefault())?.name
            ?: getDisplayItems()[0]

    override fun convertToRaw(it: String) =
        CountryUtils.getCountryCodeByName(it, Locale.getDefault())?.value
}
