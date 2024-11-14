package com.stripe.android.ui.core.elements

import com.google.common.truth.Truth.assertThat
import com.stripe.android.uicore.elements.AddressElement
import com.stripe.android.uicore.elements.AddressType
import com.stripe.android.uicore.elements.CountryConfig
import com.stripe.android.uicore.elements.DropdownFieldController
import com.stripe.android.uicore.elements.IdentifierSpec
import com.stripe.android.uicore.elements.PhoneNumberState
import com.stripe.android.uicore.elements.RowElement
import com.stripe.android.uicore.elements.SameAsShippingController
import com.stripe.android.uicore.elements.SameAsShippingElement
import com.stripe.android.uicore.elements.SectionFieldElement
import com.stripe.android.uicore.elements.SimpleTextElement
import com.stripe.android.uicore.elements.SimpleTextFieldController
import com.stripe.android.uicore.elements.TextFieldController
import com.stripe.android.uicore.elements.TextFieldIcon
import com.stripe.android.uicore.forms.FormFieldEntry
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.shadows.ShadowLooper
import java.util.concurrent.atomic.AtomicInteger
import com.stripe.android.uicore.R as UiCoreR

// TODO(ccen) Rewrite the test with generic Element and move it to stripe-ui-core
@RunWith(RobolectricTestRunner::class)
class AddressElementTest {
    private val countryDropdownFieldController = DropdownFieldController(
        CountryConfig(setOf("US", "CA"))
    )

    @Test
    fun `Verify controller error is updated as the fields change based on country`() {
        runBlocking {
            // ZZ does not have state and US does
            val addressElement = AddressElement(
                IdentifierSpec.Generic("address"),
                countryDropdownFieldController = countryDropdownFieldController,
                sameAsShippingElement = null,
                shippingValuesMap = null
            )

            var postalCodeController = addressElement.fields.postalCodeController()

            countryDropdownFieldController.onValueChange(0)
            ShadowLooper.runUiThreadTasksIncludingDelayedTasks()

            postalCodeController.onValueChange("9999")
            postalCodeController.onFocusChange(false)

            assertThat(addressElement.controller.error.first())
                .isNotNull()
            assertThat(addressElement.controller.error.first()?.errorMessage)
                .isEqualTo(UiCoreR.string.stripe_address_zip_invalid)

            countryDropdownFieldController.onValueChange(1)
            ShadowLooper.runUiThreadTasksIncludingDelayedTasks()

            postalCodeController = addressElement.fields.postalCodeController()

            postalCodeController.onValueChange("6E7")
            postalCodeController.onFocusChange(false)
            ShadowLooper.runUiThreadTasksIncludingDelayedTasks()

            assertThat(addressElement.controller.error.first()?.errorMessage)
                .isEqualTo(UiCoreR.string.stripe_address_zip_postal_invalid)
        }
    }

    @Test
    fun `verify flow of form field values`() = runTest {
        val addressElement = AddressElement(
            IdentifierSpec.Generic("address"),
            countryDropdownFieldController = countryDropdownFieldController,
            sameAsShippingElement = null,
            shippingValuesMap = null
        )
        val formFieldValueFlow = addressElement.getFormFieldValueFlow()
        var postalCodeController = addressElement.fields.postalCodeController()

        countryDropdownFieldController.onValueChange(0)

        // Add values to the fields
        postalCodeController.onValueChange("999")

        ShadowLooper.runUiThreadTasksIncludingDelayedTasks()

        // Verify
        var firstForFieldValues = formFieldValueFlow.first()
        assertThat(firstForFieldValues.toMap()[IdentifierSpec.PostalCode])
            .isEqualTo(
                FormFieldEntry("999", false)
            )

        countryDropdownFieldController.onValueChange(1)

        // Add values to the fields
        postalCodeController = addressElement.fields.postalCodeController()
        postalCodeController.onValueChange("A1B2C3")

        ShadowLooper.runUiThreadTasksIncludingDelayedTasks()

        firstForFieldValues = formFieldValueFlow.first()
        assertThat(firstForFieldValues.toMap()[IdentifierSpec.PostalCode])
            .isEqualTo(
                FormFieldEntry("A1B2C3", true)
            )
    }

