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
}
