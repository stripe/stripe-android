package com.stripe.android.paymentsheet.forms

import androidx.annotation.StringRes
import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import com.stripe.android.core.model.CountryUtils
import com.stripe.android.core.strings.resolvableString
import com.stripe.android.lpmfoundations.ContactInformationCollectionMode
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadataFactory
import com.stripe.android.lpmfoundations.paymentmethod.TestUiDefinitionFactoryArgumentsFactory
import com.stripe.android.model.PaymentMethod
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.PaymentSheetFixtures.COMPOSE_FRAGMENT_ARGS
import com.stripe.android.paymentsheet.model.PaymentSelection
import com.stripe.android.paymentsheet.paymentdatacollection.FormArguments
import com.stripe.android.paymentsheet.utils.ViewModelStoreTestRule
import com.stripe.android.testing.CleanupTestRule
import com.stripe.android.ui.core.R
import com.stripe.android.ui.core.elements.AffirmHeaderElement
import com.stripe.android.ui.core.elements.IbanConfig
import com.stripe.android.ui.core.elements.MandateTextElement
import com.stripe.android.ui.core.elements.SaveForFutureUseElement
import com.stripe.android.ui.core.elements.StaticTextElement
import com.stripe.android.uicore.elements.AddressElement
import com.stripe.android.uicore.elements.AddressFieldsElement
import com.stripe.android.uicore.elements.AddressInputMode
import com.stripe.android.uicore.elements.CountryConfig
import com.stripe.android.uicore.elements.CountryElement
import com.stripe.android.uicore.elements.DropdownFieldController
import com.stripe.android.uicore.elements.FormElement
import com.stripe.android.uicore.elements.IdentifierSpec
import com.stripe.android.uicore.elements.PhoneNumberElement
import com.stripe.android.uicore.elements.RowElement
import com.stripe.android.uicore.elements.SectionElement
import com.stripe.android.uicore.elements.SectionSingleFieldElement
import com.stripe.android.uicore.elements.SimpleTextElement
import com.stripe.android.uicore.elements.SimpleTextFieldController
import com.stripe.android.uicore.elements.TextFieldController
import com.stripe.android.uicore.forms.FormFieldEntry
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import com.stripe.android.core.R as CoreR
import com.stripe.android.uicore.R as UiCoreR

@Suppress("LargeClass")
@RunWith(RobolectricTestRunner::class)
internal class FormViewModelTest {
    private val emailIdentifier = IdentifierSpec.Email

    private val viewModelStoreRule = ViewModelStoreTestRule()

    private val coroutineScopeCleanupRule = CleanupTestRule<CoroutineScope> { cancel() }

    @get:Rule
    val ruleChain: RuleChain = RuleChain.outerRule(viewModelStoreRule)
        .around(coroutineScopeCleanupRule)

    private val coroutineScope = coroutineScopeCleanupRule.track(CoroutineScope(Dispatchers.Unconfined))

    @Test
    fun `Verify completeFormValues is not null when no elements exist`() = runTest {
        val args = COMPOSE_FRAGMENT_ARGS.copy(
            paymentMethodCode = PaymentMethod.Type.Card.code
        )
        val formViewModel = createViewModel(
            arguments = args,
            formElements = emptyList(),
        )

        assertThat(
            formViewModel.completeFormValues.first()
        ).isNotNull()
    }

    @Test
    fun `Verify if there are no text fields, there is no last text field id`() = runTest {
        // Here we have just a country, no text fields.
        val args = COMPOSE_FRAGMENT_ARGS.copy(
            paymentMethodCode = PaymentMethod.Type.Card.code
        )
        val formViewModel = createViewModel(
            args,
            listOf(
                createCountryElement(CountryUtils.supportedBillingCountries)
            )
        )

        // Verify formFieldValues does not contain email
        assertThat(formViewModel.lastTextFieldIdentifier.first()?.v1).isEqualTo(
            null
        )
    }

