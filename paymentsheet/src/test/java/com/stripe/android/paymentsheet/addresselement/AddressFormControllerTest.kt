package com.stripe.android.paymentsheet.addresselement

import app.cash.turbine.TurbineTestContext
import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import com.stripe.android.isInstanceOf
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.uicore.elements.AddressElement
import com.stripe.android.uicore.elements.AutocompleteAddressElement
import com.stripe.android.uicore.elements.AutocompleteAddressInteractor
import com.stripe.android.uicore.elements.IdentifierSpec
import com.stripe.android.uicore.elements.PhoneNumberElement
import com.stripe.android.uicore.elements.RowElement
import com.stripe.android.uicore.elements.SectionElement
import com.stripe.android.uicore.elements.SectionFieldElement
import com.stripe.android.uicore.forms.FormFieldEntry
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class AddressFormControllerTest {
    @Test
    fun `Ensure expected set of elements`() = fieldsTest(
        autocompleteConfig = AutocompleteAddressInteractor.Config(
            autocompleteCountries = setOf("US"),
            googlePlacesApiKey = "123456",
        ),
        launcherConfig = AddressLauncher.Configuration(
            address = AddressDetails(
                address = PaymentSheet.Address(
                    line1 = "123 Apple Street",
                )
            )
        )
    ) {
        val fields = awaitItem()

        assertThat(
            fields.any { field ->
                field.identifier == IdentifierSpec.Name
            }
        ).isTrue()

        assertThat(
            fields.any { field ->
                field.identifier == IdentifierSpec.Line1
            }
        ).isTrue()

        assertThat(
            fields.any { field ->
                field.identifier == IdentifierSpec.Line2
            }
        ).isTrue()

        assertThat(
            fields.any { field ->
                field is RowElement &&
                    field.fields.any { it.identifier == IdentifierSpec.City } &&
                    field.fields.any { it.identifier == IdentifierSpec.PostalCode }
            }
        ).isTrue()

        assertThat(
            fields.any { field ->
                field.identifier == IdentifierSpec.State
            }
        ).isTrue()

        assertThat(
            fields.any { field ->
                field.identifier == IdentifierSpec.Country
            }
        ).isTrue()
    }

    @Test
    fun `Phone number should be shown if field is OPTIONAL`() = phoneNumberTest(
        phoneNumberConfiguration = AddressLauncher.AdditionalFieldsConfiguration.FieldConfiguration.OPTIONAL,
        phoneNumberElementIsVisible = true,
    )

    @Test
    fun `Phone number should be shown if field is REQUIRED`() = phoneNumberTest(
        phoneNumberConfiguration = AddressLauncher.AdditionalFieldsConfiguration.FieldConfiguration.REQUIRED,
        phoneNumberElementIsVisible = true,
    )

    @Test
    fun `Phone number should not be shown if field is HIDDEN`() = phoneNumberTest(
        phoneNumberConfiguration = AddressLauncher.AdditionalFieldsConfiguration.FieldConfiguration.HIDDEN,
        phoneNumberElementIsVisible = false,
    )

    @Test
    fun `Complete form values are set when element flows are complete`() = test(
        autocompleteConfig = AutocompleteAddressInteractor.Config(
            autocompleteCountries = setOf("US"),
            googlePlacesApiKey = "123",
        )
    ) {
        val addressFormController = createAddressFormController()

        val registerCall = registerCalls.awaitItem()

        addressFormController.completeFormValues.test {
            val formValues = awaitItem()

            assertThat(formValues).isNull()

            registerCall.onEvent(
                AutocompleteAddressInteractor.Event.OnValues(
                    values = mapOf(
                        IdentifierSpec.Name to "John Doe",
                        IdentifierSpec.Line1 to "123 Apple Street",
                        IdentifierSpec.City to "San Francisco",
                        IdentifierSpec.State to "CA",
                        IdentifierSpec.Country to "US",
                        IdentifierSpec.PostalCode to "94111",
                    ),
                )
            )

            val completeFormValues = awaitItem()

            assertThat(completeFormValues?.get(IdentifierSpec.Line1))
                .isEqualTo(FormFieldEntry(value = "123 Apple Street", isComplete = true))
            assertThat(completeFormValues?.get(IdentifierSpec.Line2))
                .isEqualTo(FormFieldEntry(value = "", isComplete = true))
            assertThat(completeFormValues?.get(IdentifierSpec.City))
                .isEqualTo(FormFieldEntry(value = "San Francisco", isComplete = true))
            assertThat(completeFormValues?.get(IdentifierSpec.State))
                .isEqualTo(FormFieldEntry(value = "CA", isComplete = true))
            assertThat(completeFormValues?.get(IdentifierSpec.Country))
                .isEqualTo(FormFieldEntry(value = "US", isComplete = true))
            assertThat(completeFormValues?.get(IdentifierSpec.PostalCode))
                .isEqualTo(FormFieldEntry(value = "94111", isComplete = true))
        }
    }

    @Test
    fun `Complete form values is empty when element flows are not complete`() = test(
        autocompleteConfig = AutocompleteAddressInteractor.Config(
            autocompleteCountries = setOf("US"),
            googlePlacesApiKey = "123",
        ),
    ) {
        val addressFormController = createAddressFormController()

        val registerCall = registerCalls.awaitItem()

        registerCall.onEvent(
            AutocompleteAddressInteractor.Event.OnValues(
                values = mapOf(
                    IdentifierSpec.Name to "John Doe",
                    IdentifierSpec.Line1 to "123 Apple Street",
                    IdentifierSpec.State to "CA",
                    IdentifierSpec.Country to "US",
                    IdentifierSpec.PostalCode to "94111",
                ),
            )
        )

        addressFormController.completeFormValues.test {
            val formValues = awaitItem()

            assertThat(formValues).isNull()
        }
    }

    @Test
    fun `Last text field identifier points to last element`() = test(
        autocompleteConfig = AutocompleteAddressInteractor.Config(
            autocompleteCountries = setOf("US"),
            googlePlacesApiKey = "123",
        ),
    ) {
        val addressFormController = createAddressFormController()

        val registerCall = registerCalls.awaitItem()

        registerCall.onEvent(
            AutocompleteAddressInteractor.Event.OnExpandForm(
                values = emptyMap(),
            )
        )

        addressFormController.lastTextFieldIdentifier.test {
            assertThat(awaitItem()).isEqualTo(IdentifierSpec.PostalCode)
        }
    }

    @Test
    fun `Initial provided values are pushed to address element`() = addressElementTest(
        initialValues = mapOf(
            IdentifierSpec.Name to "John Doe",
            IdentifierSpec.Line1 to "123 Apple Street",
            IdentifierSpec.City to "San Francisco",
            IdentifierSpec.State to "CA",
            IdentifierSpec.Country to "US",
            IdentifierSpec.PostalCode to "94111",
            IdentifierSpec.Phone to "+17893424625"
        ),
        autocompleteConfig = AutocompleteAddressInteractor.Config(
            autocompleteCountries = setOf("US"),
            googlePlacesApiKey = null,
        ),
    ) {
        val element = awaitItem()

        element.getFormFieldValueFlow().test {
            assertThat(awaitItem()).containsExactlyElementsIn(
                listOf(
                    IdentifierSpec.Name to FormFieldEntry(value = "John Doe", isComplete = true),
                    IdentifierSpec.Line1 to FormFieldEntry(value = "123 Apple Street", isComplete = true),
                    IdentifierSpec.Line2 to FormFieldEntry(value = "", isComplete = true),
                    IdentifierSpec.City to FormFieldEntry(value = "San Francisco", isComplete = true),
                    IdentifierSpec.State to FormFieldEntry(value = "CA", isComplete = true),
                    IdentifierSpec.Country to FormFieldEntry(value = "US", isComplete = true),
                    IdentifierSpec.PostalCode to FormFieldEntry(value = "94111", isComplete = true),
                    IdentifierSpec.Phone to FormFieldEntry(value = "+17893424625", isComplete = true),
                    IdentifierSpec.PhoneNumberCountry to FormFieldEntry(value = "US", isComplete = true),
                )
            )
        }
    }

    private fun phoneNumberTest(
        phoneNumberConfiguration: AddressLauncher.AdditionalFieldsConfiguration.FieldConfiguration,
        phoneNumberElementIsVisible: Boolean,
    ) = fieldsTest(
        launcherConfig = AddressLauncher.Configuration(
            additionalFields = AddressLauncher.AdditionalFieldsConfiguration(
                phone = phoneNumberConfiguration,
            ),
        ),
    ) {
        val fields = awaitItem()

        assertThat(
            fields.any { field ->
                field is PhoneNumberElement
            }
        ).isEqualTo(phoneNumberElementIsVisible)
    }

    private fun fieldsTest(
        initialValues: Map<IdentifierSpec, String?> = emptyMap(),
        autocompleteConfig: AutocompleteAddressInteractor.Config = AutocompleteAddressInteractor.Config(
            autocompleteCountries = setOf("US"),
            googlePlacesApiKey = null,
        ),
        launcherConfig: AddressLauncher.Configuration = AddressLauncher.Configuration(),
        test: suspend TurbineTestContext<List<SectionFieldElement>>.() -> Unit
    ) = addressElementTest(
        initialValues = initialValues,
        autocompleteConfig = autocompleteConfig,
        launcherConfig = launcherConfig,
    ) {
        val element = awaitItem()

        element.fields.test {
            test()
        }
    }

    private fun addressElementTest(
        initialValues: Map<IdentifierSpec, String?> = emptyMap(),
        autocompleteConfig: AutocompleteAddressInteractor.Config = AutocompleteAddressInteractor.Config(
            autocompleteCountries = setOf("US"),
            googlePlacesApiKey = null,
        ),
        launcherConfig: AddressLauncher.Configuration = AddressLauncher.Configuration(),
        test: suspend TurbineTestContext<AddressElement>.() -> Unit
    ) = test(autocompleteConfig) {
        val addressFormController = createAddressFormController(
            initialValues = initialValues,
            launcherConfig = launcherConfig,
        )

        assertThat(registerCalls.awaitItem()).isNotNull()

        val elements = addressFormController.elements

        assertThat(elements).hasSize(1)
        assertThat(elements[0]).isInstanceOf<SectionElement>()

        val sectionElement = elements[0] as SectionElement
        val sectionElementFields = sectionElement.fields

        assertThat(sectionElementFields).hasSize(1)
        assertThat(sectionElementFields[0]).isInstanceOf<AutocompleteAddressElement>()

        val autocompleteAddressElement = sectionElementFields[0] as AutocompleteAddressElement

        autocompleteAddressElement.sectionFieldErrorController().addressElementFlow.test {
            test()
        }
    }

    private fun test(
        autocompleteConfig: AutocompleteAddressInteractor.Config = AutocompleteAddressInteractor.Config(
            autocompleteCountries = setOf("US"),
            googlePlacesApiKey = "123",
        ),
        block: suspend TestAutocompleteAddressInteractor.Scenario.() -> Unit
    ) = runTest {
        TestAutocompleteAddressInteractor.test(
            autocompleteConfig = autocompleteConfig,
        ) {
            block()
        }
    }

    private fun TestAutocompleteAddressInteractor.Scenario.createAddressFormController(
        initialValues: Map<IdentifierSpec, String?> = emptyMap(),
        launcherConfig: AddressLauncher.Configuration = AddressLauncher.Configuration(
            additionalFields = AddressLauncher.AdditionalFieldsConfiguration(
                phone = AddressLauncher.AdditionalFieldsConfiguration.FieldConfiguration.HIDDEN,
            )
        )
    ) = AddressFormController(
        interactor = interactor,
        config = launcherConfig,
        initialValues = initialValues,
    )
}