    @Test
    fun `changing country updates the fields`() = runTest {
        val addressElement = AddressElement(
            IdentifierSpec.Generic("address"),
            countryDropdownFieldController = countryDropdownFieldController,
            sameAsShippingElement = null,
            shippingValuesMap = null
        )

        val country = suspend {
            addressElement.fields
                .first()[0]
                .getFormFieldValueFlow()
                .first()[0].second.value
        }

        countryDropdownFieldController.onValueChange(0)

        assertThat(country()).isEqualTo("US")

        countryDropdownFieldController.onValueChange(1)

        assertThat(country()).isEqualTo("CA")
    }

    @Test
    fun `condensed shipping address element should have name and phone number fields when required`() = runTest {
        val addressElement = AddressElement(
            IdentifierSpec.Generic("address"),
            countryDropdownFieldController = countryDropdownFieldController,
            addressType = AddressType.ShippingCondensed(
                googleApiKey = null,
                autocompleteCountries = setOf(),
                phoneNumberState = PhoneNumberState.REQUIRED
            ) { throw AssertionError("Not Expected") },
            sameAsShippingElement = null,
            shippingValuesMap = null
        )

        val identifierSpecs = addressElement.fields.first().map {
            it.identifier
        }
        assertThat(identifierSpecs.contains(IdentifierSpec.Name)).isTrue()
        assertThat(identifierSpecs.contains(IdentifierSpec.Phone)).isTrue()
    }

    @Test
    fun `hidden phone number field is not shown`() = runTest {
        val addressElement = AddressElement(
            IdentifierSpec.Generic("address"),
            countryDropdownFieldController = countryDropdownFieldController,
            addressType = AddressType.ShippingCondensed(
                googleApiKey = null,
                autocompleteCountries = setOf(),
                phoneNumberState = PhoneNumberState.HIDDEN
            ) { throw AssertionError("Not Expected") },
            sameAsShippingElement = null,
            shippingValuesMap = null
        )

        val identifierSpecs = addressElement.fields.first().map {
            it.identifier
        }
        assertThat(identifierSpecs.contains(IdentifierSpec.Phone)).isFalse()
    }

    @Test
    fun `optional phone number field is shown`() = runTest {
        val addressElement = AddressElement(
            IdentifierSpec.Generic("address"),
            countryDropdownFieldController = countryDropdownFieldController,
            addressType = AddressType.ShippingCondensed(
                googleApiKey = null,
                autocompleteCountries = setOf(),
                phoneNumberState = PhoneNumberState.OPTIONAL
            ) { throw AssertionError("Not Expected") },
            sameAsShippingElement = null,
            shippingValuesMap = null
        )

        val identifierSpecs = addressElement.fields.first().map {
            it.identifier
        }
        assertThat(identifierSpecs.contains(IdentifierSpec.Phone)).isTrue()
    }

    @Test
    fun `country code in initial phone number is displayed correctly`() = runTest {
        val countryCode = "US"
        val phoneNumberCountryCode = "+1"
        val phoneNumberWithoutCountryCode = "8008675309"
        val addressElement = createAddressElement(
            initialValues = mapOf(
                IdentifierSpec.Phone to phoneNumberCountryCode + phoneNumberWithoutCountryCode,
                IdentifierSpec.Country to countryCode
            )
        )

        val phoneNumberController = addressElement.phoneNumberElement.controller

        assertThat(phoneNumberController.initialPhoneNumber).isEqualTo(phoneNumberWithoutCountryCode)
        assertThat(phoneNumberController.getCountryCode()).isEqualTo(countryCode)
    }

