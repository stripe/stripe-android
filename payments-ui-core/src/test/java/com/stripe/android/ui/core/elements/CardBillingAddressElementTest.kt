package com.stripe.android.ui.core.elements

import app.cash.turbine.test
import com.google.common.truth.Truth
import com.google.common.truth.Truth.assertThat
import com.stripe.android.ui.core.BillingDetailsCollectionConfiguration
import com.stripe.android.uicore.address.FieldType
import com.stripe.android.uicore.elements.AddressElement
import com.stripe.android.uicore.elements.AutocompleteAddressElement
import com.stripe.android.uicore.elements.AutocompleteAddressInteractor
import com.stripe.android.uicore.elements.CountryConfig
import com.stripe.android.uicore.elements.DropdownFieldController
import com.stripe.android.uicore.elements.IdentifierSpec
import com.stripe.android.uicore.elements.RowElement
import com.stripe.android.uicore.elements.SectionFieldElement
import com.stripe.android.utils.isInstanceOf
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

private val ALL_ADDRESS_FIELDS: Set<IdentifierSpec> = FieldType.entries
    .filterNot { it == FieldType.Name }
    .map { it.identifierSpec }
    .toSet()

@RunWith(RobolectricTestRunner::class)
internal class CardBillingAddressElementTest {
    val dropdownFieldController = DropdownFieldController(
        CountryConfig(emptySet())
    )
    val cardBillingElement = createCardBillingAddressElement()

    @Test
    fun `Verify that when US is selected postal is not hidden`() = runTest {
        cardBillingElement.hiddenIdentifiers.test {
            dropdownFieldController.onRawValueChange("US")
            expectMostRecentItem().verifyFieldsShown(IdentifierSpec.PostalCode)
        }
    }

    @Test
    fun `Verify that when GB is selected postal is not hidden`() = runTest {
        cardBillingElement.hiddenIdentifiers.test {
            dropdownFieldController.onRawValueChange("GB")
            expectMostRecentItem().verifyFieldsShown(IdentifierSpec.PostalCode)
        }
    }

    @Test
    fun `Verify that when CA is selected postal is not hidden`() = runTest {
        cardBillingElement.hiddenIdentifiers.test {
            dropdownFieldController.onRawValueChange("CA")
            expectMostRecentItem().verifyFieldsShown(IdentifierSpec.PostalCode)
        }
    }

    @Test
    fun `Verify that when DE is selected postal IS hidden`() = runTest {
        cardBillingElement.hiddenIdentifiers.test {
            dropdownFieldController.onRawValueChange("DE")
            expectMostRecentItem().verifyFieldsShown()
        }
    }

    @Test
    fun `Verify that automatic tax fields are unioned with AVS defaults for IN`() = runTest {
        val element = createCardBillingAddressElement(requiresBillingAddressForAutomaticTax = true)

        element.hiddenIdentifiers.test {
            // IN has no AVS default fields, but requires a postal code for automatic tax.
            dropdownFieldController.onRawValueChange("IN")
            expectMostRecentItem().verifyFieldsShown(IdentifierSpec.PostalCode)
        }
    }

    @Test
    fun `Verify that automatic tax fields for PR do not require state`() = runTest {
        val element = createCardBillingAddressElement(requiresBillingAddressForAutomaticTax = true)

        element.hiddenIdentifiers.test {
            dropdownFieldController.onRawValueChange("PR")
            expectMostRecentItem().verifyFieldsShown(
                IdentifierSpec.Line1,
                IdentifierSpec.City,
                IdentifierSpec.PostalCode,
            )
        }
    }

    @Test
    fun `Verify that automatic tax fields are shown for US`() = runTest {
        val element = createCardBillingAddressElement(requiresBillingAddressForAutomaticTax = true)

        element.hiddenIdentifiers.test {
            dropdownFieldController.onRawValueChange("US")
            expectMostRecentItem().verifyFieldsShown(
                IdentifierSpec.Line1,
                IdentifierSpec.City,
                IdentifierSpec.State,
                IdentifierSpec.PostalCode,
            )
        }
    }

    @Test
    fun `Verify that automatic tax fields have no effect when address collection is Never`() = runTest {
        val element = createCardBillingAddressElement(
            requiresBillingAddressForAutomaticTax = true,
            collectionConfiguration = BillingDetailsCollectionConfiguration(
                address = BillingDetailsCollectionConfiguration.AddressCollectionMode.Never,
            ),
        )

        element.hiddenIdentifiers.test {
            dropdownFieldController.onRawValueChange("US")
            expectMostRecentItem().verifyFieldsShown()
        }
    }

