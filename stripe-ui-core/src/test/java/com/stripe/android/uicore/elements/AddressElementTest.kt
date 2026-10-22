package com.stripe.android.uicore.elements

import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
internal class AddressElementTest {
    @Test
    fun `hidden contact fields are excluded from the address form`() {
        val element = createElement(
            addressInputMode = AddressInputMode.NoAutocomplete(
                phoneNumberConfig = AddressFieldConfiguration.HIDDEN,
                nameConfig = AddressFieldConfiguration.HIDDEN,
                emailConfig = AddressFieldConfiguration.HIDDEN,
            ),
        )

        assertThat(element.getTextFieldIdentifiers().value)
            .containsNoneOf(FormFieldId.Phone, FormFieldId.Name, FormFieldId.Email)
        assertThat(element.countryElement.controller.rawFieldValue.value).isEqualTo("US")
    }

    @Test
    fun `visible contact fields use the initial values`() {
        val element = createElement(
            addressInputMode = AddressInputMode.NoAutocomplete(
                phoneNumberConfig = AddressFieldConfiguration.REQUIRED,
                nameConfig = AddressFieldConfiguration.REQUIRED,
                emailConfig = AddressFieldConfiguration.REQUIRED,
            ),
        )

        val values = element.getFormFieldValueFlow().value.toMap()

        assertThat(values[FormFieldId.Name]?.value).isEqualTo("Jane Doe")
        assertThat(values[FormFieldId.Email]?.value).isEqualTo("jane@example.com")
        assertThat(element.phoneNumberElement.controller.getCountryCode()).isEqualTo("US")
    }

    @Test
    fun `visible phone country follows address country changes`() {
        val element = createElement(
            addressInputMode = AddressInputMode.NoAutocomplete(
                phoneNumberConfig = AddressFieldConfiguration.REQUIRED,
                nameConfig = AddressFieldConfiguration.HIDDEN,
                emailConfig = AddressFieldConfiguration.HIDDEN,
            ),
        )
        assertThat(element.fields.value).isNotEmpty()
        assertThat(element.phoneNumberElement.controller.getCountryCode()).isEqualTo("US")

        element.countryElement.controller.onRawValueChange("CA")
        assertThat(element.fields.value).isNotEmpty()

        assertThat(element.phoneNumberElement.controller.getCountryCode()).isEqualTo("CA")
    }

    private fun createElement(addressInputMode: AddressInputMode): AddressElement {
        return AddressElement(
            _identifier = FormFieldId.BillingAddress,
            rawValuesMap = mapOf(
                FormFieldId.Country to "US",
                FormFieldId.Name to "Jane Doe",
                FormFieldId.Email to "jane@example.com",
                FormFieldId.Phone to "4155550123",
            ),
            addressInputMode = addressInputMode,
            sameAsShippingElement = null,
            shippingValuesMap = null,
        )
    }
}