    @Test
    fun `country code in initial phone number is displayed correctly when country and country code differ`() = runTest {
        val countryCode = "US"
        val phoneNumberCountryCode = "+44"
        val phoneNumberWithoutCountryCode = "8008675309"
        val addressElement = createAddressElement(
            initialValues = mapOf(
                IdentifierSpec.Phone to phoneNumberCountryCode + phoneNumberWithoutCountryCode,
                IdentifierSpec.Country to countryCode
            )
        )

        val phoneNumberController = addressElement.phoneNumberElement.controller

        assertThat(phoneNumberController.initialPhoneNumber).isEqualTo(phoneNumberWithoutCountryCode)
        assertThat(phoneNumberController.getCountryCode()).isEqualTo("GB")
    }

    @Test
    fun `expanded shipping address element should have name and phone number fields when required`() = runTest {
        val addressElement = AddressElement(
            IdentifierSpec.Generic("address"),
            countryDropdownFieldController = countryDropdownFieldController,
            addressType = AddressType.ShippingExpanded(
                googleApiKey = null,
                autocompleteCountries = null,
                phoneNumberState = PhoneNumberState.REQUIRED,
            ) { throw AssertionError("Not Expected") },
            sameAsShippingElement = null,
            shippingValuesMap = null
        )

        val identifierSpecs = addressElement.fields.first().map {
            it.identifier
        }
        assertThat(identifierSpecs.contains(IdentifierSpec.Name)).isTrue()
        assertThat(identifierSpecs.contains(IdentifierSpec.Phone)).isTrue()
    }

    @Test
    fun `expanded shipping address element should hide phone number when state is hidden`() = runTest {
        val addressElement = AddressElement(
            IdentifierSpec.Generic("address"),
            countryDropdownFieldController = countryDropdownFieldController,
            addressType = AddressType.ShippingExpanded(
                googleApiKey = null,
                autocompleteCountries = null,
                phoneNumberState = PhoneNumberState.HIDDEN,
            ) { throw AssertionError("Not Expected") },
            sameAsShippingElement = null,
            shippingValuesMap = null
        )

        val identifierSpecs = addressElement.fields.first().map {
            it.identifier
        }
        assertThat(identifierSpecs.contains(IdentifierSpec.Phone)).isFalse()
    }

    @Test
    fun `expanded shipping address element should show phone number when state is optional`() = runTest {
        val addressElement = AddressElement(
            IdentifierSpec.Generic("address"),
            countryDropdownFieldController = countryDropdownFieldController,
            addressType = AddressType.ShippingExpanded(
                googleApiKey = null,
                autocompleteCountries = null,
                phoneNumberState = PhoneNumberState.OPTIONAL,
            ) { throw AssertionError("Not Expected") },
            sameAsShippingElement = null,
            shippingValuesMap = null
        )

        val identifierSpecs = addressElement.fields.first().map {
            it.identifier
        }
        assertThat(identifierSpecs.contains(IdentifierSpec.Phone)).isTrue()
    }

    @Test
    fun `normal address element should not have name and phone number fields`() = runTest {
        val addressElement = AddressElement(
            IdentifierSpec.Generic("address"),
            countryDropdownFieldController = countryDropdownFieldController,
            addressType = AddressType.Normal(),
            sameAsShippingElement = null,
            shippingValuesMap = null
        )

        val identifierSpecs = addressElement.fields.first().map {
            it.identifier
        }
        assertThat(identifierSpecs.contains(IdentifierSpec.Name)).isFalse()
        assertThat(identifierSpecs.contains(IdentifierSpec.Phone)).isFalse()
    }

    @Test
    fun `normal address element should not have one line address element`() = runTest {
        val addressElement = AddressElement(
            IdentifierSpec.Generic("address"),
            countryDropdownFieldController = countryDropdownFieldController,
            addressType = AddressType.Normal(),
            sameAsShippingElement = null,
            shippingValuesMap = null
        )

        val identifierSpecs = addressElement.fields.first().map {
            it.identifier
        }
        assertThat(identifierSpecs.contains(IdentifierSpec.OneLineAddress)).isFalse()
    }