    @Test
    fun `Verify if the last text field is hidden the second to last text field is the last text field id`() =
        runTest {
            // Here we have one hidden (email) and one required field (name), country will always be in the result,
            //  and email only if it is not hidden
            val args = COMPOSE_FRAGMENT_ARGS.copy(
                paymentMethodCode = PaymentMethod.Type.P24.code
            )
            val formViewModel = createViewModel(
                args,
                listOf(
                    createNameElement(),
                    createEmailElement(),
                    createCountryElement(CountryUtils.supportedBillingCountries),
                )
            )

            formViewModel.addHiddenIdentifiers(setOf(IdentifierSpec.Email))

            // Verify formFieldValues does not contain email
            assertThat(formViewModel.lastTextFieldIdentifier.first()?.v1).isEqualTo(
                IdentifierSpec.Name.v1
            )
        }

    @Test
    fun `Verify if a field is hidden and valid it is not in the completeFormValues`() = runTest {
        // Here we have one hidden (email) and one required field (name), bank will always be in the result,
        //  and name only if not hidden
        val args = COMPOSE_FRAGMENT_ARGS.copy(
            paymentMethodCode = PaymentMethod.Type.Card.code
        )
        val formViewModel = createViewModel(
            args,
            listOf(
                createEmailElement(),
                createCountryElement(CountryUtils.supportedBillingCountries),
            )
        )

        val emailController =
            getSectionFieldTextControllerWithLabel(formViewModel, UiCoreR.string.stripe_email)

        // Add text to the name to make it valid
        emailController?.onValueChange("email@example.com")

        // Verify formFieldValues contains email
        assertThat(
            formViewModel.completeFormValues.first()?.fieldValuePairs
        ).containsKey(IdentifierSpec.Email)

        formViewModel.addHiddenIdentifiers(setOf(IdentifierSpec.Email))

        // Verify formFieldValues does not contain email
        assertThat(formViewModel.completeFormValues.first()?.fieldValuePairs)
            .doesNotContainKey(IdentifierSpec.Email)
    }

    @Test
    fun `Hidden invalid fields arent in the formViewValue and has no effect on complete state`() =
        runTest {
            // Here we have one hidden (email) and one required field (name), bank will always be in the result,
            //  and email only if not hidden
            val args = COMPOSE_FRAGMENT_ARGS.copy(
                paymentMethodCode = PaymentMethod.Type.Card.code
            )
            val formViewModel = createViewModel(
                args,
                listOf(
                    createEmailElement(),
                    createCountryElement(CountryUtils.supportedBillingCountries),
                    SaveForFutureUseElement(true, ""),
                )
            )

            val emailController =
                getSectionFieldTextControllerWithLabel(formViewModel, UiCoreR.string.stripe_email)

            // Add text to the email to make it invalid
            emailController?.onValueChange("joe")

            // Verify formFieldValues is null because the email is required and invalid
            assertThat(formViewModel.completeFormValues.first()).isNull()

            formViewModel.addHiddenIdentifiers(setOf(IdentifierSpec.Email))

            // Verify formFieldValues is not null even though the card number is invalid
            // (because it is not required)
            val completeFormFieldValues = formViewModel.completeFormValues.first()
            assertThat(
                completeFormFieldValues
            ).isNotNull()
            assertThat(formViewModel.completeFormValues.first()?.fieldValuePairs).doesNotContainKey(
                IdentifierSpec.Email
            )
        }

