package com.stripe.android.ui.core.elements

import androidx.annotation.RestrictTo
import androidx.annotation.StringRes
import com.stripe.android.core.model.CountryCode
import com.stripe.android.core.model.CountryUtils
import com.stripe.android.ui.core.R
import java.util.Locale

/**
 * This is the configuration for a country dropdown.
 *
 * @property onlyShowCountryCodes: a list of country code that should be shown.  If empty all
 * countries will be shown.
 * @property locale: this is the locale used to display the country names.
 * @property flagMode: when true, will display items in "flag mode", a smaller form which shows the
 * flags of the countries before their name in the dropdown list, and only the flag when the list is
 * collapsed.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
class CountryConfig(
    val onlyShowCountryCodes: Set<String> = emptySet(),
    val locale: Locale = Locale.getDefault(),
    val flagMode: Boolean = false
) : DropdownConfig {
    override val debugLabel = "country"

    @StringRes
    override val label = R.string.address_label_country

    override val tinyMode = flagMode

    internal val countries = CountryUtils.getOrderedCountries(locale)
        .filter {
            onlyShowCountryCodes.isEmpty() || onlyShowCountryCodes.contains(it.code.value)
        }

    override val displayItems: List<String> = countries.map { country ->
        "${countryCodeToEmoji(country.code.value)} ${country.name}"
    }

    override fun getSelectedItemLabel(index: Int) =
        countries.getOrNull(index)?.let {
            if (flagMode) {
                countryCodeToEmoji(it.code.value)
            } else {
                it.name
            }
        } ?: ""

    override fun convertFromRaw(rawValue: String) =
        CountryUtils.getCountryByCode(CountryCode.create(rawValue), Locale.getDefault())?.name
            ?: displayItems[0]

    override fun convertToRaw(displayName: String) =
        CountryUtils.getCountryCodeByName(getCountryName(displayName), Locale.getDefault())?.value

    /**
     * Convert 2-letter country code to the corresponding flag, using
     * [regional indicator symbols](https://en.wikipedia.org/wiki/Regional_indicator_symbol).
     */
    private fun countryCodeToEmoji(countryCode: String): String {
        if (countryCode.length != 2) {
            return "🌐"
        }

        val firstLetter = Character.codePointAt(countryCode, 0) - 0x41 + 0x1F1E6
        val secondLetter = Character.codePointAt(countryCode, 1) - 0x41 + 0x1F1E6
        return String(Character.toChars(firstLetter)) + String(Character.toChars(secondLetter))
    }

    private fun getCountryName(displayName: String) =
        // Remove the flag which is located before the first space
        displayName.substring(displayName.indexOf(" ") + 1)
}