    @Test
    fun `condensed shipping address element should have one line address element`() = runTest {
        val addressElement = AddressElement(
            IdentifierSpec.Generic("address"),
            countryDropdownFieldController = countryDropdownFieldController,
            addressType = AddressType.ShippingCondensed(
                "some key",
                setOf("US", "CA"),
                PhoneNumberState.OPTIONAL
            ) { throw AssertionError("Not Expected") },
            sameAsShippingElement = null,
            shippingValuesMap = null
        )

        val identifierSpecs = addressElement.fields.first().map {
            it.identifier
        }
        assertThat(identifierSpecs.contains(IdentifierSpec.OneLineAddress)).isTrue()
    }

    @Test
    fun `AddressElement should not have OneLineAddress when places is unavailable`() = runTest {
        val addressElement = AddressElement(
            IdentifierSpec.Generic("address"),
            countryDropdownFieldController = countryDropdownFieldController,
            addressType = AddressType.ShippingCondensed(
                "some key",
                setOf("US", "CA"),
                PhoneNumberState.OPTIONAL
            ) { throw AssertionError("Not Expected") },
            sameAsShippingElement = null,
            shippingValuesMap = null,
            isPlacesAvailable = { false },
        )
        val identifierSpecs = addressElement.fields.first().map {
            it.identifier
        }
        assertThat(identifierSpecs.contains(IdentifierSpec.OneLineAddress)).isFalse()
    }

    @Test
    fun `AddressElement has no TrailingIcon on Line1 when places is unavailable`() = runTest {
        val countryDropdownFieldController = DropdownFieldController(
            CountryConfig(setOf("US", "CA", "JP"))
        )
        val addressElement = AddressElement(
            IdentifierSpec.Generic("address"),
            countryDropdownFieldController = countryDropdownFieldController,
            addressType = AddressType.ShippingCondensed(
                "some key",
                setOf("US", "CA"),
                PhoneNumberState.OPTIONAL
            ) { throw AssertionError("Not Expected") },
            sameAsShippingElement = null,
            shippingValuesMap = null,
            isPlacesAvailable = { false },
        )
        countryDropdownFieldController.onValueChange(1)

        val trailingIcon = addressElement.trailingIconFor(IdentifierSpec.Line1)
        assertThat(trailingIcon).isNull()
    }

    @Test
    fun `AddressElement has a TrailingIcon on Line1 when places is available`() = runTest {
        val countryDropdownFieldController = DropdownFieldController(
            CountryConfig(setOf("US", "CA", "JP"))
        )
        val onNavigationCounter = AtomicInteger(0)
        val addressElement = AddressElement(
            IdentifierSpec.Generic("address"),
            countryDropdownFieldController = countryDropdownFieldController,
            addressType = AddressType.ShippingExpanded(
                "some key",
                setOf("US", "CA"),
                PhoneNumberState.OPTIONAL
            ) { onNavigationCounter.getAndIncrement() },
            sameAsShippingElement = null,
            shippingValuesMap = null,
            isPlacesAvailable = { true },
        )
        countryDropdownFieldController.onValueChange(1)

        val line1TrailingIcon = addressElement.trailingIconFor(IdentifierSpec.Line1)
        assertThat(line1TrailingIcon?.contentDescription)
            .isEqualTo(UiCoreR.string.stripe_address_search_content_description)
        assertThat(addressElement.trailingIconFor(IdentifierSpec.Line2)).isNull()

        line1TrailingIcon?.onClick?.invoke()
        assertThat(onNavigationCounter.get()).isEqualTo(1)
    }

    @Test
    fun `when google api key not supplied, condensed shipping address element is not one line address element`() = runTest {
        val addressElement = AddressElement(
            IdentifierSpec.Generic("address"),
            countryDropdownFieldController = countryDropdownFieldController,
            addressType = AddressType.ShippingCondensed(
                null,
                setOf(),
                PhoneNumberState.OPTIONAL
            ) { throw AssertionError("Not Expected") },
            sameAsShippingElement = null,
            shippingValuesMap = null
        )

        val identifierSpecs = addressElement.fields.first().map {
            it.identifier
        }
        assertThat(identifierSpecs.contains(IdentifierSpec.OneLineAddress)).isFalse()
    }

