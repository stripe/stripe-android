package com.stripe.android.uicore.elements

import com.google.common.truth.Truth.assertThat
import org.junit.Test
import java.util.Locale

class DropdownFieldControllerTest {

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
