package com.stripe.android.paymentsheet.forms

import android.app.Application
import androidx.annotation.StringRes
import androidx.appcompat.view.ContextThemeWrapper
import androidx.test.core.app.ApplicationProvider
import app.cash.turbine.test
import app.cash.turbine.testIn
import com.google.common.truth.Truth.assertThat
import com.stripe.android.model.PaymentMethod
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.PaymentSheetFixtures.COMPOSE_FRAGMENT_ARGS
import com.stripe.android.paymentsheet.model.PaymentSelection
import com.stripe.android.paymentsheet.paymentdatacollection.FormArguments
import com.stripe.android.ui.core.BillingDetailsCollectionConfiguration
import com.stripe.android.ui.core.R
import com.stripe.android.ui.core.elements.AddressSpec
import com.stripe.android.ui.core.elements.CardDetailsSectionElement
import com.stripe.android.ui.core.elements.CountrySpec
import com.stripe.android.ui.core.elements.EmailElement
import com.stripe.android.ui.core.elements.EmailSpec
import com.stripe.android.ui.core.elements.IbanSpec
import com.stripe.android.ui.core.elements.LayoutSpec
import com.stripe.android.ui.core.elements.MandateTextSpec
import com.stripe.android.ui.core.elements.NameSpec
import com.stripe.android.ui.core.elements.PhoneSpec
import com.stripe.android.ui.core.elements.PlaceholderSpec
import com.stripe.android.ui.core.elements.SaveForFutureUseElement
import com.stripe.android.ui.core.elements.SaveForFutureUseSpec
import com.stripe.android.ui.core.forms.resources.LpmRepository
import com.stripe.android.uicore.address.AddressRepository
import com.stripe.android.uicore.elements.AddressElement
import com.stripe.android.uicore.elements.CountryElement
import com.stripe.android.uicore.elements.IdentifierSpec
import com.stripe.android.uicore.elements.PhoneNumberElement
import com.stripe.android.uicore.elements.RowElement
import com.stripe.android.uicore.elements.SectionElement
import com.stripe.android.uicore.elements.SectionSingleFieldElement
import com.stripe.android.uicore.elements.SimpleTextElement
import com.stripe.android.uicore.elements.SimpleTextFieldController
import com.stripe.android.uicore.elements.TextFieldController
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.robolectric.RobolectricTestRunner
import com.stripe.android.R as StripeR
import com.stripe.android.core.R as CoreR
import com.stripe.android.uicore.R as UiCoreR

@Suppress("LargeClass")
@RunWith(RobolectricTestRunner::class)
internal class FormViewModelTest {
    private val emailSection = EmailSpec()
    private val context = ContextThemeWrapper(
        ApplicationProvider.getApplicationContext(),
        StripeR.style.StripeDefaultTheme
    )
    val lpmRepository = LpmRepository(LpmRepository.LpmRepositoryArguments(context.resources))

    val showCheckboxFlow = MutableStateFlow(false)

    private fun createLpmRepositorySupportedPaymentMethod(
        paymentMethodType: PaymentMethod.Type,
        layoutSpec: LayoutSpec
    ): LpmRepository {
        val mockLpmRepository = mock<LpmRepository>()

        whenever(mockLpmRepository.fromCode(paymentMethodType.code)).thenReturn(
            LpmRepository.SupportedPaymentMethod(
                code = paymentMethodType.code,
                requiresMandate = false,
                displayNameResource = R.string.stripe_paymentsheet_payment_method_card,
                iconResource = R.drawable.stripe_ic_paymentsheet_pm_card,
                lightThemeIconUrl = null,
                darkThemeIconUrl = null,
                tintIconOnSelection = true,
                requirement = PaymentMethodRequirements(emptySet(), emptySet(), true),
                formSpec = layoutSpec
            )
        )
        return mockLpmRepository
    }