    @Test
    fun `expanded shipping address element should not have one line address element`() = runTest {
        val addressElement = AddressElement(
            IdentifierSpec.Generic("address"),
            countryDropdownFieldController = countryDropdownFieldController,
            addressType = AddressType.ShippingExpanded(
                googleApiKey = null,
                autocompleteCountries = null,
                phoneNumberState = PhoneNumberState.OPTIONAL,
            ) { throw AssertionError("Not Expected") },
            sameAsShippingElement = null,
            shippingValuesMap = null
        )

        val identifierSpecs = addressElement.fields.first().map {
            it.identifier
        }
        assertThat(identifierSpecs.contains(IdentifierSpec.OneLineAddress)).isFalse()
    }

    @Test
    fun `when same as shipping is enabled billing address is the same as shipping`() = runTest {
        val sameAsShippingElement = SameAsShippingElement(
            IdentifierSpec.SameAsShipping,
            SameAsShippingController(false)
        )
        val addressElement = AddressElement(
            IdentifierSpec.Generic("address"),
            mapOf(
                IdentifierSpec.Country to "CA"
            ),
            countryDropdownFieldController = countryDropdownFieldController,
            addressType = AddressType.Normal(),
            sameAsShippingElement = sameAsShippingElement,
            shippingValuesMap = mapOf(
                IdentifierSpec.Country to "US"
            )
        )

        val country = suspend {
            addressElement.fields
                .first()[0]
                .getFormFieldValueFlow()
                .first()[0].second.value
        }

        countryDropdownFieldController.onValueChange(1)

        assertThat(country()).isEqualTo("CA")

        sameAsShippingElement.setRawValue(mapOf(IdentifierSpec.SameAsShipping to "true"))

        assertThat(country()).isEqualTo("US")
    }

    @Test
    fun `when phone number is required, should not be complete until fully entered`() = runTest {
        val addressElement = AddressElement(
            IdentifierSpec.Generic("address"),
            mapOf(
                IdentifierSpec.Country to "CA"
            ),
            countryDropdownFieldController = countryDropdownFieldController,
            addressType = AddressType.Normal(
                phoneNumberState = PhoneNumberState.REQUIRED
            ),
            sameAsShippingElement = null,
            shippingValuesMap = null,
        )

        val phoneNumberController = addressElement.phoneNumberElement.controller

        phoneNumberController.onRawValueChange("123")
        assertThat(phoneNumberController.isComplete.value).isFalse()

        phoneNumberController.onRawValueChange("123456")
        assertThat(phoneNumberController.isComplete.value).isFalse()

        phoneNumberController.onRawValueChange("1234567890")
        assertThat(phoneNumberController.isComplete.value).isTrue()
    }

    private fun createAddressElement(initialValues: Map<IdentifierSpec, String>): AddressElement {
        return AddressElement(
            IdentifierSpec.Generic("address"),
            rawValuesMap = initialValues,
            countryDropdownFieldController = countryDropdownFieldController,
            addressType = AddressType.ShippingCondensed(
                googleApiKey = null,
                autocompleteCountries = setOf(),
                phoneNumberState = PhoneNumberState.OPTIONAL
            ) { throw AssertionError("Not Expected") },
            sameAsShippingElement = null,
            shippingValuesMap = null,
        )
    }
}

private suspend fun Flow<List<SectionFieldElement>>.postalCodeController(): TextFieldController {
    val fields = first()

    val element = fields.map { element ->
        if (element is RowElement) {
            element.fields
        } else {
            listOf(element)
        }
    }.flatten().find { element ->
        element.identifier == IdentifierSpec.PostalCode
    }

    return (element as SimpleTextElement).controller
}

private suspend fun AddressElement.trailingIconFor(
    identifierSpec: IdentifierSpec
): TextFieldIcon.Trailing? {
    val fieldForSpec = fields.first().first { it.identifier == identifierSpec }
    val controllerForSpec = (fieldForSpec as SimpleTextElement).controller
    val trailingIcon = (controllerForSpec as SimpleTextFieldController).textFieldConfig.trailingIcon
    return trailingIcon.value as? TextFieldIcon.Trailing?
}