    /**
     * This is serving as more of an integration test of forms from
     * spec to FormFieldValues.
     */
    @Test
    fun `Verify params are set when element flows are complete`() = runTest {
        val args = COMPOSE_FRAGMENT_ARGS.copy(
            paymentMethodCode = PaymentMethod.Type.P24.code,
            billingDetails = null,
        )
        val formViewModel = createViewModel(
            args,
            listOf(
                createNameElement(),
                createEmailElement(),
                createCountryElement(
                    allowedCountryCodes = setOf(
                        "AT",
                        "BE",
                        "DE",
                        "ES",
                        "IT",
                        "NL"
                    )
                ),
                SaveForFutureUseElement(true, ""),
            )
        )

        val nameElement =
            getSectionFieldTextControllerWithLabel(formViewModel, CoreR.string.stripe_address_label_full_name)
        val emailElement =
            getSectionFieldTextControllerWithLabel(formViewModel, UiCoreR.string.stripe_email)

        nameElement?.onValueChange("joe")
        assertThat(
            formViewModel.completeFormValues.first()?.fieldValuePairs?.get(IdentifierSpec.Name)
        ).isNull()

        emailElement?.onValueChange("joe@example.com")
        assertThat(
            formViewModel.completeFormValues.first()?.fieldValuePairs?.get(IdentifierSpec.Email)
                ?.value
        ).isEqualTo("joe@example.com")
        assertThat(
            formViewModel.completeFormValues.first()?.fieldValuePairs?.get(IdentifierSpec.Name)
                ?.value
        ).isEqualTo("joe")

        emailElement?.onValueChange("invalid.email@IncompleteDomain")

        assertThat(
            formViewModel.completeFormValues.first()?.fieldValuePairs?.get(IdentifierSpec.Name)
        ).isNull()
    }

    @Test
    fun `Verify params are set when element address fields are complete`() = runTest {
        val args = COMPOSE_FRAGMENT_ARGS.copy(
            paymentMethodCode = PaymentMethod.Type.SepaDebit.code,
            billingDetails = null
        )
        val addressElements = createAddressElements(
            identifier = IdentifierSpec.Generic("address"),
            allowedCountryCodes = setOf("US", "JP"),
            hideCountry = false,
        )
            .toTypedArray()

        val formViewModel = createViewModel(
            args,
            listOfNotNull(
                createNameElement(),
                createEmailElement(),
                createIbanElement(),
                *addressElements,
                createMandateElement(),
            )
        )

        getSectionFieldTextControllerWithLabel(
            formViewModel,
            CoreR.string.stripe_address_label_full_name
        )?.onValueChange("joe")
        assertThat(
            formViewModel.completeFormValues.first()?.fieldValuePairs?.get(IdentifierSpec.Name)
                ?.value
        ).isNull()

        getSectionFieldTextControllerWithLabel(
            formViewModel,
            UiCoreR.string.stripe_email
        )?.onValueChange("joe@example.com")
        assertThat(
            formViewModel.completeFormValues.first()?.fieldValuePairs?.get(IdentifierSpec.Email)
                ?.value
        ).isNull()

        getSectionFieldTextControllerWithLabel(
            formViewModel,
            R.string.stripe_iban
        )?.onValueChange("DE89370400440532013000")
        assertThat(
            formViewModel.completeFormValues.first()?.fieldValuePairs?.get(IdentifierSpec.Generic("iban"))
                ?.value
        ).isNull()

        val addressControllers = AddressControllers.create(formViewModel)
        addressControllers.controllers.forEachIndexed { index, textFieldController ->
            textFieldController.onValueChange("12345")
            assertThat(
                formViewModel
                    .completeFormValues
                    .first()
                    ?.fieldValuePairs
                    ?.get(emailIdentifier)
                    ?.value
            ).isNull()
        }
        addressControllers.dropdownControllers.forEachIndexed { index, textFieldController ->
            textFieldController.onValueChange(0)
            if (index == addressControllers.dropdownControllers.size - 1) {
                assertThat(
                    formViewModel
                        .completeFormValues
                        .first()
                        ?.fieldValuePairs
                        ?.get(emailIdentifier)
                        ?.value
                ).isNotNull()
            } else {
                assertThat(
                    formViewModel
                        .completeFormValues
                        .first()
                        ?.fieldValuePairs
                        ?.get(emailIdentifier)
                        ?.value
                ).isNull()
            }
        }
        assertThat(formViewModel.completeFormValues.first()?.userRequestedReuse).isEqualTo(
            PaymentSelection.CustomerRequestedSave.NoRequest
        )
    }

