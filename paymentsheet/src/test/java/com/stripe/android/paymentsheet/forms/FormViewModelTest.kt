package com.stripe.android.paymentsheet.forms

import android.app.Application
import androidx.annotation.StringRes
import androidx.appcompat.view.ContextThemeWrapper
import androidx.test.core.app.ApplicationProvider
import app.cash.turbine.test
import app.cash.turbine.testIn
import com.google.common.truth.Truth.assertThat
import com.stripe.android.model.PaymentMethod
import com.stripe.android.model.PaymentMethodCode
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.PaymentSheetFixtures.ARGS_WITHOUT_CONFIG
import com.stripe.android.paymentsheet.PaymentSheetFixtures.COMPOSE_FRAGMENT_ARGS
import com.stripe.android.paymentsheet.PaymentSheetFixtures.CONFIG_MINIMUM
import com.stripe.android.paymentsheet.model.PaymentSelection
import com.stripe.android.paymentsheet.paymentdatacollection.FormArguments
import com.stripe.android.ui.core.BillingDetailsCollectionConfiguration
import com.stripe.android.ui.core.R
import com.stripe.android.ui.core.elements.AddressSpec
import com.stripe.android.ui.core.elements.CardBillingAddressElement
import com.stripe.android.ui.core.elements.CardDetailsSectionElement
import com.stripe.android.ui.core.elements.CountrySpec
import com.stripe.android.ui.core.elements.EmailSpec
import com.stripe.android.ui.core.elements.IbanSpec
import com.stripe.android.ui.core.elements.LayoutSpec
import com.stripe.android.ui.core.elements.MandateTextSpec
import com.stripe.android.ui.core.elements.NameSpec
import com.stripe.android.ui.core.elements.SaveForFutureUseElement
import com.stripe.android.ui.core.elements.SaveForFutureUseSpec
import com.stripe.android.ui.core.forms.resources.LpmRepository
import com.stripe.android.uicore.address.AddressRepository
import com.stripe.android.uicore.elements.AddressElement
import com.stripe.android.uicore.elements.IdentifierSpec
import com.stripe.android.uicore.elements.RowElement
import com.stripe.android.uicore.elements.SectionElement
import com.stripe.android.uicore.elements.SectionSingleFieldElement
import com.stripe.android.uicore.elements.SimpleTextFieldController
import com.stripe.android.uicore.elements.TextFieldController
import com.stripe.android.uicore.forms.FormFieldEntry
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
internal class FormViewModelTest {
    private val emailSection = EmailSpec()
    private val context = ContextThemeWrapper(
        ApplicationProvider.getApplicationContext(),
        R.style.StripeDefaultTheme
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
                paymentMethodType.code,
                false,
                R.string.stripe_paymentsheet_payment_method_card,
                R.drawable.stripe_ic_paymentsheet_pm_card,
                null,
                null,
                true,
                PaymentMethodRequirements(emptySet(), emptySet(), true),
                layoutSpec
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
            getSectionFieldTextControllerWithLabel(formViewModel, R.string.email)

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
            getSectionFieldTextControllerWithLabel(formViewModel, R.string.email)

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
                getSectionFieldTextControllerWithLabel(formViewModel, R.string.email)

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
            getSectionFieldTextControllerWithLabel(formViewModel, R.string.address_label_full_name)
        val emailElement =
            getSectionFieldTextControllerWithLabel(formViewModel, R.string.email)

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
                        R.string.sepa_mandate
                    )
                )
            )
        )

        getSectionFieldTextControllerWithLabel(
            formViewModel,
            R.string.address_label_full_name
        )?.onValueChange("joe")
        assertThat(
            formViewModel.completeFormValues.first()?.fieldValuePairs?.get(IdentifierSpec.Name)
                ?.value
        ).isNull()

        getSectionFieldTextControllerWithLabel(
            formViewModel,
            R.string.email
        )?.onValueChange("joe@gmail.com")
        assertThat(
            formViewModel.completeFormValues.first()?.fieldValuePairs?.get(IdentifierSpec.Email)
                ?.value
        ).isNull()

        getSectionFieldTextControllerWithLabel(
            formViewModel,
            R.string.iban
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
                        R.string.sepa_mandate
                    )
                )
            )
        )

        getSectionFieldTextControllerWithLabel(
            formViewModel,
            R.string.address_label_full_name
        )?.onValueChange("joe")
        assertThat(
            formViewModel.completeFormValues.first()?.fieldValuePairs?.get(emailSection.apiPath)
                ?.value
        ).isNull()

        getSectionFieldTextControllerWithLabel(
            formViewModel,
            R.string.email
        )?.onValueChange("joe@gmail.com")
        assertThat(
            formViewModel.completeFormValues.first()?.fieldValuePairs?.get(emailSection.apiPath)
                ?.value
        ).isNull()

        getSectionFieldTextControllerWithLabel(
            formViewModel,
            R.string.iban
        )?.onValueChange("DE89370400440532013000")
        assertThat(
            formViewModel.completeFormValues.first()?.fieldValuePairs?.get(emailSection.apiPath)
                ?.value
        ).isNull()

        // Fill all address values except line2
        val addressControllers = AddressControllers.create(formViewModel)
        val populateAddressControllers = addressControllers.controllers
            .filter { it.label.first() != R.string.address_label_address_line2 }
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
    fun `Verify defaults are propagated`() = runTest {
        val billingDetailsCollectionConfiguration = BillingDetailsCollectionConfiguration(
            name = BillingDetailsCollectionConfiguration.CollectionMode.Never,
            email = BillingDetailsCollectionConfiguration.CollectionMode.Never,
            phone = BillingDetailsCollectionConfiguration.CollectionMode.Never,
            address = BillingDetailsCollectionConfiguration.AddressCollectionMode.Never,
            attachDefaultsToPaymentMethod = true,
        )
        val args = COMPOSE_FRAGMENT_ARGS.copy(
            paymentMethodCode = PaymentMethod.Type.Card.code,
            billingDetails = PaymentSheet.BillingDetails(
                name = "Jenny Rosen",
                email = "foo@bar.com",
                phone = "+13105551234",
                address = PaymentSheet.Address(
                    line1 = "123 Main Street",
                    city = "San Francisco",
                    state = "CA",
                    postalCode = "94111",
                    country = "US",
                ),
            ),
            billingDetailsCollectionConfiguration = billingDetailsCollectionConfiguration,
        )
        val formViewModel = createViewModel(
            args,
            createLpmRepositorySupportedPaymentMethod(
                PaymentMethod.Type.Card,
                LpmRepository.hardcodedCardSpec(billingDetailsCollectionConfiguration).formSpec,
            )
        )

        val cardDetailsController = formViewModel.elementsFlow.first()
            .filterIsInstance<CardDetailsSectionElement>()
            .first()?.controller!!
        cardDetailsController.setCardNumber("4242424242424242")
        cardDetailsController.setExpirationDate("130")
        cardDetailsController.setCVC("123")

        formViewModel.completeFormValues.test {
            assertThat(awaitItem()?.fieldValuePairs).containsExactlyEntriesIn(
                mapOf(
                    IdentifierSpec.Name to FormFieldEntry("Jenny Rosen", true),
                    IdentifierSpec.Email to FormFieldEntry("foo@bar.com", true),
                    IdentifierSpec.Phone to FormFieldEntry("+13105551234", true),
                    IdentifierSpec.Line1 to FormFieldEntry("123 Main Street", true),
                    IdentifierSpec.City to FormFieldEntry("San Francisco", true),
                    IdentifierSpec.State to FormFieldEntry("CA", true),
                    IdentifierSpec.PostalCode to FormFieldEntry("94111", true),
                    IdentifierSpec.Country to FormFieldEntry("US", true),
                    IdentifierSpec.CardNumber to FormFieldEntry("4242424242424242", true),
                    IdentifierSpec.CardExpMonth to FormFieldEntry("01", true),
                    IdentifierSpec.CardExpYear to FormFieldEntry("2030", true),
                    IdentifierSpec.CardCvc to FormFieldEntry("123", true),
                    IdentifierSpec.CardBrand to FormFieldEntry("visa", true),
                )
            )
        }
    }

    @Test
    fun `Verify defaults are not propagated`() = runTest {
        val billingDetailsCollectionConfiguration = BillingDetailsCollectionConfiguration(
            name = BillingDetailsCollectionConfiguration.CollectionMode.Never,
            email = BillingDetailsCollectionConfiguration.CollectionMode.Never,
            phone = BillingDetailsCollectionConfiguration.CollectionMode.Never,
            address = BillingDetailsCollectionConfiguration.AddressCollectionMode.Never,
            attachDefaultsToPaymentMethod = false,
        )
        val args = COMPOSE_FRAGMENT_ARGS.copy(
            paymentMethodCode = PaymentMethod.Type.Card.code,
            billingDetails = PaymentSheet.BillingDetails(
                name = "Jenny Rosen",
                email = "foo@bar.com",
                phone = "+13105551234",
                address = PaymentSheet.Address(
                    line1 = "123 Main Street",
                    city = "San Francisco",
                    state = "CA",
                    postalCode = "94111",
                    country = "US",
                ),
            ),
            billingDetailsCollectionConfiguration = billingDetailsCollectionConfiguration,
        )
        val formViewModel = createViewModel(
            args,
            createLpmRepositorySupportedPaymentMethod(
                PaymentMethod.Type.Card,
                LpmRepository.hardcodedCardSpec(billingDetailsCollectionConfiguration).formSpec,
            )
        )

        val elements = formViewModel.elementsFlow.first()
        val cardDetailsController = elements
            .filterIsInstance<CardDetailsSectionElement>()
            .first()?.controller!!
        cardDetailsController.setCardNumber("4242424242424242")
        cardDetailsController.setExpirationDate("130")
        cardDetailsController.setCVC("123")

        formViewModel.completeFormValues.test {
            assertThat(awaitItem()?.fieldValuePairs).containsExactlyEntriesIn(
                mapOf(
                    IdentifierSpec.CardNumber to FormFieldEntry("4242424242424242", true),
                    IdentifierSpec.CardExpMonth to FormFieldEntry("01", true),
                    IdentifierSpec.CardExpYear to FormFieldEntry("2030", true),
                    IdentifierSpec.CardCvc to FormFieldEntry("123", true),
                    IdentifierSpec.CardBrand to FormFieldEntry("visa", true),
                )
            )
        }
    }

    @Test
    fun `Verify defaults are overridden`() = runTest {
        val billingDetailsCollectionConfiguration = BillingDetailsCollectionConfiguration(
            name = BillingDetailsCollectionConfiguration.CollectionMode.Always,
//            email = BillingDetailsCollectionConfiguration.CollectionMode.Always,
//            phone = BillingDetailsCollectionConfiguration.CollectionMode.Always,
            address = BillingDetailsCollectionConfiguration.AddressCollectionMode.Full,
            attachDefaultsToPaymentMethod = true,
        )
        val args = COMPOSE_FRAGMENT_ARGS.copy(
            paymentMethodCode = PaymentMethod.Type.Card.code,
            billingDetails = PaymentSheet.BillingDetails(
                name = "Jenny Rosen",
//                email = "foo@bar.com",
//                phone = "+13105551234",
                address = PaymentSheet.Address(
                    line1 = "123 Main Street",
                    city = "San Francisco",
                    state = "CA",
                    postalCode = "94111",
                    country = "US",
                ),
            ),
            billingDetailsCollectionConfiguration = billingDetailsCollectionConfiguration,
        )
        val formViewModel = createViewModel(
            args,
            createLpmRepositorySupportedPaymentMethod(
                PaymentMethod.Type.Card,
                LpmRepository.hardcodedCardSpec(billingDetailsCollectionConfiguration).formSpec,
            )
        )

        val cardDetailsController = formViewModel.elementsFlow.first()
            .filterIsInstance<CardDetailsSectionElement>()
            .first()?.controller!!
        cardDetailsController.setName("Jane Doe")
        cardDetailsController.setCardNumber("4242424242424242")
        cardDetailsController.setExpirationDate("130")
        cardDetailsController.setCVC("123")
//        getAddressSectionTextControllerWithLabel(
//            formViewModel,
//            R.string.email
//        )?.onValueChange("mail@example.com")
//        getAddressSectionTextControllerWithLabel(
//            formViewModel,
//            R.string.address_label_phone_number,
//        )?.onValueChange("+14155554321")
        getCardAddressSectionTextControllerWithLabel(
            formViewModel,
            R.string.address_label_address_line1,
        )?.onValueChange("123 Fake St.")
        getCardAddressSectionTextControllerWithLabel(
            formViewModel,
            R.string.address_label_address_line2,
        )?.onValueChange("Line 2")
        getCardAddressSectionTextControllerWithLabel(
            formViewModel,
            R.string.address_label_city,
        )?.onValueChange("New York")
        getCardAddressSectionTextControllerWithLabel(
            formViewModel,
            R.string.address_label_zip_code,
        )?.onValueChange("10001")

        formViewModel.completeFormValues.test {
            assertThat(awaitItem()?.fieldValuePairs).containsExactlyEntriesIn(
                mapOf(
                    IdentifierSpec.Name to FormFieldEntry("Jane Doe", true),
                    IdentifierSpec.Email to FormFieldEntry("mail@example.com", true),
                    IdentifierSpec.Phone to FormFieldEntry("+14155554321", true),
                    IdentifierSpec.Line1 to FormFieldEntry("123 Fake St.", true),
                    IdentifierSpec.City to FormFieldEntry("New York", true),
                    IdentifierSpec.State to FormFieldEntry("NY", true),
                    IdentifierSpec.PostalCode to FormFieldEntry("10001", true),
                    IdentifierSpec.Country to FormFieldEntry("US", true),
                    IdentifierSpec.CardNumber to FormFieldEntry("4242424242424242", true),
                    IdentifierSpec.CardExpMonth to FormFieldEntry("01", true),
                    IdentifierSpec.CardExpYear to FormFieldEntry("2030", true),
                    IdentifierSpec.CardCvc to FormFieldEntry("123", true),
                    IdentifierSpec.CardBrand to FormFieldEntry("visa", true),
                )
            )
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

    private suspend fun getCardAddressSectionTextControllerWithLabel(
        formViewModel: FormViewModel,
        @StringRes label: Int
    ): TextFieldController? {
        val addressElementFields = formViewModel.elementsFlow.first()
            .filterIsInstance<SectionElement>()
            .flatMap { it.fields }
            .filterIsInstance<CardBillingAddressElement>()
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

    private data class AddressControllers(
        val controllers: List<TextFieldController>
    ) {
        companion object {
            suspend fun create(formViewModel: FormViewModel) =
                AddressControllers(
                    listOfNotNull(
                        getAddressSectionTextControllerWithLabel(
                            formViewModel,
                            R.string.address_label_address_line1
                        ),
                        getAddressSectionTextControllerWithLabel(
                            formViewModel,
                            R.string.address_label_address_line2
                        ),
                        getAddressSectionTextControllerWithLabel(
                            formViewModel,
                            R.string.address_label_city
                        ),
                        getAddressSectionTextControllerWithLabel(
                            formViewModel,
                            R.string.address_label_state
                        ),
                        getAddressSectionTextControllerWithLabel(
                            formViewModel,
                            R.string.address_label_zip_code
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
