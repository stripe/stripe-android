package com.stripe.android.uicore.elements

import androidx.annotation.RestrictTo
import androidx.compose.ui.autofill.ContentType
import com.stripe.android.core.model.Country
import com.stripe.android.core.model.CountryCode
import com.stripe.android.core.model.CountryUtils
import com.stripe.android.core.strings.resolvableString
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap
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
    override val mode: DropdownConfig.Mode = DropdownConfig.Mode.Full(),
    override val disableDropdownWithSingleElement: Boolean = false,
    private val collapsedLabelMapper: (Country) -> String = { country -> country.name },
    private val expandedLabelMapper: (Country) -> String = { country ->
        "${countryCodeToEmoji(country.code.value)} ${country.name}"
    },
    override val autofillType: ContentType?,
) : DropdownConfig {
    override val debugLabel = "country"

    override val label = resolvableString(CoreR.string.stripe_address_label_country_or_region)

    private val allowedCountryCodes = CountryUtils.supportedBillingCountries.filter {
        onlyShowCountryCodes.isEmpty() || it in onlyShowCountryCodes
    }.toSet()
    private val localizedCountries = ConcurrentHashMap<String, Country>()

    internal val countries by lazy {
        CountryUtils.getOrderedCountries(locale).filter { it.code.value in allowedCountryCodes }
    }
    override val rawItems by lazy { countries.map { it.code.value } }
    override val displayItems: List<String> by lazy { countries.map(expandedLabelMapper) }
    override val itemCount: Int = allowedCountryCodes.size

    // Preserve the existing alphabetical fallback when the locale has no allowed country.
    private val defaultRawValue: String? by lazy {
        locale.country.takeIf { it in allowedCountryCodes }
            ?: allowedCountryCodes.singleOrNull()
            ?: rawItems.firstOrNull()
    }

    override fun getSelectedItemLabel(index: Int) =
        countries.getOrNull(index)?.let(collapsedLabelMapper) ?: ""

    override val defaultItem: DropdownConfig.Item?
        get() = defaultRawValue?.let(::itemForCode)

    override fun itemForRaw(value: String): DropdownConfig.Item? = convertToRaw(value)?.let(::itemForCode)

    override fun itemForAutofill(value: String): DropdownConfig.Item? = itemForRaw(value)

    private fun convertToRaw(value: String): String? {
        val countryCode = value.uppercase(Locale.ROOT)
        if (countryCode in allowedCountryCodes) return countryCode
        if (value.isBlank()) return defaultRawValue
        return countries.firstOrNull {
            it.name.equals(value, ignoreCase = true) || expandedLabelMapper(it).equals(value, ignoreCase = true)
        }?.code?.value ?: defaultRawValue
    }

    override fun convertFromRaw(rawValue: String): String = itemForRaw(rawValue)?.displayText.orEmpty()

    private fun itemForCode(code: String): DropdownConfig.Item {
        val country = countryForCode(code)
        return DropdownConfig.Item(code, expandedLabelMapper(country), collapsedLabelMapper(country))
    }

    private fun countryForCode(code: String): Country = localizedCountries.getOrPut(code) {
        // CountryUtils.getDisplayCountry also initializes the complete localized country list.
        Country(CountryCode.create(code), Locale("", code).getDisplayCountry(locale))
    }

    @Suppress("MagicNumber")
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    companion object {
        /**
         * Convert 2-letter country code to the corresponding flag, using
         * [regional indicator symbols](https://en.wikipedia.org/wiki/Regional_indicator_symbol).
         */
        @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
        fun countryCodeToEmoji(countryCode: String): String {
            if (countryCode.length != 2) {
                return "🌐"
            }

            val firstLetter = Character.codePointAt(countryCode, 0) - 0x41 + 0x1F1E6
            val secondLetter = Character.codePointAt(countryCode, 1) - 0x41 + 0x1F1E6
            return String(Character.toChars(firstLetter).plus(Character.toChars(secondLetter)))
        }
    }
}