    @Test
    fun `Verify params are set when required address fields are complete`() = runTest {
        /**
         * Using sepa debit as a complex enough example to test the address portion.
         */
        val args = COMPOSE_FRAGMENT_ARGS.copy(
            paymentMethodCode = PaymentMethod.Type.SepaDebit.code,
            billingDetails = null
        )
        val addressElements = createAddressElements(
            identifier = IdentifierSpec.Generic("address"),
            allowedCountryCodes = setOf("US", "JP"),
            hideCountry = false,
        )
            .toTypedArray()

        val formViewModel = createViewModel(
            args,
            listOfNotNull(
                createNameElement(),
                createEmailElement(),
                createIbanElement(),
                *addressElements,
                createMandateElement()
            )
        )

        getSectionFieldTextControllerWithLabel(
            formViewModel,
            CoreR.string.stripe_address_label_full_name
        )?.onValueChange("joe")
        assertThat(
            formViewModel.completeFormValues.first()?.fieldValuePairs?.get(emailIdentifier)
                ?.value
        ).isNull()

        getSectionFieldTextControllerWithLabel(
            formViewModel,
            UiCoreR.string.stripe_email
        )?.onValueChange("joe@example.com")
        assertThat(
            formViewModel.completeFormValues.first()?.fieldValuePairs?.get(emailIdentifier)
                ?.value
        ).isNull()

        getSectionFieldTextControllerWithLabel(
            formViewModel,
            R.string.stripe_iban
        )?.onValueChange("DE89370400440532013000")
        assertThat(
            formViewModel.completeFormValues.first()?.fieldValuePairs?.get(emailIdentifier)
                ?.value
        ).isNull()

        // Fill all address values except line2
        val addressControllers = AddressControllers.create(formViewModel)
        val populateAddressControllers = addressControllers.controllers
            .filter { it.label.first() != resolvableString(UiCoreR.string.stripe_address_label_address_line2) }
        populateAddressControllers
            .forEachIndexed { index, textFieldController ->
                textFieldController.onValueChange("12345")

                assertThat(
                    formViewModel
                        .completeFormValues
                        .first()
                        ?.fieldValuePairs
                        ?.get(emailIdentifier)
                        ?.value
                ).isNull()
            }

        addressControllers.dropdownControllers.forEachIndexed { index, textFieldController ->
            textFieldController.onValueChange(0)
            if (index == addressControllers.dropdownControllers.size - 1) {
                assertThat(
                    formViewModel
                        .completeFormValues
                        .first()
                        ?.fieldValuePairs
                        ?.get(emailIdentifier)
                        ?.value
                ).isNotNull()
            } else {
                assertThat(
                    formViewModel
                        .completeFormValues
                        .first()
                        ?.fieldValuePairs
                        ?.get(emailIdentifier)
                        ?.value
                ).isNull()
            }
        }
    }

    @Test
    fun `Test default values are filled`() = runTest {
        val args = COMPOSE_FRAGMENT_ARGS.copy(
            paymentMethodCode = PaymentMethod.Type.Card.code,
            billingDetails = PaymentSheet.BillingDetails(
                name = "Jenny Rosen",
                email = "mail@mail.com",
                phone = "+13105551234",
                address = PaymentSheet.Address(
                    line1 = "123 Main Street",
                    line2 = "456",
                    city = "San Francisco",
                    state = "CA",
                    country = "US",
                    postalCode = "94111"
                ),
            ),
            billingDetailsCollectionConfiguration = PaymentSheet.BillingDetailsCollectionConfiguration(
                attachDefaultsToPaymentMethod = true,
            )
        )

        val viewModel = createViewModel(
            args,
            emptyList(),
        )

        viewModel.completeFormValues.test {
            assertThat(awaitItem()?.fieldValuePairs).isEqualTo(
                mapOf(
                    IdentifierSpec.Name to FormFieldEntry("Jenny Rosen", isComplete = true),
                    IdentifierSpec.Email to FormFieldEntry("mail@mail.com", isComplete = true),
                    IdentifierSpec.Phone to FormFieldEntry("+13105551234", isComplete = true),
                    IdentifierSpec.Line1 to FormFieldEntry("123 Main Street", isComplete = true),
                    IdentifierSpec.Line2 to FormFieldEntry("456", isComplete = true),
                    IdentifierSpec.City to FormFieldEntry("San Francisco", isComplete = true),
                    IdentifierSpec.State to FormFieldEntry("CA", isComplete = true),
                    IdentifierSpec.Country to FormFieldEntry("US", isComplete = true),
                    IdentifierSpec.PostalCode to FormFieldEntry("94111", isComplete = true),
                )
            )
        }
    }

