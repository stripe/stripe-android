package com.stripe.android.uicore.elements

import app.cash.turbine.ReceiveTurbine
import app.cash.turbine.Turbine
import app.cash.turbine.TurbineTestContext
import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import com.stripe.android.uicore.R
import com.stripe.android.uicore.forms.FormFieldEntry
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Test

class AutocompleteAddressControllerTest {
    @Test
    fun `Phone number field is hidden when state is HIDDEN`() = elementsTest(
        phoneNumberConfig = AddressFieldConfiguration.HIDDEN,
        sameAsShippingElement = null,
        shippingValuesMap = emptyMap(),
    ) { elements ->
        assertThat(elements.filterIsInstance<PhoneNumberElement>()).isEmpty()
    }

    @Test
    fun `Phone number field is shown when state is OPTIONAL`() = elementsTest(
        phoneNumberConfig = AddressFieldConfiguration.OPTIONAL,
    ) { elements ->
        assertThat(elements.filterIsInstance<PhoneNumberElement>()).hasSize(1)
    }

    @Test
    fun `Phone number field is shown when state is REQUIRED`() = elementsTest(
        phoneNumberConfig = AddressFieldConfiguration.REQUIRED,
    ) { elements ->
        assertThat(elements.filterIsInstance<PhoneNumberElement>()).hasSize(1)
    }

    @Test
    fun `Name field is hidden when state is HIDDEN`() = elementsTest(
        nameConfig = AddressFieldConfiguration.HIDDEN
    ) { elements ->
        assertThat(
            elements.any { it.identifier == IdentifierSpec.Name }
        ).isFalse()
    }

    @Test
    fun `Name field is shown when state is OPTIONAL`() = elementsTest(
        nameConfig = AddressFieldConfiguration.OPTIONAL
    ) { elements ->
        assertThat(
            elements.any { it.identifier == IdentifierSpec.Name }
        ).isTrue()
    }

    @Test
    fun `Name should be shown when state is REQUIRED`() = elementsTest(
        nameConfig = AddressFieldConfiguration.REQUIRED
    ) { elements ->
        assertThat(
            elements.any { it.identifier == IdentifierSpec.Name }
        ).isTrue()
    }

    @Test
    fun `Email field is hidden when state is HIDDEN`() = elementsTest(
        emailConfig = AddressFieldConfiguration.HIDDEN
    ) { elements ->
        assertThat(elements.filterIsInstance<EmailElement>()).isEmpty()
    }

    @Test
    fun `Email field is shown when state is OPTIONAL`() = elementsTest(
        emailConfig = AddressFieldConfiguration.OPTIONAL
    ) { elements ->
        assertThat(elements.filterIsInstance<EmailElement>()).hasSize(1)
    }

    @Test
    fun `Email should be shown when state is REQUIRED`() = elementsTest(
        emailConfig = AddressFieldConfiguration.REQUIRED
    ) { elements ->
        assertThat(elements.filterIsInstance<EmailElement>()).hasSize(1)
    }

    @Test
    fun `Country should be hidden if 'hideCountry' is set to true`() = elementsTest(
        hideCountry = true
    ) { elements ->
        assertThat(elements.filterIsInstance<CountryElement>()).isEmpty()
    }

    @Test
    fun `Country should be shown if 'hideCountry' is set to false`() = elementsTest(
        hideCountry = false
    ) { fields ->
        assertThat(fields.filterIsInstance<CountryElement>()).hasSize(1)
    }

