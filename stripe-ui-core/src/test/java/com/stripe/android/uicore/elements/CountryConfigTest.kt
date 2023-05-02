package com.stripe.android.uicore.elements

import com.google.common.truth.Truth.assertThat
import com.stripe.android.core.model.CountryUtils
import org.junit.Test
import java.util.Locale
import com.stripe.android.core.R as CoreR

class CountryConfigTest {

    @Test
    fun `Verify the displayed country list`() {
        assertThat(CountryConfig(locale = Locale.US).displayItems[0])
            .isEqualTo("🇺🇸 United States")
    }

    @Test
    fun `Verify the label`() {
        assertThat(CountryConfig(locale = Locale.US).label)
            .isEqualTo(CoreR.string.stripe_address_label_country_or_region)
    }

    @Test
    fun `Verify only show countries requested`() {
        assertThat(
            CountryConfig(
                onlyShowCountryCodes = setOf("AT"),
                locale = Locale.US
            ).displayItems[0]
        ).isEqualTo("🇦🇹 Austria")
    }

    @Test
    fun `Regular mode shows only country name when collapsed`() {
        assertThat(
            CountryConfig(
                onlyShowCountryCodes = setOf("AT"),
                locale = Locale.US,
                tinyMode = false
            ).getSelectedItemLabel(0)
        ).isEqualTo("Austria")
    }

    @Test
    fun `When collapsed correct label is shown`() {
        assertThat(
            CountryConfig(
                onlyShowCountryCodes = setOf("AT"),
                locale = Locale.US,
                tinyMode = true,
                collapsedLabelMapper = { country ->
                    CountryConfig.countryCodeToEmoji(country.code.value)
                },
                expandedLabelMapper = { country -> country.name }
            ).getSelectedItemLabel(0)
        ).isEqualTo("🇦🇹")
    }

    @Test
    fun `test country list `() {
        val defaultCountries = CountryConfig(
            onlyShowCountryCodes = emptySet(),
            locale = Locale.US
        ).displayItems
        val supportedCountries = CountryConfig(
            onlyShowCountryCodes = CountryUtils.supportedBillingCountries,
            locale = Locale.US
        ).displayItems

        val excludedCountries = setOf(
            "American Samoa", "Christmas Island", "Cocos (Keeling) Islands", "Cuba",
            "Heard & McDonald Islands", "Iran", "Marshall Islands", "Micronesia",
            "Norfolk Island", "North Korea", "Northern Mariana Islands", "Palau", "Sudan", "Syria",
            "U.S. Outlying Islands", "U.S. Virgin Islands"
        )

        excludedCountries.forEach {
            assertThat(supportedCountries.contains(it)).isFalse()
        }

        assertThat(
            defaultCountries.size
        ).isEqualTo(235)

        assertThat(
            supportedCountries.size
        ).isEqualTo(235)
    }
}