    @Test
    fun `Test only provided default values are filled`() = runTest {
        val args = COMPOSE_FRAGMENT_ARGS.copy(
            paymentMethodCode = PaymentMethod.Type.Card.code,
            billingDetails = PaymentSheet.BillingDetails(
                name = "Jenny Rosen",
                email = "mail@mail.com",
                address = PaymentSheet.Address(
                    country = "US",
                    postalCode = "94111"
                ),
            ),
            billingDetailsCollectionConfiguration = PaymentSheet.BillingDetailsCollectionConfiguration(
                attachDefaultsToPaymentMethod = true,
            )
        )

        val viewModel = createViewModel(
            args,
            emptyList(),
        )

        viewModel.completeFormValues.test {
            assertThat(awaitItem()?.fieldValuePairs).isEqualTo(
                mapOf(
                    IdentifierSpec.Name to FormFieldEntry("Jenny Rosen", isComplete = true),
                    IdentifierSpec.Email to FormFieldEntry("mail@mail.com", isComplete = true),
                    IdentifierSpec.Country to FormFieldEntry("US", isComplete = true),
                    IdentifierSpec.PostalCode to FormFieldEntry("94111", isComplete = true),
                )
            )
        }
    }

    @Test
    fun `Test default values are not filled`() = runTest {
        val args = COMPOSE_FRAGMENT_ARGS.copy(
            paymentMethodCode = PaymentMethod.Type.Card.code,
            billingDetails = PaymentSheet.BillingDetails(
                name = "Jenny Rosen",
                email = "mail@mail.com",
                phone = "+13105551234",
                address = PaymentSheet.Address(
                    line1 = "123 Main Street",
                    line2 = "456",
                    city = "San Francisco",
                    state = "CA",
                    country = "US",
                    postalCode = "94111"
                ),
            ),
            billingDetailsCollectionConfiguration = PaymentSheet.BillingDetailsCollectionConfiguration(
                attachDefaultsToPaymentMethod = false,
            )
        )

        val viewModel = createViewModel(
            args,
            emptyList(),
        )

        viewModel.completeFormValues.test {
            assertThat(awaitItem()?.fieldValuePairs).isEmpty()
        }
    }

    @Test
    fun `Test phone country changes with AddressElement country`() =
        runBlocking {
            val billingDetailsCollectionConfiguration = PaymentSheet.BillingDetailsCollectionConfiguration(
                name = PaymentSheet.BillingDetailsCollectionConfiguration.CollectionMode.Always,
                email = PaymentSheet.BillingDetailsCollectionConfiguration.CollectionMode.Always,
                phone = PaymentSheet.BillingDetailsCollectionConfiguration.CollectionMode.Always,
                address = PaymentSheet.BillingDetailsCollectionConfiguration.AddressCollectionMode.Full,
                attachDefaultsToPaymentMethod = false,
            )

            val args = COMPOSE_FRAGMENT_ARGS.copy(
                PaymentMethod.Type.Card.code,
                billingDetailsCollectionConfiguration = billingDetailsCollectionConfiguration,
                billingDetails = PaymentSheet.BillingDetails(),
            )

            val cardFormElements = PaymentMethodMetadataFactory.create().formElementsForCode(
                code = "card",
                uiDefinitionFactoryArgumentsFactory = TestUiDefinitionFactoryArgumentsFactory.create(
                    coroutineScope = coroutineScope,
                ),
            )!!

            val formViewModel = createViewModel(
                args,
                cardFormElements + createPhoneElement(),
            )

            val elements = formViewModel.elements
            val countryElement = elements
                .filterIsInstance<SectionElement>()
                .flatMap { it.fields }
                .filterIsInstance<AddressFieldsElement>()
                .firstOrNull()
                ?.countryElement
            val phoneElement = elements
                .filterIsInstance<SectionElement>()
                .flatMap { it.fields }
                .filterIsInstance<PhoneNumberElement>()
                .firstOrNull()

            assertThat(countryElement).isNotNull()
            assertThat(phoneElement).isNotNull()
            countryElement?.controller?.onRawValueChange("CA")
            assertThat(phoneElement?.controller?.countryDropdownController?.rawFieldValue?.first())
                .isEqualTo("CA")
            phoneElement?.controller?.onValueChange("+13105551234")
            countryElement?.controller?.onRawValueChange("US")
            // Phone number shouldn't change because it is already filled.
            assertThat(phoneElement?.controller?.countryDropdownController?.rawFieldValue?.first())
                .isEqualTo("CA")
        }

