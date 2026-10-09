package com.stripe.android.uicore.elements

import com.google.common.truth.Truth.assertThat
import com.stripe.android.core.model.CountryUtils
import com.stripe.android.core.strings.resolvableString
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
            .isEqualTo(resolvableString(CoreR.string.stripe_address_label_country_or_region))
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
    fun `country codes are matched without regard to case`() {
        val config = italyAndAustriaConfig()
        val italyIndex = config.rawItems.indexOf("IT")

        assertThat(italyIndex).isGreaterThan(0)
        assertThat(config.convertFromRaw("IT")).isEqualTo(config.displayItems[italyIndex])
        assertThat(config.convertFromRaw("it")).isEqualTo(config.displayItems[italyIndex])
    }

    @Test
    fun `country names are matched without regard to case`() {
        val config = italyAndAustriaConfig()
        val italyIndex = config.rawItems.indexOf("IT")

        assertThat(italyIndex).isGreaterThan(0)
        assertThat(config.convertFromRaw("Italy")).isEqualTo(config.displayItems[italyIndex])
        assertThat(config.convertFromRaw("italy")).isEqualTo(config.displayItems[italyIndex])
    }

    @Test
    fun `country matching uses configured locale when system locale differs`() {
        val defaultLocale = Locale.getDefault()
        try {
            Locale.setDefault(Locale.US)
            val config = CountryConfig(
                onlyShowCountryCodes = setOf("AT", "IT"),
                locale = Locale.FRANCE,
            )
            val italyIndex = config.rawItems.indexOf("IT")

            assertThat(italyIndex).isGreaterThan(0)
            assertThat(config.displayItems[italyIndex]).isEqualTo("🇮🇹 Italie")
            assertThat(config.convertFromRaw("IT")).isEqualTo(config.displayItems[italyIndex])
            assertThat(config.convertFromRaw("Italie")).isEqualTo(config.displayItems[italyIndex])
        } finally {
            Locale.setDefault(defaultLocale)
        }
    }

    @Test
    fun `country name matching is independent of display mapper`() {
        val config = CountryConfig(
            onlyShowCountryCodes = setOf("AT", "IT"),
            locale = Locale.US,
            expandedLabelMapper = { country -> "Code: ${country.code.value}" },
        )
        val italyIndex = config.rawItems.indexOf("IT")

        assertThat(italyIndex).isGreaterThan(0)
        assertThat(config.convertFromRaw("Italy")).isEqualTo("Code: IT")
    }

    @Test
    fun `disallowed country name falls back to first allowed country`() {
        val config = CountryConfig(
            onlyShowCountryCodes = setOf("AT", "US"),
            locale = Locale.US,
        )

        assertThat(config.rawItems).doesNotContain("IT")
        assertThat(config.convertFromRaw("Italy")).isEqualTo(config.displayItems.first())
    }

    @Test
    fun `unknown country value falls back to first country`() {
        val config = italyAndAustriaConfig()

        assertThat(config.convertFromRaw("Unknown country"))
            .isEqualTo(config.displayItems.first())
    }

    @Test
    fun `Regular mode shows only country name when collapsed`() {
        assertThat(
            CountryConfig(
                onlyShowCountryCodes = setOf("AT"),
                locale = Locale.US,
                mode = DropdownConfig.Mode.Full(),
            ).getSelectedItemLabel(0)
        ).isEqualTo("Austria")
    }

    @Test
    fun `When collapsed correct label is shown`() {
        assertThat(
            CountryConfig(
                onlyShowCountryCodes = setOf("AT"),
                locale = Locale.US,
                mode = DropdownConfig.Mode.Condensed,
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
            "American Samoa",
            "Christmas Island",
            "Cocos (Keeling) Islands",
            "Cuba",
            "Heard & McDonald Islands",
            "Iran",
            "Marshall Islands",
            "Micronesia",
            "Norfolk Island",
            "North Korea",
            "Northern Mariana Islands",
            "Palau",
            "Sudan",
            "Syria",
            "U.S. Outlying Islands",
            "U.S. Virgin Islands"
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

    private fun italyAndAustriaConfig() = CountryConfig(
        onlyShowCountryCodes = setOf("AT", "IT"),
        locale = Locale.US,
    )
}
