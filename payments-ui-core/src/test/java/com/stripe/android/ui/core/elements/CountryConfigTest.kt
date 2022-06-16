package com.stripe.android.ui.core.elements

import com.google.common.truth.Truth.assertThat
import com.stripe.android.ui.core.R
import org.junit.Test
import java.util.Locale

class CountryConfigTest {

    @Test
    fun `Verify the displayed country list`() {
        assertThat(CountryConfig(locale = Locale.US).displayItems[0])
            .isEqualTo("🇺🇸 United States")
    }

    @Test
    fun `Verify the label`() {
        assertThat(CountryConfig(locale = Locale.US).label)
            .isEqualTo(R.string.address_label_country_or_region)
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
    fun `Verify converts diplay name to country`() {
        val config = CountryConfig(
            onlyShowCountryCodes = setOf("AT"),
            locale = Locale.US
        )
        assertThat(
            config.convertToRaw(config.displayItems[0])
        ).isEqualTo("AT")
    }

    @Test
    fun `Regular mode shows only country name when collapsed`() {
        assertThat(
            CountryConfig(
                onlyShowCountryCodes = setOf("AT"),
                locale = Locale.US,
                flagMode = false
            ).getSelectedItemLabel(0)
        ).isEqualTo("Austria")
    }

    @Test
    fun `Flag mode shows only flag when collapsed`() {
        assertThat(
            CountryConfig(
                onlyShowCountryCodes = setOf("AT"),
                locale = Locale.US,
                flagMode = true
            ).getSelectedItemLabel(0)
        ).isEqualTo("🇦🇹")
    }

    @Test
    fun `Flag mode converts diplay name to country`() {
        val config = CountryConfig(
            onlyShowCountryCodes = setOf("AT"),
            locale = Locale.US,
            flagMode = true
        )
        assertThat(
            config.convertToRaw(config.displayItems[0])
        ).isEqualTo("AT")
    }

    @Test
    fun `test country list `() {
        val defaultCountries = CountryConfig(
            onlyShowCountryCodes = emptySet(),
            locale = Locale.US
        ).displayItems
        val supportedCountries = CountryConfig(
            onlyShowCountryCodes = supportedBillingCountries,
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
        ).isEqualTo(249)

        assertThat(
            supportedCountries.size
        ).isEqualTo(233)
    }
}