    @Test
    fun `Test phone country changes with standalone CountryElement`() =
        runBlocking {
            val billingDetailsCollectionConfiguration = PaymentSheet.BillingDetailsCollectionConfiguration(
                name = PaymentSheet.BillingDetailsCollectionConfiguration.CollectionMode.Always,
                email = PaymentSheet.BillingDetailsCollectionConfiguration.CollectionMode.Always,
                phone = PaymentSheet.BillingDetailsCollectionConfiguration.CollectionMode.Always,
                address = PaymentSheet.BillingDetailsCollectionConfiguration.AddressCollectionMode.Full,
                attachDefaultsToPaymentMethod = false,
            )

            val args = COMPOSE_FRAGMENT_ARGS.copy(
                PaymentMethod.Type.PayPal.code,
                billingDetailsCollectionConfiguration = billingDetailsCollectionConfiguration,
                billingDetails = PaymentSheet.BillingDetails(),
            )
            val addressElements = createAddressElements(
                identifier = IdentifierSpec.BillingAddress,
                allowedCountryCodes = CountryUtils.supportedBillingCountries,
                hideCountry = true,
            )
                .toTypedArray()
            val formViewModel = createViewModel(
                args,
                listOfNotNull(
                    createNameElement(),
                    createEmailElement(),
                    createPhoneElement(),
                    createCountryElement(CountryUtils.supportedBillingCountries),
                    *addressElements,
                ),
            )

            val elements = formViewModel.elements
            val countryElement = elements
                .filterIsInstance<SectionElement>()
                .flatMap { it.fields }
                .filterIsInstance<CountryElement>()
                .firstOrNull()
            val phoneElement = elements
                .filterIsInstance<SectionElement>()
                .flatMap { it.fields }
                .filterIsInstance<PhoneNumberElement>()
                .firstOrNull()

            assertThat(countryElement).isNotNull()
            assertThat(phoneElement).isNotNull()
            countryElement?.controller?.onRawValueChange("CA")
            assertThat(phoneElement?.controller?.countryDropdownController?.rawFieldValue?.first())
                .isEqualTo("CA")
            phoneElement?.controller?.onValueChange("+13105551234")
            countryElement?.controller?.onRawValueChange("US")
            // Phone number shouldn't change because it is already filled.
            assertThat(phoneElement?.controller?.countryDropdownController?.rawFieldValue?.first())
                .isEqualTo("CA")
        }

    private suspend fun getSectionFieldTextControllerWithLabel(
        formViewModel: FormViewModel,
        @StringRes label: Int
    ) =
        formViewModel.elements
            .filterIsInstance<SectionElement>()
            .flatMap { it.fields }
            .filterIsInstance<SectionSingleFieldElement>()
            .map { it.controller }
            .filterIsInstance<TextFieldController>()
            .firstOrNull { it.label.first() == resolvableString(label) }

    private data class AddressControllers(
        val controllers: List<TextFieldController>,
        val dropdownControllers: List<DropdownFieldController>,
    ) {
        companion object {
            suspend fun create(formViewModel: FormViewModel) =
                AddressControllers(
                    listOfNotNull(
                        getAddressSectionTextControllerWithLabel(
                            formViewModel,
                            CoreR.string.stripe_address_label_address_line1
                        ),
                        getAddressSectionTextControllerWithLabel(
                            formViewModel,
                            UiCoreR.string.stripe_address_label_address_line2
                        ),
                        getAddressSectionTextControllerWithLabel(
                            formViewModel,
                            CoreR.string.stripe_address_label_city
                        ),
                        getAddressSectionTextControllerWithLabel(
                            formViewModel,
                            CoreR.string.stripe_address_label_zip_code
                        )
                    ),
                    listOfNotNull(
                        getAddressSectionDropdownControllerWithLabel(
                            formViewModel,
                            CoreR.string.stripe_address_label_state
                        )
                    )
                )
        }
    }