    @Test
    fun `Same as shipping element & shipping values are respected when provided`() {
        val shippingValuesMap = mapOf(
            IdentifierSpec.Line1 to "123 Main Street",
            IdentifierSpec.Line2 to "456",
            IdentifierSpec.City to "San Francisco",
            IdentifierSpec.State to "CA",
            IdentifierSpec.Country to "US",
            IdentifierSpec.PostalCode to "94111",
        )
        val element = createSameAsShippingElement()

        formFieldsTest(
            sameAsShippingElement = element,
            shippingValuesMap = shippingValuesMap,
        ) {
            assertThat(awaitItem()).containsExactlyElementsIn(
                listOf(
                    IdentifierSpec.Line1 to FormFieldEntry(value = "", isComplete = false),
                    IdentifierSpec.Line2 to FormFieldEntry(value = "", isComplete = true),
                    IdentifierSpec.City to FormFieldEntry(value = "", isComplete = false),
                    IdentifierSpec.State to FormFieldEntry(value = null, isComplete = false),
                    IdentifierSpec.Country to FormFieldEntry(value = "US", isComplete = true),
                    IdentifierSpec.PostalCode to FormFieldEntry(value = "", isComplete = false),
                )
            )

            element.controller.onValueChange(value = true)

            assertThat(awaitItem()).containsAtLeastElementsIn(
                listOf(
                    IdentifierSpec.Line1 to FormFieldEntry(value = "123 Main Street", isComplete = true),
                    IdentifierSpec.Line2 to FormFieldEntry(value = "456", isComplete = true),
                    IdentifierSpec.City to FormFieldEntry(value = "", isComplete = false),
                    IdentifierSpec.State to FormFieldEntry(value = "CA", isComplete = true),
                    IdentifierSpec.Country to FormFieldEntry(value = "US", isComplete = true),
                    IdentifierSpec.PostalCode to FormFieldEntry(value = "", isComplete = false),
                )
            )

            assertThat(awaitItem()).containsAtLeastElementsIn(
                listOf(
                    IdentifierSpec.Line1 to FormFieldEntry(value = "123 Main Street", isComplete = true),
                    IdentifierSpec.Line2 to FormFieldEntry(value = "456", isComplete = true),
                    IdentifierSpec.City to FormFieldEntry(value = "San Francisco", isComplete = true),
                    IdentifierSpec.State to FormFieldEntry(value = "CA", isComplete = true),
                    IdentifierSpec.Country to FormFieldEntry(value = "US", isComplete = true),
                    IdentifierSpec.PostalCode to FormFieldEntry(value = "94111", isComplete = true),
                )
            )

            ensureAllEventsConsumed()
        }
    }

    @Test
    fun `Element does not use autocomplete if no google places API key is provided`() = noAutocompleteTest(
        autocompleteConfig = AutocompleteAddressInteractor.Config(
            googlePlacesApiKey = null,
            autocompleteCountries = setOf("US"),
            isPlacesAvailable = true,
        ),
    )

    @Test
    fun `Element does not use autocomplete if Places is not available`() = noAutocompleteTest(
        autocompleteConfig = AutocompleteAddressInteractor.Config(
            googlePlacesApiKey = "123",
            autocompleteCountries = setOf("US"),
            isPlacesAvailable = false,
        ),
    )

