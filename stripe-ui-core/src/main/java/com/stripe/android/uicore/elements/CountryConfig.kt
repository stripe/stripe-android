package com.stripe.android.uicore.elements

import androidx.annotation.RestrictTo
import androidx.annotation.StringRes
import com.stripe.android.core.model.Country
import com.stripe.android.core.model.CountryCode
import com.stripe.android.core.model.CountryUtils
import java.util.Locale
import com.stripe.android.core.R as CoreR

/**
 * This is the configuration for a country dropdown.
 *
 * @property onlyShowCountryCodes: a list of country codes that should be shown. If empty, all
 * countries will be shown.
 * @property locale: the locale used to display the country names.
 * @property tinyMode: whether to display in "tiny mode" when collapsed, a smaller UI form used when
 * the dropdown menu is inside another component, like [PhoneNumberElement].
 * @property collapsedLabelMapper: function called to get the collapsed label for the given country.
 * @param expandedLabelMapper: function called to get the expanded label for the given country.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
class CountryConfig(
    val onlyShowCountryCodes: Set<String> = emptySet(),
    val locale: Locale = Locale.getDefault(),
    override val tinyMode: Boolean = false,
    override val disableDropdownWithSingleElement: Boolean = false,
    private val collapsedLabelMapper: (Country) -> String = { country -> country.name },
    expandedLabelMapper: (Country) -> String = { country ->
        "${countryCodeToEmoji(country.code.value)} ${country.name}"
    }
) : DropdownConfig {
    override val debugLabel = "country"

    @StringRes
    override val label = CoreR.string.stripe_address_label_country_or_region

    internal val countries = CountryUtils.getOrderedCountries(locale)
        .filter {
            onlyShowCountryCodes.isEmpty() || onlyShowCountryCodes.contains(it.code.value)
        }

    override val rawItems = countries.map { it.code.value }

    override val displayItems: List<String> = countries.map(expandedLabelMapper)

    override val showSearch: Boolean
        get() = countries.size > 10

    override fun getSelectedItemLabel(index: Int) =
        countries.getOrNull(index)?.let(collapsedLabelMapper) ?: ""

    override fun convertFromRaw(rawValue: String) =
        CountryUtils.getCountryByCode(CountryCode.create(rawValue), Locale.getDefault())
            ?.let { country ->
                countries.indexOf(country).takeUnless { it == -1 }?.let {
                    displayItems[it]
                }
            } ?: displayItems.firstOrNull() ?: ""

    @Suppress("MagicNumber")
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    companion object {
        /**
         * Convert 2-letter country code to the corresponding flag, using
         * [regional indicator symbols](https://en.wikipedia.org/wiki/Regional_indicator_symbol).
         */
        internal fun countryCodeToEmoji(countryCode: String): String {
            if (countryCode.length != 2) {
                return "🌐"
            }

            val firstLetter = Character.codePointAt(countryCode, 0) - 0x41 + 0x1F1E6
            val secondLetter = Character.codePointAt(countryCode, 1) - 0x41 + 0x1F1E6
            return String(Character.toChars(firstLetter).plus(Character.toChars(secondLetter)))
        }
    }
}
