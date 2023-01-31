package com.stripe.android.paymentsheet.addresselement

import com.google.common.truth.Truth.assertThat
import com.stripe.android.uicore.elements.AddressType
import com.stripe.android.uicore.elements.PhoneNumberState
import org.junit.Test

class AddressSpecFactoryTest {
    private val required = AddressLauncher.AdditionalFieldsConfiguration.FieldConfiguration.REQUIRED

    @Test
    fun `null config creates condensed addressSpec`() {
        val addressSpec = AddressSpecFactory.create(
            condensedForm = true,
            config = null,
            onNavigation = {}
        )
        assertThat(addressSpec.showLabel).isFalse()
        assertThat(addressSpec.allowedCountryCodes).isNotEmpty()
        val type = addressSpec.type as AddressType.ShippingCondensed
        assertThat(type.googleApiKey).isNull()
        assertThat(type.autocompleteCountries).isNull()
        assertThat(type.phoneNumberState).isEqualTo(PhoneNumberState.OPTIONAL)
    }

    @Test
    fun `config creates condensed addressSpec`() {
        val addressSpec = AddressSpecFactory.create(
            condensedForm = true,
            config = AddressLauncher.Configuration(
                googlePlacesApiKey = "apiKey",
                autocompleteCountries = setOf("US", "CA"),
                additionalFields = AddressLauncher.AdditionalFieldsConfiguration(phone = required)
            ),
            onNavigation = {}
        )
        assertThat(addressSpec.showLabel).isFalse()
        assertThat(addressSpec.allowedCountryCodes).isEmpty()
        val type = addressSpec.type as AddressType.ShippingCondensed
        assertThat(type.googleApiKey).isEqualTo("apiKey")
        assertThat(type.autocompleteCountries).isEqualTo(setOf("US", "CA"))
        assertThat(type.phoneNumberState).isEqualTo(PhoneNumberState.REQUIRED)
    }

    @Test
    fun `null config creates expanded addressSpec`() {
        val addressSpec = AddressSpecFactory.create(
            condensedForm = false,
            config = null,
            onNavigation = {}
        )
        assertThat(addressSpec.showLabel).isFalse()
        assertThat(addressSpec.allowedCountryCodes).isNotEmpty()
        val type = addressSpec.type as AddressType.ShippingExpanded
        assertThat(type.googleApiKey).isNull()
        assertThat(type.autocompleteCountries).isNull()
        assertThat(type.phoneNumberState).isEqualTo(PhoneNumberState.OPTIONAL)
    }

    @Test
    fun `config creates expanded addressSpec`() {
        val addressSpec = AddressSpecFactory.create(
            condensedForm = false,
            config = AddressLauncher.Configuration(
                googlePlacesApiKey = "apiKey",
                autocompleteCountries = setOf("US", "CA"),
                additionalFields = AddressLauncher.AdditionalFieldsConfiguration(phone = required)
            ),
            onNavigation = {}
        )
        assertThat(addressSpec.showLabel).isFalse()
        assertThat(addressSpec.allowedCountryCodes).isEmpty()
        val type = addressSpec.type as AddressType.ShippingExpanded
        assertThat(type.googleApiKey).isEqualTo("apiKey")
        assertThat(type.autocompleteCountries).isEqualTo(setOf("US", "CA"))
        assertThat(type.phoneNumberState).isEqualTo(PhoneNumberState.REQUIRED)
    }

    @Test
    fun `onNavigation is used in address spec`() {
        var navigationCounter = 0
        val onNavigation: () -> Unit = {
            navigationCounter++
        }
        val addressSpec = AddressSpecFactory.create(
            condensedForm = false,
            config = null,
            onNavigation = onNavigation
        )
        val type = addressSpec.type as AddressType.ShippingExpanded
        assertThat(navigationCounter).isEqualTo(0)
        type.onNavigation()
        assertThat(navigationCounter).isEqualTo(1)
    }

    @Test
    fun `parsePhoneNumberConfig HIDDEN`() {
        val config = AddressLauncher.AdditionalFieldsConfiguration.FieldConfiguration.HIDDEN
        assertThat(AddressSpecFactory.parsePhoneNumberConfig(config))
            .isEqualTo(PhoneNumberState.HIDDEN)
    }

    @Test
    fun `parsePhoneNumberConfig OPTIONAL`() {
        val config = AddressLauncher.AdditionalFieldsConfiguration.FieldConfiguration.OPTIONAL
        assertThat(AddressSpecFactory.parsePhoneNumberConfig(config))
            .isEqualTo(PhoneNumberState.OPTIONAL)
    }

    @Test
    fun `parsePhoneNumberConfig REQUIRED`() {
        val config = AddressLauncher.AdditionalFieldsConfiguration.FieldConfiguration.REQUIRED
        assertThat(AddressSpecFactory.parsePhoneNumberConfig(config))
            .isEqualTo(PhoneNumberState.REQUIRED)
    }

    @Test
    fun `parsePhoneNumberConfig null`() {
        assertThat(AddressSpecFactory.parsePhoneNumberConfig(null))
            .isEqualTo(PhoneNumberState.OPTIONAL)
    }
}