    @Test
    fun `Element does not use autocomplete if autocomplete country not supported`() = noAutocompleteTest(
        autocompleteConfig = AutocompleteAddressInteractor.Config(
            googlePlacesApiKey = "123",
            autocompleteCountries = emptySet(),
            isPlacesAvailable = true,
        ),
    )

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun `Should expand form if expand form event is provided`() = runTest(UnconfinedTestDispatcher()) {
        TestAutocompleteAddressInteractor.test(
            autocompleteConfig = AutocompleteAddressInteractor.Config(
                googlePlacesApiKey = "123",
                autocompleteCountries = setOf("US"),
                isPlacesAvailable = true,
            )
        ) {
            val controller = createAutocompleteAddressController(
                interactor = interactor,
            )

            val registerCall = registerCalls.awaitItem()

            controller.addressElementFlow.test {
                val firstElements = awaitItem().fields.value

                assertThat(
                    firstElements.any { field ->
                        field.identifier == IdentifierSpec.Line1
                    }
                ).isFalse()

                assertThat(
                    firstElements.any { field ->
                        field.identifier == IdentifierSpec.Line2
                    }
                ).isFalse()

                assertThat(
                    firstElements.any { field ->
                        field is AddressTextFieldElement
                    }
                ).isTrue()

                registerCall.onEvent(
                    AutocompleteAddressInteractor.Event.OnExpandForm(
                        values = emptyMap()
                    )
                )

                val secondElements = awaitItem().fields.value

                assertThat(
                    secondElements.any { field ->
                        field.identifier == IdentifierSpec.Line1
                    }
                ).isTrue()

                assertThat(
                    secondElements.any { field ->
                        field.identifier == IdentifierSpec.Line2
                    }
                ).isTrue()

                assertThat(
                    secondElements.any { field ->
                        field is AddressTextFieldElement
                    }
                ).isFalse()
            }
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun `Should updates values on autocomplete values event with values`() = runTest(UnconfinedTestDispatcher()) {
        TestAutocompleteAddressInteractor.test(
            autocompleteConfig = AutocompleteAddressInteractor.Config(
                googlePlacesApiKey = "123",
                autocompleteCountries = setOf("US")
            )
        ) {
            val controller = createAutocompleteAddressController(
                interactor = interactor,
                values = mapOf(
                    IdentifierSpec.Line1 to "123",
                )
            )

            val registerCall = registerCalls.awaitItem()

            controller.formFieldValues.test {
                assertThat(awaitItem()).containsExactly(
                    IdentifierSpec.Line1 to FormFieldEntry(value = "123", isComplete = true),
                    IdentifierSpec.Line2 to FormFieldEntry(value = "", isComplete = true),
                    IdentifierSpec.City to FormFieldEntry(value = "", isComplete = false),
                    IdentifierSpec.State to FormFieldEntry(value = null, isComplete = false),
                    IdentifierSpec.Country to FormFieldEntry(value = "US", isComplete = true),
                    IdentifierSpec.PostalCode to FormFieldEntry(value = "", isComplete = false),
                )

                registerCall.onEvent(
                    AutocompleteAddressInteractor.Event.OnValues(
                        values = mapOf(
                            IdentifierSpec.Line1 to "123 Main Street",
                            IdentifierSpec.Line2 to "456",
                            IdentifierSpec.City to "San Francisco",
                            IdentifierSpec.State to "CA",
                            IdentifierSpec.Country to "US",
                            IdentifierSpec.PostalCode to "94111",
                        )
                    )
                )

                assertThat(awaitItem()).containsExactly(
                    IdentifierSpec.Line1 to FormFieldEntry(value = "123 Main Street", isComplete = true),
                    IdentifierSpec.Line2 to FormFieldEntry(value = "456", isComplete = true),
                    IdentifierSpec.City to FormFieldEntry(value = "San Francisco", isComplete = true),
                    IdentifierSpec.State to FormFieldEntry(value = "CA", isComplete = true),
                    IdentifierSpec.Country to FormFieldEntry(value = "US", isComplete = true),
                    IdentifierSpec.PostalCode to FormFieldEntry(value = "94111", isComplete = true),
                )
            }
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun `Should updates values on expand event if it has values`() = runTest(UnconfinedTestDispatcher()) {
        TestAutocompleteAddressInteractor.test(
            autocompleteConfig = AutocompleteAddressInteractor.Config(
                googlePlacesApiKey = "123",
                autocompleteCountries = setOf("US")
            )
        ) {
            val controller = createAutocompleteAddressController(
                interactor = interactor,
                values = mapOf(
                    IdentifierSpec.Line1 to "123",
                )
            )

            val registerCall = registerCalls.awaitItem()

            controller.formFieldValues.test {
                assertThat(awaitItem()).containsExactly(
                    IdentifierSpec.Line1 to FormFieldEntry(value = "123", isComplete = true),
                    IdentifierSpec.Line2 to FormFieldEntry(value = "", isComplete = true),
                    IdentifierSpec.City to FormFieldEntry(value = "", isComplete = false),
                    IdentifierSpec.State to FormFieldEntry(value = null, isComplete = false),
                    IdentifierSpec.Country to FormFieldEntry(value = "US", isComplete = true),
                    IdentifierSpec.PostalCode to FormFieldEntry(value = "", isComplete = false),
                )

                registerCall.onEvent(
                    AutocompleteAddressInteractor.Event.OnValues(
                        values = mapOf(
                            IdentifierSpec.Line1 to "123 Main Street",
                        )
                    )
                )

                assertThat(awaitItem()).containsExactly(
                    IdentifierSpec.Line1 to FormFieldEntry(value = "123 Main Street", isComplete = true),
                    IdentifierSpec.Line2 to FormFieldEntry(value = "", isComplete = true),
                    IdentifierSpec.City to FormFieldEntry(value = "", isComplete = false),
                    IdentifierSpec.State to FormFieldEntry(value = null, isComplete = false),
                    IdentifierSpec.Country to FormFieldEntry(value = "US", isComplete = true),
                    IdentifierSpec.PostalCode to FormFieldEntry(value = "", isComplete = false),
                )
            }
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun `Should merge with existing values on autocomplete event`() = runTest(UnconfinedTestDispatcher()) {
        TestAutocompleteAddressInteractor.test(DEFAULT_AUTOCOMPLETE_CONFIG) {
            val controller = createAutocompleteAddressController(
                interactor = interactor,
                nameConfig = AddressFieldConfiguration.REQUIRED,
                phoneNumberConfig = AddressFieldConfiguration.REQUIRED,
                emailConfig = AddressFieldConfiguration.REQUIRED,
                values = mapOf(
                    IdentifierSpec.Name to "John Doe",
                    IdentifierSpec.Email to "email@email.com",
                    IdentifierSpec.Phone to "+11234567890",
                    IdentifierSpec.Line1 to "123",
                )
            )

            val registerCall = registerCalls.awaitItem()

            controller.formFieldValues.test {
                assertThat(awaitItem()).containsExactly(
                    IdentifierSpec.Name to FormFieldEntry(value = "John Doe", isComplete = true),
                    IdentifierSpec.Email to FormFieldEntry(value = "email@email.com", isComplete = true),
                    IdentifierSpec.PhoneNumberCountry to FormFieldEntry(value = "US", isComplete = true),
                    IdentifierSpec.Phone to FormFieldEntry(value = "+11234567890", isComplete = true),
                    IdentifierSpec.Line1 to FormFieldEntry(value = "123", isComplete = true),
                    IdentifierSpec.Line2 to FormFieldEntry(value = "", isComplete = true),
                    IdentifierSpec.City to FormFieldEntry(value = "", isComplete = false),
                    IdentifierSpec.State to FormFieldEntry(value = null, isComplete = false),
                    IdentifierSpec.Country to FormFieldEntry(value = "US", isComplete = true),
                    IdentifierSpec.PostalCode to FormFieldEntry(value = "", isComplete = false),
                )

                registerCall.onEvent(
                    AutocompleteAddressInteractor.Event.OnValues(
                        values = mapOf(
                            IdentifierSpec.Line1 to "123 Main Street",
                            IdentifierSpec.Line2 to "456",
                            IdentifierSpec.City to "San Francisco",
                            IdentifierSpec.State to "CA",
                            IdentifierSpec.Country to "US",
                            IdentifierSpec.PostalCode to "94111",
                        )
                    )
                )

                assertThat(awaitItem()).containsExactly(
                    IdentifierSpec.Name to FormFieldEntry(value = "John Doe", isComplete = true),
                    IdentifierSpec.Email to FormFieldEntry(value = "email@email.com", isComplete = true),
                    IdentifierSpec.PhoneNumberCountry to FormFieldEntry(value = "US", isComplete = true),
                    IdentifierSpec.Phone to FormFieldEntry(value = "+11234567890", isComplete = true),
                    IdentifierSpec.Line1 to FormFieldEntry(value = "123 Main Street", isComplete = true),
                    IdentifierSpec.Line2 to FormFieldEntry(value = "456", isComplete = true),
                    IdentifierSpec.City to FormFieldEntry(value = "San Francisco", isComplete = true),
                    IdentifierSpec.State to FormFieldEntry(value = "CA", isComplete = true),
                    IdentifierSpec.Country to FormFieldEntry(value = "US", isComplete = true),
                    IdentifierSpec.PostalCode to FormFieldEntry(value = "94111", isComplete = true),
                )
            }
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun `Should override filled values with null when key provided in event`() = runTest(UnconfinedTestDispatcher()) {
        TestAutocompleteAddressInteractor.test(DEFAULT_AUTOCOMPLETE_CONFIG) {
            val controller = createAutocompleteAddressController(
                interactor = interactor,
                values = mapOf(
                    IdentifierSpec.Line1 to "123 Main Street",
                    IdentifierSpec.Line2 to "456",
                    IdentifierSpec.City to "San Francisco",
                    IdentifierSpec.State to "CA",
                    IdentifierSpec.Country to "US",
                    IdentifierSpec.PostalCode to "94111",
                )
            )

            val registerCall = registerCalls.awaitItem()

            controller.formFieldValues.test {
                assertThat(awaitItem()).containsExactly(
                    IdentifierSpec.Line1 to FormFieldEntry(value = "123 Main Street", isComplete = true),
                    IdentifierSpec.Line2 to FormFieldEntry(value = "456", isComplete = true),
                    IdentifierSpec.City to FormFieldEntry(value = "San Francisco", isComplete = true),
                    IdentifierSpec.State to FormFieldEntry(value = "CA", isComplete = true),
                    IdentifierSpec.Country to FormFieldEntry(value = "US", isComplete = true),
                    IdentifierSpec.PostalCode to FormFieldEntry(value = "94111", isComplete = true),
                )

                registerCall.onEvent(
                    AutocompleteAddressInteractor.Event.OnValues(
                        values = mapOf(
                            IdentifierSpec.Line1 to "123 Main Street",
                            IdentifierSpec.Line2 to null,
                            IdentifierSpec.City to null,
                            IdentifierSpec.State to "CA",
                            IdentifierSpec.Country to "US",
                            IdentifierSpec.PostalCode to "94111",
                        )
                    )
                )

                assertThat(awaitItem()).containsExactly(
                    IdentifierSpec.Line1 to FormFieldEntry(value = "123 Main Street", isComplete = true),
                    IdentifierSpec.Line2 to FormFieldEntry(value = "", isComplete = true),
                    IdentifierSpec.City to FormFieldEntry(value = "", isComplete = false),
                    IdentifierSpec.State to FormFieldEntry(value = "CA", isComplete = true),
                    IdentifierSpec.Country to FormFieldEntry(value = "US", isComplete = true),
                    IdentifierSpec.PostalCode to FormFieldEntry(value = "94111", isComplete = true),
                )
            }
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun `On autocomplete event should update country`() = runTest(UnconfinedTestDispatcher()) {
        TestAutocompleteAddressInteractor.test(
            autocompleteConfig = AutocompleteAddressInteractor.Config(
                googlePlacesApiKey = "123",
                autocompleteCountries = setOf("US")
            )
        ) {
            val controller = createAutocompleteAddressController(
                interactor = interactor,
                values = mapOf(
                    IdentifierSpec.Line1 to "123",
                )
            )

            val registerCall = registerCalls.awaitItem()

            controller.formFieldValues.test {
                assertThat(awaitItem()).containsExactly(
                    IdentifierSpec.Line1 to FormFieldEntry(value = "123", isComplete = true),
                    IdentifierSpec.Line2 to FormFieldEntry(value = "", isComplete = true),
                    IdentifierSpec.City to FormFieldEntry(value = "", isComplete = false),
                    IdentifierSpec.State to FormFieldEntry(value = null, isComplete = false),
                    IdentifierSpec.Country to FormFieldEntry(value = "US", isComplete = true),
                    IdentifierSpec.PostalCode to FormFieldEntry(value = "", isComplete = false),
                )

                registerCall.onEvent(
                    AutocompleteAddressInteractor.Event.OnValues(
                        values = mapOf(
                            IdentifierSpec.Line1 to "123 Main Street",
                            IdentifierSpec.Country to "CA"
                        )
                    )
                )

                assertThat(expectMostRecentItem()).containsExactly(
                    IdentifierSpec.Line1 to FormFieldEntry(value = "123 Main Street", isComplete = true),
                    IdentifierSpec.Line2 to FormFieldEntry(value = "", isComplete = true),
                    IdentifierSpec.City to FormFieldEntry(value = "", isComplete = false),
                    IdentifierSpec.State to FormFieldEntry(value = null, isComplete = false),
                    IdentifierSpec.Country to FormFieldEntry(value = "CA", isComplete = true),
                    IdentifierSpec.PostalCode to FormFieldEntry(value = "", isComplete = false),
                )
            }
        }
    }

    @Test
    fun `On values has line 1, should be expanded form`() = elementsTest(
        values = mapOf(
            IdentifierSpec.Line1 to "123 Apple Street"
        ),
        autocompleteConfig = AutocompleteAddressInteractor.Config(
            googlePlacesApiKey = "123",
            autocompleteCountries = setOf("US"),
            isPlacesAvailable = true,
        ),
    ) { elements ->
        val containsLineOne = elements.any { element ->
            element.identifier == IdentifierSpec.Line1
        }

        val containsLineTwo = elements.any { element ->
            element.identifier == IdentifierSpec.Line2
        }

        assertThat(containsLineOne).isTrue()
        assertThat(containsLineTwo).isTrue()
    }

    @Test
    fun `Element contains condensed elements & navigates on autocomplete click`() = elementsTest(
        values = emptyMap(),
        autocompleteConfig = AutocompleteAddressInteractor.Config(
            googlePlacesApiKey = "123",
            autocompleteCountries = setOf("US"),
            isPlacesAvailable = true,
        ),
    ) { elements ->
        val filteredForAddressTextFieldElement = elements.filterIsInstance<AddressTextFieldElement>()

        assertThat(filteredForAddressTextFieldElement).hasSize(1)

        val autocompleteTextElement = filteredForAddressTextFieldElement[0]

        autocompleteTextElement.controller.launchAutocompleteScreen()

        assertThat(onAutocompleteCalls.awaitItem().country).isEqualTo("US")
    }

    @Test
    fun `Element contains expanded elements & navigates on autocomplete click`() = elementsTest(
        values = mapOf(
            IdentifierSpec.Line1 to "123 Apple Street"
        ),
        autocompleteConfig = AutocompleteAddressInteractor.Config(
            googlePlacesApiKey = "123",
            autocompleteCountries = setOf("US"),
            isPlacesAvailable = true,
        ),
    ) { elements ->
        val element = elements.firstOrNull { element ->
            element.identifier == IdentifierSpec.Line1
        }

        assertThat(element).isNotNull()
        assertThat(element).isInstanceOf(SimpleTextElement::class.java)

        val textElement = element as SimpleTextElement

        assertThat(textElement.controller).isInstanceOf(SimpleTextFieldController::class.java)

        textElement.controller.trailingIcon.test {
            val textFieldIcon = awaitItem()

            assertThat(textFieldIcon).isNotNull()
            assertThat(textFieldIcon).isInstanceOf(TextFieldIcon.Trailing::class.java)

            val trailingIcon = textFieldIcon as TextFieldIcon.Trailing

            assertThat(trailingIcon.idRes).isEqualTo(R.drawable.stripe_ic_search)

            trailingIcon.onClick?.invoke()

            cancelAndIgnoreRemainingEvents()
        }

        assertThat(onAutocompleteCalls.awaitItem().country).isEqualTo("US")
    }

    @Test
    fun `Should register on creation`() = runTest {
        TestAutocompleteAddressInteractor.test(
            autocompleteConfig = AutocompleteAddressInteractor.Config(
                googlePlacesApiKey = null,
                autocompleteCountries = emptySet()
            )
        ) {
            createAutocompleteAddressController(
                interactor = interactor,
            )

            assertThat(registerCalls.awaitItem()).isNotNull()
        }
    }

    @Test
    fun `Errors should be pulled from internal address element`() = runTest {
        val controller = createAutocompleteAddressController(
            phoneNumberConfig = AddressFieldConfiguration.REQUIRED,
            interactor = TestAutocompleteAddressInteractor.noOp(
                autocompleteConfig = AutocompleteAddressInteractor.Config(
                    googlePlacesApiKey = null,
                    autocompleteCountries = emptySet(),
                    isPlacesAvailable = false,
                )
            ),
            values = mapOf(
                IdentifierSpec.Country to "US",
                IdentifierSpec.PostalCode to "999",
                IdentifierSpec.Phone to "+1222"
            )
        )

        controller.error.test {
            val error = awaitItem()

            assertThat(error?.errorMessage).isEqualTo(R.string.stripe_address_zip_incomplete)

            controller.addressElementFlow.test {
                val addressElement = awaitItem()

                addressElement.fields.test {
                    val fields = awaitItem()

                    val rowElements = fields.filterIsInstance<RowElement>()

                    assertThat(rowElements).hasSize(1)

                    val zipCodeElement = rowElements[0].fields.find { element ->
                        element.identifier == IdentifierSpec.PostalCode
                    }

                    assertThat(zipCodeElement).isNotNull()

                    requireNotNull(zipCodeElement).setRawValue(mapOf(IdentifierSpec.PostalCode to "99999"))
                }
            }

            val nextError = expectMostRecentItem()

            assertThat(nextError?.errorMessage).isEqualTo(R.string.stripe_incomplete_phone_number)
        }
    }

    @Test
    fun `on condensed to expanded form, should change address controllers`() = runTest {
        TestAutocompleteAddressInteractor.test(
            autocompleteConfig = AutocompleteAddressInteractor.Config(
                googlePlacesApiKey = "123",
                autocompleteCountries = setOf("US"),
                isPlacesAvailable = true,
            ),
        ) {
            val controller = createAutocompleteAddressController(interactor = interactor)

            val registerCall = registerCalls.awaitItem()

            controller.addressController.test {
                val firstAddressController = awaitItem()

                registerCall.onEvent(
                    AutocompleteAddressInteractor.Event.OnValues(
                        values = mapOf(
                            IdentifierSpec.Line1 to "123 Main Street",
                            IdentifierSpec.Line2 to "456",
                            IdentifierSpec.City to "San Francisco",
                            IdentifierSpec.State to "CA",
                            IdentifierSpec.Country to "US",
                            IdentifierSpec.PostalCode to "94111",
                        )
                    )
                )

                val secondAddressController = awaitItem()

                assertThat(firstAddressController).isNotEqualTo(secondAddressController)
            }
        }
    }

    @Test
    fun `on validating, should update all internal fields of validation state`() =
        fieldsTest { controller ->
            val fields = awaitItem()

            fields.element(IdentifierSpec.Country).errorTest(fieldError = null)
            fields.element(IdentifierSpec.Line1).errorTest(fieldError = null)
            fields.element(IdentifierSpec.Line2).errorTest(fieldError = null)
            fields.element(IdentifierSpec.State).errorTest(fieldError = null)
            fields.element(IdentifierSpec.PostalCode).errorTest(fieldError = null)
            fields.element(IdentifierSpec.City).errorTest(fieldError = null)

            controller.onValidationStateChanged(true)

            fields.element(IdentifierSpec.Country).errorTest(fieldError = null)
            fields.element(IdentifierSpec.Line1)
                .errorTest(fieldError = FieldError(R.string.stripe_blank_and_required))
            fields.element(IdentifierSpec.Line2).errorTest(fieldError = null)
            fields.element(IdentifierSpec.State)
                .errorTest(fieldError = FieldError(R.string.stripe_blank_and_required))
            fields.element(IdentifierSpec.PostalCode)
                .errorTest(fieldError = FieldError(R.string.stripe_blank_and_required))
            fields.element(IdentifierSpec.City)
                .errorTest(fieldError = FieldError(R.string.stripe_blank_and_required))
        }

    private fun noAutocompleteTest(
        autocompleteConfig: AutocompleteAddressInteractor.Config,
    ) = elementsTest(
        autocompleteConfig = autocompleteConfig,
    ) { fields ->
        assertThat(fields.filterIsInstance<AutocompleteAddressElement>()).isEmpty()

        val field = fields.firstOrNull { field ->
            field.identifier == IdentifierSpec.Line1
        }

        assertThat(field).isNotNull()
        assertThat(field).isInstanceOf(SimpleTextElement::class.java)

        val textElement = field as SimpleTextElement

        assertThat(textElement.controller).isInstanceOf(SimpleTextFieldController::class.java)

        textElement.controller.trailingIcon.test {
            assertThat(awaitItem()).isNull()
        }
    }

    private fun elementsTest(
        values: Map<IdentifierSpec, String?> = emptyMap(),
        nameConfig: AddressFieldConfiguration = AddressFieldConfiguration.HIDDEN,
        phoneNumberConfig: AddressFieldConfiguration = AddressFieldConfiguration.HIDDEN,
        emailConfig: AddressFieldConfiguration = AddressFieldConfiguration.HIDDEN,
        sameAsShippingElement: SameAsShippingElement? = null,
        shippingValuesMap: Map<IdentifierSpec, String?> = emptyMap(),
        autocompleteConfig: AutocompleteAddressInteractor.Config = AutocompleteAddressInteractor.Config(
            autocompleteCountries = setOf("AT", "BE", "DE", "ES", "IT", "NL"),
            googlePlacesApiKey = null,
        ),
        eventToEmit: AutocompleteAddressInteractor.Event? = null,
        hideCountry: Boolean = false,
        test: suspend TestAutocompleteAddressInteractor.Scenario.(elements: List<SectionFieldElement>) -> Unit
    ) = runTest {
        TestAutocompleteAddressInteractor.test(
            autocompleteConfig = autocompleteConfig,
        ) {
            val controller = createAutocompleteAddressController(
                values = values,
                nameConfig = nameConfig,
                phoneNumberConfig = phoneNumberConfig,
                emailConfig = emailConfig,
                sameAsShippingElement = sameAsShippingElement,
                shippingValuesMap = shippingValuesMap,
                interactor = interactor,
                hideCountry = hideCountry,
            )

            val registerCall = registerCalls.awaitItem()

            eventToEmit?.let {
                registerCall.onEvent(it)
            }

            controller.addressElementFlow.test {
                val element = awaitItem()

                element.fields.test {
                    test(awaitItem())
                }
            }
        }
    }

    private fun formFieldsTest(
        phoneNumberConfig: AddressFieldConfiguration = AddressFieldConfiguration.HIDDEN,
        sameAsShippingElement: SameAsShippingElement? = null,
        shippingValuesMap: Map<IdentifierSpec, String?> = emptyMap(),
        test: suspend TurbineTestContext<List<Pair<IdentifierSpec, FormFieldEntry>>>.() -> Unit
    ) = runTest {
        val controller = createAutocompleteAddressController(
            phoneNumberConfig = phoneNumberConfig,
            sameAsShippingElement = sameAsShippingElement,
            shippingValuesMap = shippingValuesMap,
        )

        controller.addressElementFlow.test {
            val element = awaitItem()

            element.getFormFieldValueFlow().test {
                test(this)
            }
        }
    }

    private fun createAutocompleteAddressController(
        values: Map<IdentifierSpec, String?> = emptyMap(),
        phoneNumberConfig: AddressFieldConfiguration = AddressFieldConfiguration.HIDDEN,
        nameConfig: AddressFieldConfiguration = AddressFieldConfiguration.HIDDEN,
        emailConfig: AddressFieldConfiguration = AddressFieldConfiguration.HIDDEN,
        sameAsShippingElement: SameAsShippingElement? = null,
        shippingValuesMap: Map<IdentifierSpec, String?> = emptyMap(),
        autocompleteConfig: AutocompleteAddressInteractor.Config = AutocompleteAddressInteractor.Config(
            autocompleteCountries = setOf("AT", "BE", "DE", "ES", "IT", "NL"),
            googlePlacesApiKey = null,
        ),
        hideCountry: Boolean = false,
        interactor: AutocompleteAddressInteractor =
            TestAutocompleteAddressInteractor.noOp(
                autocompleteConfig = autocompleteConfig,
            ),
    ): AutocompleteAddressController {
        return AutocompleteAddressController(
            identifier = IdentifierSpec.Generic("address"),
            initialValues = values,
            sameAsShippingElement = sameAsShippingElement,
            shippingValuesMap = shippingValuesMap,
            phoneNumberConfig = phoneNumberConfig,
            emailConfig = emailConfig,
            nameConfig = nameConfig,
            hideCountry = hideCountry,
            interactorFactory = { interactor },
        )
    }

    private fun createSameAsShippingElement(): SameAsShippingElement {
        return SameAsShippingElement(
            identifier = IdentifierSpec.SameAsShipping,
            controller = SameAsShippingController(initialValue = false)
        )
    }

    private fun List<SectionFieldElement>.element(identifierSpec: IdentifierSpec): SectionFieldElement {
        val element = nullableElement(identifierSpec)

        assertThat(element).isNotNull()

        return requireNotNull(element)
    }

    private fun List<SectionFieldElement>.nullableElement(identifierSpec: IdentifierSpec): SectionFieldElement? {
        for (element in this) {
            if (element is RowElement) {
                element.fields.nullableElement(identifierSpec)?.let {
                    return it
                }
            } else if (element.identifier == identifierSpec) {
                return element
            }
        }

        return null
    }

    private suspend fun SectionFieldElement.errorTest(fieldError: FieldError?) {
        sectionFieldErrorController().error.test {
            fieldError?.let {
                val error = awaitItem()

                assertThat(error?.errorMessage).isEqualTo(it.errorMessage)
                assertThat(error?.formatArgs).isEqualTo(it.formatArgs)
            } ?: run {
                assertThat(awaitItem()).isNull()
            }
        }
    }

    private fun fieldsTest(
        block: suspend TurbineTestContext<List<SectionFieldElement>>.(controller: AutocompleteAddressController) -> Unit
    ) = runTest {
        val controller = createAutocompleteAddressController()

        controller.addressElementFlow.test {
            val addressElement = awaitItem()

            addressElement.fields.test {
                block(this, controller)
            }
        }
    }

    private class TestAutocompleteAddressInteractor private constructor(
        override val autocompleteConfig: AutocompleteAddressInteractor.Config,
    ) : AutocompleteAddressInteractor {
        private val registerCalls = Turbine<Call.Register>()
        private val onAutocompleteCalls = Turbine<Call.OnAutocomplete>()

        override fun register(onEvent: (AutocompleteAddressInteractor.Event) -> Unit) {
            registerCalls.add(Call.Register(onEvent))
        }

        override fun onAutocomplete(country: String) {
            onAutocompleteCalls.add(Call.OnAutocomplete(country))
        }

        sealed interface Call {
            class Register(
                val onEvent: (AutocompleteAddressInteractor.Event) -> Unit
            )

            class OnAutocomplete(
                val country: String
            )
        }

        class Scenario(
            val interactor: AutocompleteAddressInteractor,
            val registerCalls: ReceiveTurbine<Call.Register>,
            val onAutocompleteCalls: ReceiveTurbine<Call.OnAutocomplete>,
        )

        companion object {
            suspend fun test(
                autocompleteConfig: AutocompleteAddressInteractor.Config,
                test: suspend Scenario.() -> Unit,
            ) {
                val interactor = TestAutocompleteAddressInteractor(
                    autocompleteConfig = autocompleteConfig,
                )

                val registerCalls = interactor.registerCalls
                val onAutocompleteCalls = interactor.onAutocompleteCalls

                test(
                    Scenario(
                        interactor = interactor,
                        registerCalls = registerCalls,
                        onAutocompleteCalls = onAutocompleteCalls,
                    )
                )

                registerCalls.ensureAllEventsConsumed()
                onAutocompleteCalls.ensureAllEventsConsumed()
            }

            fun noOp(
                autocompleteConfig: AutocompleteAddressInteractor.Config,
            ) = TestAutocompleteAddressInteractor(
                autocompleteConfig = autocompleteConfig,
            )
        }
    }

    private companion object {
        val DEFAULT_AUTOCOMPLETE_CONFIG = AutocompleteAddressInteractor.Config(
            googlePlacesApiKey = "123",
            autocompleteCountries = setOf("US"),
        )
    }
}
