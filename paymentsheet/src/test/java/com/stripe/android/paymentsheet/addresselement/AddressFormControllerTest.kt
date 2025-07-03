package com.stripe.android.paymentsheet.addresselement

import app.cash.turbine.TurbineTestContext
import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import com.stripe.android.isInstanceOf
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.uicore.elements.AutocompleteAddressController
import com.stripe.android.uicore.elements.AutocompleteAddressElement
import com.stripe.android.uicore.elements.AutocompleteAddressInteractor
import com.stripe.android.uicore.elements.IdentifierSpec
import com.stripe.android.uicore.elements.PhoneNumberElement
import com.stripe.android.uicore.elements.RowElement
import com.stripe.android.uicore.elements.SectionElement
import com.stripe.android.uicore.elements.SectionFieldElement
import com.stripe.android.uicore.forms.FormFieldEntry
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
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
    fun `Complete form values are set when element flows are complete`() = runTest(UnconfinedTestDispatcher()) {
        val autocompleteEvent = MutableSharedFlow<AutocompleteAddressInteractor.Event>()

        val addressFormController = createAddressFormController(
            autocompleteEvent = autocompleteEvent,
            autocompleteConfig = AutocompleteAddressInteractor.Config(
                autocompleteCountries = setOf("US"),
                googlePlacesApiKey = "123",
            ),
        )

        addressFormController.completeFormValues.test {
            val formValues = awaitItem()

            assertThat(formValues).isNull()

            autocompleteEvent.emit(
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
    fun `Complete form values is empty when element flows are not complete`() = runTest(UnconfinedTestDispatcher()) {
        val autocompleteEvent = MutableSharedFlow<AutocompleteAddressInteractor.Event>()

        val addressFormController = createAddressFormController(
            autocompleteEvent = autocompleteEvent,
            autocompleteConfig = AutocompleteAddressInteractor.Config(
                autocompleteCountries = setOf("US"),
                googlePlacesApiKey = "123",
            ),
        )

        autocompleteEvent.emit(
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
    fun `Last text field identifier points to last element`() = runTest(UnconfinedTestDispatcher()) {
        val autocompleteEvent = MutableSharedFlow<AutocompleteAddressInteractor.Event>()

        val addressFormController = createAddressFormController(
            autocompleteConfig = AutocompleteAddressInteractor.Config(
                autocompleteCountries = setOf("US"),
                googlePlacesApiKey = "123",
            ),
            autocompleteEvent = autocompleteEvent,
        )

        autocompleteEvent.emit(
            AutocompleteAddressInteractor.Event.OnExpandForm(
                values = emptyMap(),
            )
        )

        addressFormController.lastTextFieldIdentifier.test {
            assertThat(awaitItem()).isEqualTo(IdentifierSpec.PostalCode)
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
        autocompleteConfig: AutocompleteAddressInteractor.Config = AutocompleteAddressInteractor.Config(
            autocompleteCountries = setOf("US"),
            googlePlacesApiKey = null,
        ),
        autocompleteEvent: MutableSharedFlow<AutocompleteAddressInteractor.Event> = MutableSharedFlow(),
        launcherConfig: AddressLauncher.Configuration = AddressLauncher.Configuration(),
        test: suspend TurbineTestContext<List<SectionFieldElement>>.() -> Unit
    ) = runTest(UnconfinedTestDispatcher()) {
        val addressFormController = createAddressFormController(
            autocompleteConfig = autocompleteConfig,
            launcherConfig = launcherConfig,
            autocompleteEvent = autocompleteEvent,
        )

        val elements = addressFormController.elements

        assertThat(elements).hasSize(1)
        assertThat(elements[0]).isInstanceOf<SectionElement>()

        val sectionElement = elements[0] as SectionElement
        val sectionElementFields = sectionElement.fields

        assertThat(sectionElementFields).hasSize(1)
        assertThat(sectionElementFields[0]).isInstanceOf<AutocompleteAddressElement>()

        val autocompleteAddressElement = sectionElementFields[0] as AutocompleteAddressElement

        val controller = autocompleteAddressElement.sectionFieldErrorController()

        assertThat(controller).isInstanceOf<AutocompleteAddressController>()

        val autocompleteAddressController = controller as AutocompleteAddressController

        autocompleteAddressController.addressElementFlow.test {
            val element = awaitItem()

            element.fields.test {
                test()
            }
        }
    }

    private fun TestScope.createAddressFormController(
        autocompleteConfig: AutocompleteAddressInteractor.Config = AutocompleteAddressInteractor.Config(
            autocompleteCountries = setOf("US"),
            googlePlacesApiKey = "123",
        ),
        autocompleteEvent: MutableSharedFlow<AutocompleteAddressInteractor.Event> = MutableSharedFlow(),
        launcherConfig: AddressLauncher.Configuration = AddressLauncher.Configuration(
            additionalFields = AddressLauncher.AdditionalFieldsConfiguration(
                phone = AddressLauncher.AdditionalFieldsConfiguration.FieldConfiguration.HIDDEN,
            )
        )
    ) = AddressFormController(
        interactor = TestAutocompleteAddressInteractor(
            interactorScope = backgroundScope,
            config = autocompleteConfig,
            autocompleteEvent = autocompleteEvent,
        ),
        config = launcherConfig,
    )
}
