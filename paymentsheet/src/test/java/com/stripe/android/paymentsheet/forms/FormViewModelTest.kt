package com.stripe.android.paymentsheet.forms

import android.content.Context
import androidx.annotation.StringRes
import androidx.appcompat.view.ContextThemeWrapper
import androidx.lifecycle.asLiveData
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import com.stripe.android.model.PaymentMethod
import com.stripe.android.paymentsheet.PaymentSheetFixtures.COMPOSE_FRAGMENT_ARGS
import com.stripe.android.paymentsheet.model.PaymentSelection
import com.stripe.android.paymentsheet.paymentdatacollection.FormArguments
import com.stripe.android.ui.core.R
import com.stripe.android.ui.core.address.AddressRepository
import com.stripe.android.ui.core.elements.AddressElement
import com.stripe.android.ui.core.elements.AddressSpec
import com.stripe.android.ui.core.elements.CountrySpec
import com.stripe.android.ui.core.elements.EmailSpec
import com.stripe.android.ui.core.elements.IbanSpec
import com.stripe.android.ui.core.elements.IdentifierSpec
import com.stripe.android.ui.core.elements.LayoutSpec
import com.stripe.android.ui.core.elements.MandateTextSpec
import com.stripe.android.ui.core.elements.NameSpec
import com.stripe.android.ui.core.elements.RowElement
import com.stripe.android.ui.core.elements.SaveForFutureUseElement
import com.stripe.android.ui.core.elements.SaveForFutureUseSpec
import com.stripe.android.ui.core.elements.SectionElement
import com.stripe.android.ui.core.elements.SectionSingleFieldElement
import com.stripe.android.ui.core.elements.SimpleTextFieldController
import com.stripe.android.ui.core.elements.TextFieldController
import com.stripe.android.ui.core.forms.resources.LpmRepository
import com.stripe.android.ui.core.forms.resources.ResourceRepository
import com.stripe.android.ui.core.forms.resources.StaticAddressResourceRepository
import com.stripe.android.ui.core.forms.resources.StaticLpmResourceRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.robolectric.RobolectricTestRunner
import org.robolectric.shadows.ShadowLooper

@RunWith(RobolectricTestRunner::class)
internal class FormViewModelTest {
    private val emailSection = EmailSpec()
    private val context = ContextThemeWrapper(
        ApplicationProvider.getApplicationContext(),
        R.style.StripeDefaultTheme
    )
    val lpmRepository = LpmRepository(LpmRepository.LpmRepositoryArguments(context.resources))

    private val addressResourceRepository = StaticAddressResourceRepository(
        AddressRepository(
            ApplicationProvider.getApplicationContext<Context>().resources
        )
    )
    val showCheckboxFlow = MutableStateFlow(false)

    private fun createLpmRepositorySupportedPaymentMethod(
        paymentMethodType: PaymentMethod.Type,
        layoutSpec: LayoutSpec
    ): StaticLpmResourceRepository {
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
        return StaticLpmResourceRepository(
            mockLpmRepository
        )
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

        val values = mutableListOf<Boolean?>()
        formViewModel.saveForFutureUse.asLiveData()
            .observeForever {
                values.add(it)
            }
        assertThat(
            formViewModel.completeFormValues.first()?.fieldValuePairs?.get(IdentifierSpec.SaveForFutureUse)?.value
        ).isEqualTo("true")

        formViewModel.setSaveForFutureUse(false)

        assertThat(values[1]).isFalse()

        assertThat(
            formViewModel.completeFormValues.first()?.fieldValuePairs?.get(IdentifierSpec.SaveForFutureUse)?.value
        ).isEqualTo("false")
    }

    @Test
    fun `Verify setting save for future use visibility removes it from completed values`() {
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
        showCheckboxFlow.tryEmit(true)

        val values = mutableListOf<Set<IdentifierSpec>>()
        formViewModel.hiddenIdentifiers.asLiveData()
            .observeForever {
                values.add(it)
            }
        assertThat(values[0]).isEmpty()

        showCheckboxFlow.tryEmit(false)

        ShadowLooper.runUiThreadTasksIncludingDelayedTasks()

        assertThat(values[1]).containsExactly(
            IdentifierSpec.SaveForFutureUse
        )
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
        lpmResourceRepository: ResourceRepository<LpmRepository>
    ) = FormViewModel(
        context = context,
        formArguments = arguments,
        lpmResourceRepository = lpmResourceRepository,
        addressResourceRepository = addressResourceRepository,
        showCheckboxFlow = showCheckboxFlow
    )
}

internal suspend fun FormViewModel.setSaveForFutureUse(value: Boolean) {
    elementsFlow
        .firstOrNull()
        ?.filterIsInstance<SaveForFutureUseElement>()
        ?.firstOrNull()?.controller?.onValueChange(value)
}