    @Test
    fun `Verify that automatic tax fields have no effect when address collection is Full`() = runTest {
        val element = createCardBillingAddressElement(
            requiresBillingAddressForAutomaticTax = true,
            collectionConfiguration = BillingDetailsCollectionConfiguration(
                address = BillingDetailsCollectionConfiguration.AddressCollectionMode.Full,
            ),
        )

        element.hiddenIdentifiers.test {
            dropdownFieldController.onRawValueChange("US")
            assertThat(expectMostRecentItem()).isEmpty()
        }
    }

    @Test
    fun `Verify that AutocompleteAddressElement is used when billing details collection is Full`() =
        autocompleteTest(
            configuration = BillingDetailsCollectionConfiguration(
                address = BillingDetailsCollectionConfiguration.AddressCollectionMode.Full,
            )
        ) { cardBillingAddressElement ->
            assertThat(cardBillingAddressElement.addressElement)
                .isInstanceOf<AutocompleteAddressElement>()
        }

    @Test
    fun `Verify that AddressElement is used when billing details collection is Never`() = autocompleteTest(
        configuration = BillingDetailsCollectionConfiguration(
            address = BillingDetailsCollectionConfiguration.AddressCollectionMode.Never,
        )
    ) { cardBillingAddressElement ->
        assertThat(cardBillingAddressElement.addressElement)
            .isInstanceOf<AddressElement>()
    }

    @Test
    fun `Verify that AddressElement is used when billing details collection is Automatic`() = autocompleteTest(
        configuration = BillingDetailsCollectionConfiguration(
            address = BillingDetailsCollectionConfiguration.AddressCollectionMode.Automatic,
        )
    ) { cardBillingAddressElement ->
        assertThat(cardBillingAddressElement.addressElement)
            .isInstanceOf<AddressElement>()
    }

    @Test
    fun `Verify that email is used when email collection is required for non-autocomplete case`() =
        nonAutocompleteEmailAndPhoneTest(
            collectsEmail = true,
            collectsPhone = false,
        ) { hasEmail, hasPhone ->
            assertThat(hasEmail).isTrue()
            assertThat(hasPhone).isFalse()
        }

    @Test
    fun `Verify that phone is used when phone collection is required for non-autocomplete case`() =
        nonAutocompleteEmailAndPhoneTest(
            collectsEmail = false,
            collectsPhone = true,
        ) { hasEmail, hasPhone ->
            assertThat(hasEmail).isFalse()
            assertThat(hasPhone).isTrue()
        }

    @Test
    fun `Verify that email & phone is used when collection is required for non-autocomplete case`() =
        nonAutocompleteEmailAndPhoneTest(
            collectsEmail = true,
            collectsPhone = true,
        ) { hasEmail, hasPhone ->
            assertThat(hasEmail).isTrue()
            assertThat(hasPhone).isTrue()
        }

    @Test
    fun `Verify that email is used when email collection is required for autocomplete case`() =
        autocompleteEmailAndPhoneTest(
            collectsEmail = true,
            collectsPhone = false,
        ) { hasEmail, hasPhone ->
            assertThat(hasEmail).isTrue()
            assertThat(hasPhone).isFalse()
        }

    @Test
    fun `Verify that phone is used when phone collection is required for autocomplete case`() =
        autocompleteEmailAndPhoneTest(
            collectsEmail = false,
            collectsPhone = true,
        ) { hasEmail, hasPhone ->
            assertThat(hasEmail).isFalse()
            assertThat(hasPhone).isTrue()
        }

    @Test
    fun `Verify that email & phone is used when collection is required for autocomplete case`() =
        autocompleteEmailAndPhoneTest(
            collectsEmail = true,
            collectsPhone = true,
        ) { hasEmail, hasPhone ->
            assertThat(hasEmail).isTrue()
            assertThat(hasPhone).isTrue()
        }

    @Test
    fun `Verify that only errors from non-hidden fields are displayed`() = runTest {
        cardBillingElement.onValidationStateChanged(isValidating = true)

        cardBillingElement.sectionFieldErrorController().validationMessage.test {
            assertThat(awaitItem()).isNotNull()

            val postalCodeField = cardBillingElement
                .addressController
                .value
                .fieldsFlowable
                .value
                .findField(IdentifierSpec.PostalCode)

            assertThat(postalCodeField).isNotNull()

            val nonNullPostalCodeField = requireNotNull(postalCodeField)

            nonNullPostalCodeField.setRawValue(
                mapOf(
                    IdentifierSpec.PostalCode to "99999"
                )
            )

            assertThat(awaitItem()).isNull()
        }
    }

