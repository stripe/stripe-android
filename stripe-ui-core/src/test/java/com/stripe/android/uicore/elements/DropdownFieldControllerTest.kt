package com.stripe.android.uicore.elements

import androidx.compose.ui.autofill.ContentType
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import java.util.Locale

class DropdownFieldControllerTest {

    @Test
    fun `initial country selection and label do not build the menu`() {
        val controller = DropdownFieldController(
            CountryConfig(
                locale = Locale.US,
                expandedLabelMapper = {
                    check(it.code.value == "IT") { "Only the selected country should be localized" }
                    it.name
                },
                autofillType = ContentType.AddressCountry,
            ),
            initialValue = "it",
        )

        assertThat(controller.rawFieldValue.value).isEqualTo("IT")
        assertThat(controller.selectedItemLabel.value).isEqualTo("Italy")
        assertThat(controller.formFieldValue.value.value).isEqualTo("IT")
        assertThat(controller.itemCount).isEqualTo(235)
    }

    @Test
    fun `device country is selected without building the menu`() {
        val controller = DropdownFieldController(
            CountryConfig(
                locale = Locale.US,
                expandedLabelMapper = {
                    check(it.code.value == "US") { "Only the selected country should be localized" }
                    it.name
                },
                autofillType = ContentType.AddressCountry,
            ),
        )

        assertThat(controller.rawFieldValue.value).isEqualTo("US")
        assertThat(controller.selectedItemLabel.value).isEqualTo("United States")
    }

    @Test
    fun `explicit country avoids the default when the locale has no country`() {
        val controller = DropdownFieldController(
            CountryConfig(
                locale = Locale.ENGLISH,
                expandedLabelMapper = {
                    check(it.code.value == "IT") { "Only the selected country should be localized" }
                    it.name
                },
                autofillType = ContentType.AddressCountry,
            ),
            initialValue = "IT",
        )

        assertThat(controller.rawFieldValue.value).isEqualTo("IT")
        assertThat(controller.selectedItemLabel.value).isEqualTo("Italy")
    }

    @Test
    fun `opening the menu preserves the selected country`() {
        val controller = DropdownFieldController(
            CountryConfig(locale = Locale.US, autofillType = ContentType.AddressCountry),
            initialValue = "IT",
        )

        assertThat(controller.displayItems.first()).isEqualTo("🇺🇸 United States")
        assertThat(controller.rawFieldValue.value).isEqualTo("IT")
        assertThat(controller.displayItems[requireNotNull(controller.selectedIndex.value)]).isEqualTo("🇮🇹 Italy")
    }

    @Test
    fun `language only locale retains the first localized country as default`() {
        val config = CountryConfig(
            onlyShowCountryCodes = setOf("DE", "AT"),
            locale = Locale.ENGLISH,
            autofillType = ContentType.AddressCountry,
        )

        assertThat(DropdownFieldController(config).rawFieldValue.value).isEqualTo("AT")
    }

    @Test
    fun `country with no default stays empty until a value is supplied`() {
        val controller = DropdownFieldController(
            CountryConfig(
                mode = DropdownConfig.Mode.Full(selectsFirstOptionAsDefault = false),
                locale = Locale.US,
                autofillType = ContentType.AddressCountry,
            ),
        )

        assertThat(controller.rawFieldValue.value).isNull()
        assertThat(controller.isComplete.value).isFalse()
        controller.onRawValueChange("unknown")
        assertThat(controller.rawFieldValue.value).isEqualTo("US")
    }

    @Test
    fun `two character localized country name is accepted`() {
        val controller = DropdownFieldController(
            CountryConfig(locale = Locale.SIMPLIFIED_CHINESE, autofillType = ContentType.AddressCountry),
        )

        controller.onAutofillValue("法国")

        assertThat(controller.rawFieldValue.value).isEqualTo("FR")
        assertThat(controller.selectedItemLabel.value).isEqualTo("法国")
    }

    private val controller = DropdownFieldController(
        AdministrativeAreaConfig(AdministrativeAreaConfig.Country.US())
    )

    @Test
    fun `onAutofillValue with abbreviation selects matching state`() {
        controller.onAutofillValue("CA")
        assertThat(controller.rawFieldValue.value).isEqualTo("CA")
    }

    @Test
    fun `onAutofillValue with full name selects matching state`() {
        controller.onAutofillValue("California")
        assertThat(controller.rawFieldValue.value).isEqualTo("CA")
    }

    @Test
    fun `onAutofillValue with full name is case insensitive`() {
        controller.onAutofillValue("california")
        assertThat(controller.rawFieldValue.value).isEqualTo("CA")
    }

    @Test
    fun `initial lowercase country code is exposed as canonical code`() {
        val config = CountryConfig(
            onlyShowCountryCodes = setOf("AT", "IT"),
            locale = Locale.US,
            autofillType = ContentType.AddressCountry,
        )
        assertThat(config.rawItems.indexOf("IT")).isGreaterThan(0)

        val controller = DropdownFieldController(config, initialValue = "it")

        assertThat(controller.rawFieldValue.value).isEqualTo("IT")
    }

    @Test
    fun `initial country name is exposed as canonical code`() {
        val config = CountryConfig(
            onlyShowCountryCodes = setOf("AT", "IT"),
            locale = Locale.US,
            autofillType = ContentType.AddressCountry,
        )
        assertThat(config.rawItems.indexOf("IT")).isGreaterThan(0)

        val controller = DropdownFieldController(config, initialValue = "Italy")

        assertThat(controller.rawFieldValue.value).isEqualTo("IT")
    }

    @Test
    fun `initial lowercase state code is exposed as canonical code`() {
        val controller = DropdownFieldController(
            AdministrativeAreaConfig(AdministrativeAreaConfig.Country.US()),
            initialValue = "wa",
        )

        assertThat(controller.rawFieldValue.value).isEqualTo("WA")
    }

    @Test
    fun `initial state name is exposed as canonical code`() {
        val controller = DropdownFieldController(
            AdministrativeAreaConfig(AdministrativeAreaConfig.Country.US()),
            initialValue = "Washington",
        )

        assertThat(controller.rawFieldValue.value).isEqualTo("WA")
    }
}
