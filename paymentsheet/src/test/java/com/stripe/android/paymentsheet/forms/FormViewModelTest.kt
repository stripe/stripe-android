package com.stripe.android.paymentsheet.forms

import android.app.Application
import android.content.Context
import androidx.annotation.StringRes
import androidx.appcompat.view.ContextThemeWrapper
import androidx.lifecycle.asLiveData
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import com.stripe.android.core.injection.DUMMY_INJECTOR_KEY
import com.stripe.android.core.injection.Injectable
import com.stripe.android.core.injection.Injector
import com.stripe.android.core.injection.WeakMapInjectorRegistry
import com.stripe.android.model.PaymentMethod
import com.stripe.android.paymentsheet.PaymentSheetFixtures.COMPOSE_FRAGMENT_ARGS
import com.stripe.android.paymentsheet.injection.FormViewModelSubcomponent
import com.stripe.android.paymentsheet.model.PaymentSelection
import com.stripe.android.ui.core.R
import com.stripe.android.ui.core.address.AddressFieldElementRepository
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
import com.stripe.android.ui.core.forms.resources.StaticResourceRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.any
import org.mockito.kotlin.argWhere
import org.mockito.kotlin.mock
import org.mockito.kotlin.spy
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.robolectric.RobolectricTestRunner
import org.robolectric.shadows.ShadowLooper
import javax.inject.Provider

@ExperimentalCoroutinesApi
@FlowPreview
@RunWith(RobolectricTestRunner::class)
internal class FormViewModelTest {
    private val emailSection = EmailSpec()
    private val context = ContextThemeWrapper(
        ApplicationProvider.getApplicationContext(), R.style.StripeDefaultTheme
    )
    val lpmRepository = LpmRepository(context.resources)

    private val resourceRepository =
        StaticResourceRepository(
            AddressFieldElementRepository(
                ApplicationProvider.getApplicationContext<Context>().resources
            ),
            mock()
        )