    companion object {
        private suspend fun getAddressSectionTextControllerWithLabel(
            formViewModel: FormViewModel,
            @StringRes label: Int
        ): TextFieldController? {
            val addressElementFields = formViewModel.elements
                .filterIsInstance<SectionElement>()
                .flatMap { it.fields }
                .filterIsInstance<AddressElement>()
                .firstOrNull()
                ?.fields
                ?.first()
            return addressElementFields
                ?.filterIsInstance<SectionSingleFieldElement>()
                ?.map { (it.controller as? SimpleTextFieldController) }
                ?.firstOrNull { it?.label?.first() == resolvableString(label) }
                ?: addressElementFields
                    ?.asSequence()
                    ?.filterIsInstance<RowElement>()
                    ?.map { it.fields }
                    ?.flatten()
                    ?.map { (it.controller as? SimpleTextFieldController) }
                    ?.firstOrNull { it?.label?.first() == resolvableString(label) }
        }

        private suspend fun getAddressSectionDropdownControllerWithLabel(
            formViewModel: FormViewModel,
            @StringRes label: Int
        ): DropdownFieldController? {
            val addressElementFields = formViewModel.elements
                .filterIsInstance<SectionElement>()
                .flatMap { it.fields }
                .filterIsInstance<AddressElement>()
                .firstOrNull()
                ?.fields
                ?.first()
            return addressElementFields
                ?.filterIsInstance<SectionSingleFieldElement>()
                ?.map { (it.controller as? DropdownFieldController) }
                ?.firstOrNull { it?.label?.first() == resolvableString(label) }
                ?: addressElementFields
                    ?.asSequence()
                    ?.filterIsInstance<RowElement>()
                    ?.map { it.fields }
                    ?.flatten()
                    ?.map { (it.controller as? DropdownFieldController) }
                    ?.firstOrNull { it?.label?.first() == resolvableString(label) }
        }
    }

    @Test
    fun `updateFormElements updates elements when identifiers differ`() {
        val args = COMPOSE_FRAGMENT_ARGS.copy(
            paymentMethodCode = PaymentMethod.Type.Card.code
        )
        val originalElements = listOf(
            createEmailElement(),
        )
        val formViewModel = createViewModel(args, originalElements)

        assertThat(formViewModel.elements).isEqualTo(originalElements)

        val newElements = listOf(
            createNameElement(),
            createEmailElement(),
        )
        formViewModel.updateFormElements(newElements)

        assertThat(formViewModel.elements).isEqualTo(newElements)
    }

    @Test
    fun `updateFormElements preserves form field values when identifiers match`() = runTest {
        val args = COMPOSE_FRAGMENT_ARGS.copy(
            paymentMethodCode = PaymentMethod.Type.P24.code
        )
        val originalElements = listOf(
            createNameElement(),
            createEmailElement(),
        )
        val formViewModel = createViewModel(args, originalElements)

        val nameController = getSectionFieldTextControllerWithLabel(
            formViewModel,
            CoreR.string.stripe_address_label_full_name
        )
        val emailController = getSectionFieldTextControllerWithLabel(
            formViewModel,
            UiCoreR.string.stripe_email
        )

        nameController?.onValueChange("Jane Doe")
        emailController?.onValueChange("jane@example.com")

        val newElementsWithSameIdentifiers = listOf(
            createNameElement(),
            createEmailElement(),
        )
        formViewModel.updateFormElements(newElementsWithSameIdentifiers)

        val updatedNameController = getSectionFieldTextControllerWithLabel(
            formViewModel,
            CoreR.string.stripe_address_label_full_name
        )
        val updatedEmailController = getSectionFieldTextControllerWithLabel(
            formViewModel,
            UiCoreR.string.stripe_email
        )

        assertThat(updatedNameController?.fieldValue?.first()).isEqualTo("Jane Doe")
        assertThat(updatedEmailController?.fieldValue?.first()).isEqualTo("jane@example.com")
    }