    @Test
    fun `Verify setting save for future use value is updated in flowable`() = runTest {
        val args = COMPOSE_FRAGMENT_ARGS.copy(
            paymentMethodCode = PaymentMethod.Type.Card.code
        )
        val formViewModel = createViewModel(
            args,
            createLpmRepositorySupportedPaymentMethod(
                PaymentMethod.Type.Card,
                LayoutSpec.create(
                    EmailSpec(),
                    SaveForFutureUseSpec()
                )
            )
        )

        showCheckboxFlow.emit(true)

        // Set all the card fields, billing is set in the args
        val emailController =
            getSectionFieldTextControllerWithLabel(formViewModel, UiCoreR.string.stripe_email)

        emailController?.onValueChange("joe@email.com")

        assertThat(
            formViewModel.completeFormValues.first()?.fieldValuePairs?.get(IdentifierSpec.SaveForFutureUse)?.value
        ).isNotNull()

        val receiver = formViewModel.saveForFutureUse.testIn(this)
        assertThat(receiver.awaitItem()).isTrue()

        assertThat(
            formViewModel.completeFormValues.first()?.fieldValuePairs?.get(IdentifierSpec.SaveForFutureUse)?.value
        ).isEqualTo("true")

        formViewModel.setSaveForFutureUse(false)

        assertThat(receiver.awaitItem()).isFalse()

        assertThat(
            formViewModel.completeFormValues.first()?.fieldValuePairs?.get(IdentifierSpec.SaveForFutureUse)?.value
        ).isEqualTo("false")

        receiver.cancel()
    }

    @Test
    fun `Verify setting save for future use visibility removes it from completed values`() =
        runTest {
            val args = COMPOSE_FRAGMENT_ARGS.copy(
                paymentMethodCode = PaymentMethod.Type.Card.code
            )
            val formViewModel = createViewModel(
                args,
                createLpmRepositorySupportedPaymentMethod(
                    PaymentMethod.Type.Card,
                    LayoutSpec.create(
                        SaveForFutureUseSpec()
                    )
                )
            )

            formViewModel.hiddenIdentifiers.test {
                assertThat(awaitItem()).containsExactly(IdentifierSpec.SaveForFutureUse)

                showCheckboxFlow.tryEmit(true)
                assertThat(awaitItem()).isEmpty()

                showCheckboxFlow.tryEmit(false)
                assertThat(awaitItem()).containsExactly(IdentifierSpec.SaveForFutureUse)
            }
        }