    private fun List<SectionFieldElement>.findField(identifierSpec: IdentifierSpec): SectionFieldElement? {
        for (element in this) {
            when (element) {
                is RowElement -> element.fields.findField(identifierSpec)?.let {
                    return it
                }
                else -> element.takeIf {
                    it.identifier == identifierSpec
                }?.let {
                    return it
                }
            }
        }

        return null
    }

    /**
     * Asserts which fields are shown to the customer - the complement of this hidden-identifiers
     * set - rather than which are hidden, since that's what a human reviewing a test failure
     * actually wants to check against the expected UX.
     */
    private fun Set<IdentifierSpec>.verifyFieldsShown(vararg shownFields: IdentifierSpec) {
        Truth.assertThat(ALL_ADDRESS_FIELDS - this).containsExactlyElementsIn(shownFields.toSet())
    }

    private fun createCardBillingAddressElement(
        requiresBillingAddressForAutomaticTax: Boolean = false,
        collectionConfiguration: BillingDetailsCollectionConfiguration = BillingDetailsCollectionConfiguration(),
    ): CardBillingAddressElement {
        return CardBillingAddressElement(
            identifier = IdentifierSpec.Generic("billing_element"),
            rawValuesMap = emptyMap(),
            countryCodes = emptySet(),
            countryDropdownFieldController = dropdownFieldController,
            autocompleteAddressInteractorFactory = null,
            sameAsShippingElement = null,
            shippingValuesMap = null,
            collectionConfiguration = collectionConfiguration,
            requiresBillingAddressForAutomaticTax = requiresBillingAddressForAutomaticTax,
        )
    }

    private fun nonAutocompleteEmailAndPhoneTest(
        collectsEmail: Boolean,
        collectsPhone: Boolean,
        block: (hasEmail: Boolean, hasPhone: Boolean) -> Unit,
    ) = autocompleteTest(
        configuration = BillingDetailsCollectionConfiguration(
            collectEmail = collectsEmail,
            collectPhone = collectsPhone,
            address = BillingDetailsCollectionConfiguration.AddressCollectionMode.Automatic,
        )
    ) { cardBillingAddressElement ->
        val addressController = cardBillingAddressElement.addressController.value

        val addressFields = addressController.fieldsFlowable.value

        val hasEmail = addressFields.any { field ->
            field.identifier == IdentifierSpec.Email
        }

        val hasPhone = addressFields.any { field ->
            field.identifier == IdentifierSpec.Phone
        }

        block(hasEmail, hasPhone)
    }

    private fun autocompleteEmailAndPhoneTest(
        collectsEmail: Boolean,
        collectsPhone: Boolean,
        block: (hasEmail: Boolean, hasPhone: Boolean) -> Unit,
    ) = autocompleteTest(
        configuration = BillingDetailsCollectionConfiguration(
            collectEmail = collectsEmail,
            collectPhone = collectsPhone,
            address = BillingDetailsCollectionConfiguration.AddressCollectionMode.Full,
        )
    ) { cardBillingAddressElement ->
        val addressController = cardBillingAddressElement.addressController.value

        val addressFields = addressController.fieldsFlowable.value

        val hasEmail = addressFields.any { field ->
            field.identifier == IdentifierSpec.Email
        }

        val hasPhone = addressFields.any { field ->
            field.identifier == IdentifierSpec.Phone
        }

        block(hasEmail, hasPhone)
    }

    private fun autocompleteTest(
        configuration: BillingDetailsCollectionConfiguration,
        block: (CardBillingAddressElement) -> Unit,
    ) = runTest {
        block(
            CardBillingAddressElement(
                identifier = IdentifierSpec.Generic("billing_element"),
                rawValuesMap = emptyMap(),
                countryCodes = emptySet(),
                countryDropdownFieldController = dropdownFieldController,
                autocompleteAddressInteractorFactory = {
                    object : AutocompleteAddressInteractor {
                        override val autocompleteConfig: AutocompleteAddressInteractor.Config =
                            AutocompleteAddressInteractor.Config(
                                googlePlacesApiKey = null,
                                autocompleteCountries = emptySet()
                            )

                        override fun register(onEvent: (AutocompleteAddressInteractor.Event) -> Unit) {
                            // No-op
                        }

                        override fun onAutocomplete(country: String) {
                            error("Should not be called!")
                        }
                    }
                },
                sameAsShippingElement = null,
                shippingValuesMap = null,
                collectionConfiguration = configuration,
            )
        )
    }
}