    @Test
    fun `Factory gets initialized by Injector when Injector is available`() {
        val mockBuilder = mock<FormViewModelSubcomponent.Builder>()
        val mockSubcomponent = mock<FormViewModelSubcomponent>()
        val mockViewModel = mock<FormViewModel>()

        whenever(mockBuilder.build()).thenReturn(mockSubcomponent)
        whenever(mockBuilder.paymentMethodCode(any())).thenReturn(mockBuilder)
        whenever(mockBuilder.formFragmentArguments(any())).thenReturn(mockBuilder)
        whenever(mockSubcomponent.viewModel).thenReturn(mockViewModel)

        val injector = object : Injector {
            override fun inject(injectable: Injectable<*>) {
                val factory = injectable as FormViewModel.Factory
                factory.subComponentBuilderProvider = Provider { mockBuilder }
            }
        }
        val injectorKey = WeakMapInjectorRegistry.nextKey("testKey")
        val config = COMPOSE_FRAGMENT_ARGS.copy(injectorKey = injectorKey)
        WeakMapInjectorRegistry.register(injector, injectorKey)
        val factory = FormViewModel.Factory(
            config,
            ApplicationProvider.getApplicationContext<Application>().resources,
            PaymentMethod.Type.Sofort.code
        ) { ApplicationProvider.getApplicationContext<Application>() }
        val factorySpy = spy(factory)
        val createdViewModel = factorySpy.create(FormViewModel::class.java)
        verify(factorySpy, times(0)).fallbackInitialize(any())
        assertThat(createdViewModel).isEqualTo(mockViewModel)

        WeakMapInjectorRegistry.clear()
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun `Factory gets initialized with fallback when no Injector is available`() = runTest {
        val config = COMPOSE_FRAGMENT_ARGS.copy(injectorKey = DUMMY_INJECTOR_KEY)
        val factory = FormViewModel.Factory(
            config,
            ApplicationProvider.getApplicationContext<Application>().resources,
            PaymentMethod.Type.Sofort.code
        ) { ApplicationProvider.getApplicationContext<Application>() }
        val factorySpy = spy(factory)
        assertNotNull(factorySpy.create(FormViewModel::class.java))
        verify(factorySpy).fallbackInitialize(
            argWhere {
                it.resource == ApplicationProvider.getApplicationContext<Application>().resources
            }
        )
    }

    private fun createRepositorySupportedPaymentMethod(
        paymentMethodType: PaymentMethod.Type,
        layoutSpec: LayoutSpec
    ): StaticResourceRepository {
        val mockLpmRepository = mock<LpmRepository>()

        whenever(mockLpmRepository.fromCode(paymentMethodType.code)).thenReturn(
            LpmRepository.SupportedPaymentMethod(
                paymentMethodType,
                R.string.stripe_paymentsheet_payment_method_card,
                R.drawable.stripe_ic_paymentsheet_pm_card,
                PaymentMethodRequirements(emptySet(), emptySet(), true),
                layoutSpec
            )
        )
        return StaticResourceRepository(
            AddressFieldElementRepository(
                ApplicationProvider.getApplicationContext<Context>().resources
            ),
            mockLpmRepository
        )
    }

    @Test
    fun `Verify setting save for future use value is updated in flowable`() = runTest {
        val args = COMPOSE_FRAGMENT_ARGS

        val formViewModel = FormViewModel(
            PaymentMethod.Type.Card.code,
            args,
            resourceRepository = createRepositorySupportedPaymentMethod(
                PaymentMethod.Type.Card,
                LayoutSpec.create(
                    EmailSpec(),
                    SaveForFutureUseSpec()
                )
            ),
            transformSpecToElement = TransformSpecToElement(resourceRepository, args, context)
        )

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
        val args = COMPOSE_FRAGMENT_ARGS
        val formViewModel = FormViewModel(
            PaymentMethod.Type.Card.code,
            args,
            resourceRepository = createRepositorySupportedPaymentMethod(
                PaymentMethod.Type.Card,
                LayoutSpec.create(
                    SaveForFutureUseSpec()
                )
            ),
            transformSpecToElement = TransformSpecToElement(resourceRepository, args, context)
        )

        val values = mutableListOf<List<IdentifierSpec>>()
        formViewModel.hiddenIdentifiers.asLiveData()
            .observeForever {
                values.add(it)
            }
        assertThat(values[0]).isEmpty()

        formViewModel.saveForFutureUseVisible.value = false

        ShadowLooper.runUiThreadTasksIncludingDelayedTasks()

        assertThat(values[1][0]).isEqualTo(
            IdentifierSpec.SaveForFutureUse
        )
    }

    @ExperimentalCoroutinesApi
    @Test
    fun `Verify if there are no text fields, there is no last text field id`() = runTest {
        // Here we have just a country, no text fields.
        val args = COMPOSE_FRAGMENT_ARGS
        val formViewModel = FormViewModel(
            PaymentMethod.Type.Card.code,
            args,
            resourceRepository = createRepositorySupportedPaymentMethod(
                PaymentMethod.Type.Card,
                LayoutSpec.create(
                    CountrySpec(),
                )
            ),
            transformSpecToElement = TransformSpecToElement(resourceRepository, args, context)
        )

        // Verify formFieldValues does not contain email
        assertThat(formViewModel.lastTextFieldIdentifier.first()?.v1).isEqualTo(
            null
        )
    }

    @ExperimentalCoroutinesApi
    @Test
    fun `Verify if the last text field is hidden the second to last text field is the last text field id`() = runTest {
        // Here we have one hidden (email) and one required field (name), country will always be in the result,
        //  and email only if it is not hidden
        val args = COMPOSE_FRAGMENT_ARGS
        val formViewModel = FormViewModel(
            PaymentMethod.Type.P24.code,
            args,
            resourceRepository = createRepositorySupportedPaymentMethod(
                PaymentMethod.Type.P24,
                LayoutSpec.create(
                    NameSpec(),
                    EmailSpec(),
                    CountrySpec(),
                )
            ),
            transformSpecToElement = TransformSpecToElement(resourceRepository, args, context)
        )

        formViewModel.addHiddenIdentifiers(listOf(IdentifierSpec.Email))

        // Verify formFieldValues does not contain email
        assertThat(formViewModel.lastTextFieldIdentifier.first()?.v1).isEqualTo(
            IdentifierSpec.Name.v1
        )
    }

    @ExperimentalCoroutinesApi
    @Test
    fun `Verify if a field is hidden and valid it is not in the completeFormValues`() = runTest {
        // Here we have one hidden (email) and one required field (name), bank will always be in the result,
        //  and name only if not hidden
        val args = COMPOSE_FRAGMENT_ARGS
        val formViewModel = FormViewModel(
            PaymentMethod.Type.Card.code,
            args,
            resourceRepository = createRepositorySupportedPaymentMethod(
                PaymentMethod.Type.Card,
                LayoutSpec.create(
                    EmailSpec(),
                    CountrySpec(),
                )
            ),
            transformSpecToElement = TransformSpecToElement(resourceRepository, args, context)
        )

        val emailController =
            getSectionFieldTextControllerWithLabel(formViewModel, R.string.email)

        // Add text to the name to make it valid
        emailController?.onValueChange("email@valid.com")

        // Verify formFieldValues contains email
        assertThat(
            formViewModel.completeFormValues.first()?.fieldValuePairs
        ).containsKey(IdentifierSpec.Email)

        formViewModel.addHiddenIdentifiers(listOf(IdentifierSpec.Email))

        // Verify formFieldValues does not contain email
        assertThat(formViewModel.completeFormValues.first()?.fieldValuePairs)
            .doesNotContainKey(IdentifierSpec.Email)
    }

    @ExperimentalCoroutinesApi
    @Test
    fun `Hidden invalid fields arent in the formViewValue and has no effect on complete state`() = runTest {
        // Here we have one hidden (email) and one required field (name), bank will always be in the result,
        //  and email only if not hidden
        val args = COMPOSE_FRAGMENT_ARGS
        val formViewModel = FormViewModel(
            PaymentMethod.Type.Card.code,
            args,
            resourceRepository = createRepositorySupportedPaymentMethod(
                PaymentMethod.Type.Card,
                LayoutSpec.create(
                    EmailSpec(),
                    CountrySpec(),
                    SaveForFutureUseSpec()
                )
            ),
            transformSpecToElement = TransformSpecToElement(resourceRepository, args, context)
        )

        val emailController =
            getSectionFieldTextControllerWithLabel(formViewModel, R.string.email)

        // Add text to the email to make it invalid
        emailController?.onValueChange("joe")

        // Verify formFieldValues is null because the email is required and invalid
        assertThat(formViewModel.completeFormValues.first()).isNull()

        formViewModel.addHiddenIdentifiers(listOf(IdentifierSpec.Email))

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
    @ExperimentalCoroutinesApi
    @Test
    fun `Verify params are set when element flows are complete`() = runTest {
        val args = COMPOSE_FRAGMENT_ARGS.copy(
            billingDetails = null,
            showCheckbox = true,
            showCheckboxControlledFields = true
        )
        val formViewModel = FormViewModel(
            PaymentMethod.Type.P24.code,
            args,
            resourceRepository = createRepositorySupportedPaymentMethod(
                PaymentMethod.Type.P24,
                LayoutSpec.create(
                    NameSpec(),
                    EmailSpec(),
                    CountrySpec(onlyShowCountryCodes = setOf("AT", "BE", "DE", "ES", "IT", "NL")),
                    SaveForFutureUseSpec()
                )
            ),
            transformSpecToElement = TransformSpecToElement(resourceRepository, args, context)
        )

        val nameElement =
            getSectionFieldTextControllerWithLabel(formViewModel, R.string.address_label_name)
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

    @ExperimentalCoroutinesApi
    @Test
    fun `Verify params are set when element address fields are complete`() = runTest {
        val args = COMPOSE_FRAGMENT_ARGS.copy(
            showCheckbox = false,
            showCheckboxControlledFields = true,
            billingDetails = null
        )
        val formViewModel = FormViewModel(
            PaymentMethod.Type.SepaDebit.code,
            args,
            resourceRepository = createRepositorySupportedPaymentMethod(
                PaymentMethod.Type.SepaDebit,
                LayoutSpec.create(
                    NameSpec(),
                    EmailSpec(),
                    IbanSpec(),
                    AddressSpec(
                        IdentifierSpec.Generic("address"),
                        validCountryCodes = setOf("US", "JP")
                    ),
                    MandateTextSpec(
                        IdentifierSpec.Generic("mandate"),
                        R.string.sepa_mandate
                    ),
                )
            ),
            transformSpecToElement = TransformSpecToElement(resourceRepository, args, context)
        )

        getSectionFieldTextControllerWithLabel(
            formViewModel,
            R.string.address_label_name
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
            textFieldController.onValueChange("1234")
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

    @ExperimentalCoroutinesApi
    @Test
    fun `Verify params are set when required address fields are complete`() = runTest {
        /**
         * Using sepa debit as a complex enough example to test the address portion.
         */
        val args = COMPOSE_FRAGMENT_ARGS.copy(
            billingDetails = null
        )
        val formViewModel = FormViewModel(
            PaymentMethod.Type.SepaDebit.code,
            args,
            resourceRepository = createRepositorySupportedPaymentMethod(
                PaymentMethod.Type.SepaDebit,
                LayoutSpec.create(
                    NameSpec(),
                    EmailSpec(),
                    IbanSpec(),
                    AddressSpec(
                        IdentifierSpec.Generic("address"),
                        validCountryCodes = setOf("US", "JP")
                    ),
                    MandateTextSpec(
                        IdentifierSpec.Generic("mandate"),
                        R.string.sepa_mandate
                    ),
                )
            ),
            transformSpecToElement = TransformSpecToElement(resourceRepository, args, context)
        )

        getSectionFieldTextControllerWithLabel(
            formViewModel,
            R.string.address_label_name
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
                textFieldController.onValueChange("1234")

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
        formViewModel.elements.first()!!
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
                        ),
                    )
                )
        }
    }

    companion object {
        private suspend fun getAddressSectionTextControllerWithLabel(
            formViewModel: FormViewModel,
            @StringRes label: Int
        ): TextFieldController? {
            val addressElementFields = formViewModel.elements.first()!!
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
}

@OptIn(FlowPreview::class)
internal suspend fun FormViewModel.setSaveForFutureUse(value: Boolean) {
    elements
        .firstOrNull()
        ?.filterIsInstance<SaveForFutureUseElement>()
        ?.firstOrNull()?.controller?.onValueChange(value)
}