    @Test
    fun `updateFormElements updates elements when identifiers match but types differ`() {
        val args = COMPOSE_FRAGMENT_ARGS.copy(
            paymentMethodCode = PaymentMethod.Type.Card.code
        )
        val originalElements = listOf(
            AffirmHeaderElement(
                identifier = IdentifierSpec.Generic("affirm_promotion")
            ),
        )
        val formViewModel = createViewModel(args, originalElements)

        assertThat(formViewModel.elements).isEqualTo(originalElements)

        val newElements = listOf(
            StaticTextElement(
                identifier = IdentifierSpec.Generic("affirm_promotion"),
                text = resolvableString("Static text"),
            ),
        )
        formViewModel.updateFormElements(newElements)

        assertThat(formViewModel.elements).isEqualTo(newElements)
    }

    @Test
    fun `updateFormElements does not update elements when identifiers match`() {
        val args = COMPOSE_FRAGMENT_ARGS.copy(
            paymentMethodCode = PaymentMethod.Type.Card.code
        )
        val originalElements = listOf(
            createEmailElement(),
            createCountryElement(CountryUtils.supportedBillingCountries),
        )
        val formViewModel = createViewModel(args, originalElements)

        val newElementsWithSameIdentifiers = listOf(
            createEmailElement(),
            createCountryElement(CountryUtils.supportedBillingCountries),
        )
        formViewModel.updateFormElements(newElementsWithSameIdentifiers)

        assertThat(formViewModel.elements).isSameInstanceAs(originalElements)
    }

    private fun createCountryElement(
        allowedCountryCodes: Set<String>,
    ): FormElement {
        return SectionElement.wrap(
            CountryElement(
                identifier = IdentifierSpec.Country,
                controller = DropdownFieldController(
                    config = CountryConfig(allowedCountryCodes),
                    initialValue = null,
                ),
            )
        )
    }

    private fun createNameElement(): FormElement {
        return ContactInformationCollectionMode.Name.formElement(emptyMap())
    }

    private fun createEmailElement(): FormElement {
        return ContactInformationCollectionMode.Email.formElement(emptyMap())
    }

    private fun createPhoneElement(): FormElement {
        return ContactInformationCollectionMode.Phone.formElement(emptyMap())
    }

    private fun createAddressElements(
        identifier: IdentifierSpec,
        allowedCountryCodes: Set<String>,
        hideCountry: Boolean,
    ): List<FormElement> {
        val addressElement = AddressElement(
            _identifier = identifier,
            rawValuesMap = emptyMap(),
            countryCodes = allowedCountryCodes,
            addressInputMode = AddressInputMode.NoAutocomplete(),
            sameAsShippingElement = null,
            shippingValuesMap = emptyMap(),
            hideCountry = hideCountry,
        )
        return listOf(
            SectionElement.wrap(addressElement, resolvableString(R.string.stripe_billing_details))
        )
    }

    private fun createIbanElement(): FormElement {
        val identifier = IdentifierSpec.Generic("sepa_debit[iban]")
        return SectionElement.wrap(
            SimpleTextElement(
                identifier = identifier,
                controller = SimpleTextFieldController(
                    textFieldConfig = IbanConfig(),
                    initialValue = null,
                ),
            )
        )
    }

    private fun createMandateElement(): FormElement {
        return MandateTextElement(
            identifier = IdentifierSpec.Generic("mandate"),
            stringResId = R.string.stripe_sepa_mandate,
            args = listOf(""),
        )
    }

    private fun createViewModel(
        arguments: FormArguments,
        formElements: List<FormElement>,
    ) = FormViewModel(
        formArguments = arguments,
        formElements = formElements,
    ).also { viewModelStoreRule.track(it) }
}