    @Test
    fun `Verify if there are no text fields, there is no last text field id`() = runTest {
        // Here we have just a country, no text fields.
        val args = COMPOSE_FRAGMENT_ARGS.copy(
            paymentMethodCode = PaymentMethod.Type.Card.code
        )
        val formViewModel = createViewModel(
            args,
            createLpmRepositorySupportedPaymentMethod(
                PaymentMethod.Type.Card,
                LayoutSpec.create(
                    CountrySpec()
                )
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
                createLpmRepositorySupportedPaymentMethod(
                    PaymentMethod.Type.P24,
                    LayoutSpec.create(
                        NameSpec(),
                        EmailSpec(),
                        CountrySpec()
                    )
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
            createLpmRepositorySupportedPaymentMethod(
                PaymentMethod.Type.Card,
                LayoutSpec.create(
                    EmailSpec(),
                    CountrySpec()
                )
            )
        )

        val emailController =
            getSectionFieldTextControllerWithLabel(formViewModel, UiCoreR.string.stripe_email)

        // Add text to the name to make it valid
        emailController?.onValueChange("email@valid.com")

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
                createLpmRepositorySupportedPaymentMethod(
                    PaymentMethod.Type.Card,
                    LayoutSpec.create(
                        EmailSpec(),
                        CountrySpec(),
                        SaveForFutureUseSpec()
                    )
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
            showCheckbox = true,
            showCheckboxControlledFields = true
        )
        val formViewModel = createViewModel(
            args,
            createLpmRepositorySupportedPaymentMethod(
                PaymentMethod.Type.P24,
                LayoutSpec.create(
                    NameSpec(),
                    EmailSpec(),
                    CountrySpec(
                        allowedCountryCodes = setOf(
                            "AT",
                            "BE",
                            "DE",
                            "ES",
                            "IT",
                            "NL"
                        )
                    ),
                    SaveForFutureUseSpec()
                )
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

        emailElement?.onValueChange("joe@gmail.com")
        assertThat(
            formViewModel.completeFormValues.first()?.fieldValuePairs?.get(IdentifierSpec.Email)
                ?.value
        ).isEqualTo("joe@gmail.com")
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
            showCheckbox = false,
            showCheckboxControlledFields = true,
            billingDetails = null
        )
        val formViewModel = createViewModel(
            args,
            createLpmRepositorySupportedPaymentMethod(
                PaymentMethod.Type.SepaDebit,
                LayoutSpec.create(
                    NameSpec(),
                    EmailSpec(),
                    IbanSpec(),
                    AddressSpec(
                        IdentifierSpec.Generic("address"),
                        allowedCountryCodes = setOf("US", "JP")
                    ),
                    MandateTextSpec(
                        IdentifierSpec.Generic("mandate"),
                        R.string.stripe_sepa_mandate
                    )
                )
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
        )?.onValueChange("joe@gmail.com")
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
            if (index == addressControllers.controllers.size - 1) {
                assertThat(
                    formViewModel
                        .completeFormValues
                        .first()
                        ?.fieldValuePairs
                        ?.get(emailSection.apiPath)
                        ?.value
                ).isNotNull()
            } else {
                assertThat(
                    formViewModel
                        .completeFormValues
                        .first()
                        ?.fieldValuePairs
                        ?.get(emailSection.apiPath)
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
        val formViewModel = createViewModel(
            args,
            createLpmRepositorySupportedPaymentMethod(
                PaymentMethod.Type.SepaDebit,
                LayoutSpec.create(
                    NameSpec(),
                    EmailSpec(),
                    IbanSpec(),
                    AddressSpec(
                        IdentifierSpec.Generic("address"),
                        allowedCountryCodes = setOf("US", "JP")
                    ),
                    MandateTextSpec(
                        IdentifierSpec.Generic("mandate"),
                        R.string.stripe_sepa_mandate
                    )
                )
            )
        )

        getSectionFieldTextControllerWithLabel(
            formViewModel,
            CoreR.string.stripe_address_label_full_name
        )?.onValueChange("joe")
        assertThat(
            formViewModel.completeFormValues.first()?.fieldValuePairs?.get(emailSection.apiPath)
                ?.value
        ).isNull()

        getSectionFieldTextControllerWithLabel(
            formViewModel,
            UiCoreR.string.stripe_email
        )?.onValueChange("joe@gmail.com")
        assertThat(
            formViewModel.completeFormValues.first()?.fieldValuePairs?.get(emailSection.apiPath)
                ?.value
        ).isNull()

        getSectionFieldTextControllerWithLabel(
            formViewModel,
            R.string.stripe_iban
        )?.onValueChange("DE89370400440532013000")
        assertThat(
            formViewModel.completeFormValues.first()?.fieldValuePairs?.get(emailSection.apiPath)
                ?.value
        ).isNull()

        // Fill all address values except line2
        val addressControllers = AddressControllers.create(formViewModel)
        val populateAddressControllers = addressControllers.controllers
            .filter { it.label.first() != UiCoreR.string.stripe_address_label_address_line2 }
        populateAddressControllers
            .forEachIndexed { index, textFieldController ->
                textFieldController.onValueChange("12345")

                if (index == populateAddressControllers.size - 1) {
                    assertThat(
                        formViewModel
                            .completeFormValues
                            .first()
                            ?.fieldValuePairs
                            ?.get(emailSection.apiPath)
                            ?.value
                    ).isNotNull()
                } else {
                    assertThat(
                        formViewModel
                            .completeFormValues
                            .first()
                            ?.fieldValuePairs
                            ?.get(emailSection.apiPath)
                            ?.value
                    ).isNull()
                }
            }
    }

    @Test
    fun `Test default values are filled`() {
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
            createLpmRepositorySupportedPaymentMethod(
                PaymentMethod.Type.Card,
                LpmRepository.HardcodedCard.formSpec,
            )
        )

        assertThat(viewModel.defaultValuesToInclude).containsExactlyEntriesIn(
            mapOf(
                IdentifierSpec.Name to "Jenny Rosen",
                IdentifierSpec.Email to "mail@mail.com",
                IdentifierSpec.Phone to "+13105551234",
                IdentifierSpec.Line1 to "123 Main Street",
                IdentifierSpec.Line2 to "456",
                IdentifierSpec.City to "San Francisco",
                IdentifierSpec.State to "CA",
                IdentifierSpec.Country to "US",
                IdentifierSpec.PostalCode to "94111",
            )
        )
    }

    @Test
    fun `Test only provided default values are filled`() {
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
            createLpmRepositorySupportedPaymentMethod(
                PaymentMethod.Type.Card,
                LpmRepository.HardcodedCard.formSpec,
            )
        )

        assertThat(viewModel.defaultValuesToInclude).containsExactlyEntriesIn(
            mapOf(
                IdentifierSpec.Name to "Jenny Rosen",
                IdentifierSpec.Email to "mail@mail.com",
                IdentifierSpec.Country to "US",
                IdentifierSpec.PostalCode to "94111",
            )
        )
    }

    @Test
    fun `Test default values are not filled`() {
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
            createLpmRepositorySupportedPaymentMethod(
                PaymentMethod.Type.Card,
                LpmRepository.HardcodedCard.formSpec,
            )
        )

        assertThat(viewModel.defaultValuesToInclude).isEmpty()
    }

    @Test
    fun `Test placeholder specs are transformed correctly`() = runBlocking {
        val billingDetailsCollectionConfiguration = PaymentSheet.BillingDetailsCollectionConfiguration(
            name = PaymentSheet.BillingDetailsCollectionConfiguration.CollectionMode.Always,
            email = PaymentSheet.BillingDetailsCollectionConfiguration.CollectionMode.Always,
            phone = PaymentSheet.BillingDetailsCollectionConfiguration.CollectionMode.Always,
            address = PaymentSheet.BillingDetailsCollectionConfiguration.AddressCollectionMode.Full,
            attachDefaultsToPaymentMethod = false,
        )
        val specs = listOf(
            PlaceholderSpec(field = PlaceholderSpec.PlaceholderField.Name),
            PlaceholderSpec(field = PlaceholderSpec.PlaceholderField.Email),
            PlaceholderSpec(field = PlaceholderSpec.PlaceholderField.Phone),
            PlaceholderSpec(field = PlaceholderSpec.PlaceholderField.BillingAddress),
        )

        val args = COMPOSE_FRAGMENT_ARGS.copy(
            billingDetailsCollectionConfiguration = billingDetailsCollectionConfiguration,
        )
        val formViewModel = createViewModel(
            args,
            createLpmRepositorySupportedPaymentMethod(
                PaymentMethod.Type.Bancontact,
                LayoutSpec(specs),
            )
        )
        val formElement = formViewModel.elementsFlow.first()

        val nameSection = formElement[0] as SectionElement
        val nameElement = nameSection.fields[0] as SimpleTextElement
        assertThat(nameElement.controller.label.first()).isEqualTo(CoreR.string.stripe_address_label_full_name)
        assertThat(nameElement.identifier.v1).isEqualTo("billing_details[name]")

        val emailSection = formElement[1] as SectionElement
        val emailElement = emailSection.fields[0] as EmailElement
        assertThat(emailElement.controller.label.first()).isEqualTo(UiCoreR.string.stripe_email)
        assertThat(emailElement.identifier.v1).isEqualTo("billing_details[email]")

        val phoneSection = formElement[2] as SectionElement
        val phoneElement = phoneSection.fields[0] as PhoneNumberElement
        assertThat(phoneElement.controller.label.first()).isEqualTo(CoreR.string.stripe_address_label_phone_number)
        assertThat(phoneElement.identifier.v1).isEqualTo("billing_details[phone]")

        val addressSection = formElement[3] as SectionElement
        val addressElement = addressSection.fields[0] as AddressElement

        val identifiers = addressElement.fields.first().map { it.identifier }
        // Check that the address element contains country.
        assertThat(identifiers).contains(IdentifierSpec.Country)
    }

    @Test
    fun `Test address without country placeholder produces correct element`() = runBlocking {
        val billingDetailsCollectionConfiguration = PaymentSheet.BillingDetailsCollectionConfiguration(
            name = PaymentSheet.BillingDetailsCollectionConfiguration.CollectionMode.Always,
            email = PaymentSheet.BillingDetailsCollectionConfiguration.CollectionMode.Always,
            phone = PaymentSheet.BillingDetailsCollectionConfiguration.CollectionMode.Always,
            address = PaymentSheet.BillingDetailsCollectionConfiguration.AddressCollectionMode.Full,
            attachDefaultsToPaymentMethod = false,
        )

        val args = COMPOSE_FRAGMENT_ARGS.copy(
            billingDetailsCollectionConfiguration = billingDetailsCollectionConfiguration,
        )
        val formViewModel = createViewModel(
            args,
            createLpmRepositorySupportedPaymentMethod(
                PaymentMethod.Type.Bancontact,
                LayoutSpec(
                    listOf(
                        PlaceholderSpec(field = PlaceholderSpec.PlaceholderField.BillingAddressWithoutCountry)
                    )
                ),
            )
        )
        val formElement = formViewModel.elementsFlow.first()

        val addressSection = formElement.first() as SectionElement
        val addressElement = addressSection.fields[0] as AddressElement
        val identifiers = addressElement.fields.first().map { it.identifier }
        // Check that the address element doesn't contain country.
        assertThat(identifiers).doesNotContain(IdentifierSpec.Country)
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

            val internalBillingDetailsCollectionConfig = BillingDetailsCollectionConfiguration(
                collectName = true,
                collectEmail = true,
                collectPhone = true,
                address = BillingDetailsCollectionConfiguration.AddressCollectionMode.Full,
            )

            val args = COMPOSE_FRAGMENT_ARGS.copy(
                PaymentMethod.Type.Card.code,
                billingDetailsCollectionConfiguration = billingDetailsCollectionConfiguration,
                billingDetails = PaymentSheet.BillingDetails(),
            )

            val formViewModel = createViewModel(
                args,
                createLpmRepositorySupportedPaymentMethod(
                    PaymentMethod.Type.Card,
                    LpmRepository.hardcodedCardSpec(internalBillingDetailsCollectionConfig).formSpec,
                ),
            )

            val elements = formViewModel.elementsFlow.first()
            val countryElement = elements
                .filterIsInstance<SectionElement>()
                .flatMap { it.fields }
                .filterIsInstance<AddressElement>()
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
                PaymentMethod.Type.Sofort.code,
                billingDetailsCollectionConfiguration = billingDetailsCollectionConfiguration,
                billingDetails = PaymentSheet.BillingDetails(),
            )
            val formViewModel = createViewModel(
                args,
                createLpmRepositorySupportedPaymentMethod(
                    PaymentMethod.Type.Sofort,
                    LayoutSpec(
                        listOf(
                            NameSpec(),
                            EmailSpec(),
                            PhoneSpec(),
                            CountrySpec(),
                            AddressSpec(hideCountry = true),
                        ),
                    )
                ),
            )

            val elements = formViewModel.elementsFlow.first()
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

    @Test
    fun `Test viewData flow`() = runTest {
        val formViewModel = createViewModel(
            COMPOSE_FRAGMENT_ARGS.copy(
                paymentMethodCode = PaymentMethod.Type.Card.code,
            ),
            createLpmRepositorySupportedPaymentMethod(
                PaymentMethod.Type.Card,
                LpmRepository.HardcodedCard.formSpec,
            ),
        )

        formViewModel.viewDataFlow.test {
            val viewData = awaitItem()
            assertThat(viewData.elements.first())
                .isInstanceOf(CardDetailsSectionElement::class.java)
            assertThat(viewData.completeFormValues)
                .isNull()
            assertThat(viewData.hiddenIdentifiers)
                .doesNotContain(IdentifierSpec("test"))
            assertThat(viewData.lastTextFieldIdentifier)
                .isNotNull()

            formViewModel.addHiddenIdentifiers(
                setOf(IdentifierSpec("test"))
            )

            assertThat(expectMostRecentItem().hiddenIdentifiers)
                .contains(IdentifierSpec("test"))
        }
    }

    private suspend fun getSectionFieldTextControllerWithLabel(
        formViewModel: FormViewModel,
        @StringRes label: Int
    ) =
        formViewModel.elementsFlow.first()
            .filterIsInstance<SectionElement>()
            .flatMap { it.fields }
            .filterIsInstance<SectionSingleFieldElement>()
            .map { it.controller }
            .filterIsInstance<TextFieldController>()
            .firstOrNull { it.label.first() == label }

    private data class AddressControllers(
        val controllers: List<TextFieldController>
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
                            CoreR.string.stripe_address_label_state
                        ),
                        getAddressSectionTextControllerWithLabel(
                            formViewModel,
                            CoreR.string.stripe_address_label_zip_code
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
            val addressElementFields = formViewModel.elementsFlow.first()
                .filterIsInstance<SectionElement>()
                .flatMap { it.fields }
                .filterIsInstance<AddressElement>()
                .firstOrNull()
                ?.fields
                ?.first()
            return addressElementFields
                ?.filterIsInstance<SectionSingleFieldElement>()
                ?.map { (it.controller as? SimpleTextFieldController) }
                ?.firstOrNull { it?.label?.first() == label }
                ?: addressElementFields
                    ?.asSequence()
                    ?.filterIsInstance<RowElement>()
                    ?.map { it.fields }
                    ?.flatten()
                    ?.map { (it.controller as? SimpleTextFieldController) }
                    ?.firstOrNull { it?.label?.first() == label }
        }
    }

    fun createViewModel(
        arguments: FormArguments,
        lpmRepository: LpmRepository
    ) = FormViewModel(
        context = context,
        formArguments = arguments,
        lpmRepository = lpmRepository,
        addressRepository = createAddressRepository(),
        showCheckboxFlow = showCheckboxFlow
    )
}

internal suspend fun FormViewModel.setSaveForFutureUse(value: Boolean) {
    elementsFlow
        .firstOrNull()
        ?.filterIsInstance<SaveForFutureUseElement>()
        ?.firstOrNull()?.controller?.onValueChange(value)
}

private fun createAddressRepository(): AddressRepository {
    return AddressRepository(
        resources = ApplicationProvider.getApplicationContext<Application>().resources,
        workContext = Dispatchers.Unconfined,
    )
}
