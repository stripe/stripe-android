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
import com.stripe.android.paymentsheet.PaymentSheetFixtures.COMPOSE_FRAGMENT_ARGS
import com.stripe.android.paymentsheet.R
import com.stripe.android.paymentsheet.injection.FormViewModelSubcomponent
import com.stripe.android.paymentsheet.model.PaymentSelection
import com.stripe.android.ui.core.address.AddressFieldElementRepository
import com.stripe.android.ui.core.elements.AddressElement
import com.stripe.android.ui.core.elements.CountrySpec
import com.stripe.android.ui.core.elements.EmailSpec
import com.stripe.android.ui.core.elements.IdentifierSpec
import com.stripe.android.ui.core.elements.LayoutSpec
import com.stripe.android.ui.core.elements.RowElement
import com.stripe.android.ui.core.elements.SaveForFutureUseController
import com.stripe.android.ui.core.elements.SaveForFutureUseElement
import com.stripe.android.ui.core.elements.SaveForFutureUseSpec
import com.stripe.android.ui.core.elements.SectionElement
import com.stripe.android.ui.core.elements.SectionSingleFieldElement
import com.stripe.android.ui.core.elements.SectionSpec
import com.stripe.android.ui.core.elements.SimpleTextFieldController
import com.stripe.android.ui.core.elements.SimpleTextSpec.Companion.NAME
import com.stripe.android.ui.core.elements.TextFieldController
import com.stripe.android.ui.core.forms.SepaDebitForm
import com.stripe.android.ui.core.forms.SofortForm
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
    private val emailSection =
        SectionSpec(IdentifierSpec.Generic("email_section"), EmailSpec)
    private val nameSection = SectionSpec(
        IdentifierSpec.Generic("name_section"),
        NAME
    )
    private val countrySection = SectionSpec(
        IdentifierSpec.Generic("country_section"),
        CountrySpec()
    )
    private val context = ContextThemeWrapper(
        ApplicationProvider.getApplicationContext(), com.stripe.android.ui.core.R.style.StripeDefaultTheme
    )

    private val resourceRepository =
        StaticResourceRepository(
            AddressFieldElementRepository(
                ApplicationProvider.getApplicationContext<Context>().resources
            )
        )

    @Test
    fun `Factory gets initialized by Injector when Injector is available`() {
        val mockBuilder = mock<FormViewModelSubcomponent.Builder>()
        val mockSubcomponent = mock<FormViewModelSubcomponent>()
        val mockViewModel = mock<FormViewModel>()

        whenever(mockBuilder.build()).thenReturn(mockSubcomponent)
        whenever(mockBuilder.layout(any())).thenReturn(mockBuilder)
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
            SofortForm
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
            SofortForm
        ) { ApplicationProvider.getApplicationContext<Application>() }
        val factorySpy = spy(factory)
        assertNotNull(factorySpy.create(FormViewModel::class.java))
        verify(factorySpy).fallbackInitialize(
            argWhere {
                it.resource == ApplicationProvider.getApplicationContext<Application>().resources
            }
        )
    }

    @Test
    fun `Verify setting save for future use`() = runTest {
        val args = COMPOSE_FRAGMENT_ARGS
        val formViewModel = FormViewModel(
            LayoutSpec.create(
                emailSection,
                countrySection,
                SaveForFutureUseSpec()
            ),
            args,
            resourceRepository = resourceRepository,
            transformSpecToElement = TransformSpecToElement(resourceRepository, args, context)
        )

        val values = mutableListOf<Boolean?>()
        formViewModel.saveForFutureUse.asLiveData()
            .observeForever {
                values.add(it)
            }
        assertThat(values[0]).isTrue()

        formViewModel.setSaveForFutureUse(false)
        formViewModel.addHiddenIdentifiers(listOf(EmailSpec.identifier))

        assertThat(values[1]).isFalse()
    }

    @Test
    fun `Verify setting save for future use visibility`() {
        val args = COMPOSE_FRAGMENT_ARGS
        val formViewModel = FormViewModel(
            LayoutSpec.create(
                emailSection,
                countrySection,
                SaveForFutureUseSpec()
            ),
            args,
            resourceRepository = resourceRepository,
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

        assertThat(values[1][0]).isEqualTo(IdentifierSpec.SaveForFutureUse)
    }

    @Test
    fun `Verify setting section as hidden sets sub-fields as hidden as well`() = runTest {
        val args = COMPOSE_FRAGMENT_ARGS
        val formViewModel = FormViewModel(
            LayoutSpec.create(
                emailSection,
                countrySection,
                SaveForFutureUseSpec()
            ),
            args,
            resourceRepository = resourceRepository,
            transformSpecToElement = TransformSpecToElement(resourceRepository, args, context)
        )

        val values = mutableListOf<List<IdentifierSpec>>()
        formViewModel.hiddenIdentifiers.asLiveData()
            .observeForever {
                values.add(it)
            }
        assertThat(values[0]).isEmpty()

        formViewModel.setSaveForFutureUse(false)
        formViewModel.addHiddenIdentifiers(listOf(IdentifierSpec.Generic("email_section")))

        ShadowLooper.runUiThreadTasksIncludingDelayedTasks()

        assertThat(values[1][0]).isEqualTo(IdentifierSpec.Generic("email_section"))
        assertThat(values[1][1]).isEqualTo(IdentifierSpec.Email)
    }

    @ExperimentalCoroutinesApi
    @Test
    fun `Verify if there are no text fields nothing is hidden`() = runTest {
        // Here we have just a country, no text fields.
        val args = COMPOSE_FRAGMENT_ARGS
        val formViewModel = FormViewModel(
            LayoutSpec.create(
                countrySection
            ),
            args,
            resourceRepository = resourceRepository,
            transformSpecToElement = TransformSpecToElement(resourceRepository, args, context)
        )

        // Verify formFieldValues does not contain email
        assertThat(formViewModel.lastTextFieldIdentifier.first()?.v1).isEqualTo(
            null
        )
    }

    @ExperimentalCoroutinesApi
    @Test
    fun `Verify if the last text field is hidden the second to last text field is the last display text field`() = runTest {
        // Here we have one hidden and one required field, country will always be in the result,
        //  and name only if saveForFutureUse is true
        val args = COMPOSE_FRAGMENT_ARGS
        val formViewModel = FormViewModel(
            LayoutSpec.create(
                nameSection,
                emailSection,
                countrySection,
                SaveForFutureUseSpec()
            ),
            args,
            resourceRepository = resourceRepository,
            transformSpecToElement = TransformSpecToElement(resourceRepository, args, context)
        )

        val saveForFutureUseController = formViewModel.elements.first()!!.map { it.controller }
            .filterIsInstance(SaveForFutureUseController::class.java).first()

        formViewModel.addHiddenIdentifiers(listOf(EmailSpec.identifier))
        saveForFutureUseController.onValueChange(false)

        // Verify formFieldValues does not contain email
        assertThat(formViewModel.lastTextFieldIdentifier.first()?.v1).isEqualTo(
            nameSection.fields.first().identifier.v1
        )
    }

    @ExperimentalCoroutinesApi
    @Test
    fun `Verify if a field is hidden and valid it is not in the completeFormValues`() = runTest {
        // Here we have one hidden and one required field, country will always be in the result,
        //  and name only if saveForFutureUse is true
        val args = COMPOSE_FRAGMENT_ARGS
        val formViewModel = FormViewModel(
            LayoutSpec.create(
                emailSection,
                countrySection,
                SaveForFutureUseSpec()
            ),
            args,
            resourceRepository = resourceRepository,
            transformSpecToElement = TransformSpecToElement(resourceRepository, args, context)
        )

        val saveForFutureUseController = formViewModel.elements.first()!!.map { it.controller }
            .filterIsInstance(SaveForFutureUseController::class.java).first()
        val emailController =
            getSectionFieldTextControllerWithLabel(formViewModel, com.stripe.android.ui.core.R.string.email)

        // Add text to the name to make it valid
        emailController?.onValueChange("email@valid.com")

        // Verify formFieldValues contains email
        assertThat(
            formViewModel.completeFormValues.first()?.fieldValuePairs
        ).containsKey(
            emailSection.fields[0].identifier
        )

        saveForFutureUseController.onValueChange(false)

        // Verify formFieldValues does not contain email
        assertThat(formViewModel.completeFormValues.first()?.fieldValuePairs).doesNotContainKey(
            emailSection.api_path
        )
    }

    @ExperimentalCoroutinesApi
    @Test
    fun `Hidden invalid fields arent in the formViewValue and has no effect on complete state`() = runTest {
        // Here we have one hidden and one required field, country will always be in the result,
        //  and name only if saveForFutureUse is true
        val args = COMPOSE_FRAGMENT_ARGS
        val formViewModel = FormViewModel(
            LayoutSpec.create(
                emailSection,
                countrySection,
                SaveForFutureUseSpec()
            ),
            args,
            resourceRepository = resourceRepository,
            transformSpecToElement = TransformSpecToElement(resourceRepository, args, context)
        )

        val saveForFutureUseController = formViewModel.elements.first()!!.map { it.controller }
            .filterIsInstance(SaveForFutureUseController::class.java).first()
        val emailController =
            getSectionFieldTextControllerWithLabel(formViewModel, com.stripe.android.ui.core.R.string.email)

        // Add text to the email to make it invalid
        emailController?.onValueChange("email is invalid")

        // Verify formFieldValues is null because the email is required and invalid
        assertThat(formViewModel.completeFormValues.first()).isNull()

        formViewModel.addHiddenIdentifiers(listOf(EmailSpec.identifier))
        saveForFutureUseController.onValueChange(false)

        // Verify formFieldValues is not null even though the email is invalid
        // (because it is not required)
        val completeFormFieldValues = formViewModel.completeFormValues.first()
        assertThat(
            completeFormFieldValues
        ).isNotNull()
        assertThat(formViewModel.completeFormValues.first()?.fieldValuePairs).doesNotContainKey(
            emailSection.api_path
        )
        assertThat(formViewModel.completeFormValues.first()?.userRequestedReuse).isEqualTo(
            PaymentSelection.CustomerRequestedSave.RequestNoReuse
        )
    }

    /**
     * This is serving as more of an integration test of forms from
     * spec to FormFieldValues.
     */
    @ExperimentalCoroutinesApi
    @Test
    fun `Verify params are set when element flows are complete`() = runTest {
        /**
         * Using sofort as a complex enough example to test the form view model class.
         */
        val args = COMPOSE_FRAGMENT_ARGS.copy(
            billingDetails = null,
            showCheckbox = true,
            showCheckboxControlledFields = true
        )
        val formViewModel = FormViewModel(
            LayoutSpec.create(
                SectionSpec(
                    IdentifierSpec.Generic("name_section"),
                    NAME
                ),
                SectionSpec(IdentifierSpec.Generic("email_section"), EmailSpec),
                SectionSpec(
                    IdentifierSpec.Generic("country_section"),
                    CountrySpec(setOf("AT", "BE", "DE", "ES", "IT", "NL"))
                ),
                SaveForFutureUseSpec()
            ),
            args,
            resourceRepository = resourceRepository,
            transformSpecToElement = TransformSpecToElement(resourceRepository, args, context)
        )

        val nameElement =
            getSectionFieldTextControllerWithLabel(formViewModel, com.stripe.android.ui.core.R.string.address_label_name)
        val emailElement =
            getSectionFieldTextControllerWithLabel(formViewModel, com.stripe.android.ui.core.R.string.email)

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
            formViewModel.completeFormValues.first()?.fieldValuePairs?.get(NAME.identifier)
                ?.value
        ).isEqualTo("joe")
        assertThat(formViewModel.completeFormValues.first()?.userRequestedReuse).isEqualTo(
            PaymentSelection.CustomerRequestedSave.RequestReuse
        )

        emailElement?.onValueChange("invalid.email@IncompleteDomain")

        assertThat(
            formViewModel.completeFormValues.first()?.fieldValuePairs?.get(NAME.identifier)
        ).isNull()
    }

    @ExperimentalCoroutinesApi
    @Test
    fun `Verify params are set when element address fields are complete`() = runTest {
        /**
         * Using sepa debit as a complex enough example to test the address portion.
         */
        val args = COMPOSE_FRAGMENT_ARGS.copy(
            showCheckbox = false,
            showCheckboxControlledFields = true,
            billingDetails = null
        )
        val formViewModel = FormViewModel(
            SepaDebitForm,
            args,
            resourceRepository = resourceRepository,
            transformSpecToElement = TransformSpecToElement(resourceRepository, args, context)
        )

        getSectionFieldTextControllerWithLabel(
            formViewModel,
            com.stripe.android.ui.core.R.string.address_label_name
        )?.onValueChange("joe")
        assertThat(
            formViewModel.completeFormValues.first()?.fieldValuePairs?.get(IdentifierSpec.Name)
                ?.value
        ).isNull()

        getSectionFieldTextControllerWithLabel(
            formViewModel,
            com.stripe.android.ui.core.R.string.email
        )?.onValueChange("joe@gmail.com")
        assertThat(
            formViewModel.completeFormValues.first()?.fieldValuePairs?.get(IdentifierSpec.Email)
                ?.value
        ).isNull()

        getSectionFieldTextControllerWithLabel(
            formViewModel,
            com.stripe.android.ui.core.R.string.iban
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
                        ?.get(EmailSpec.identifier)
                        ?.value
                ).isNotNull()
            } else {
                assertThat(
                    formViewModel
                        .completeFormValues
                        .first()
                        ?.fieldValuePairs
                        ?.get(EmailSpec.identifier)
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
            SepaDebitForm,
            args,
            resourceRepository = resourceRepository,
            transformSpecToElement = TransformSpecToElement(resourceRepository, args, context)
        )

        getSectionFieldTextControllerWithLabel(
            formViewModel,
            com.stripe.android.ui.core.R.string.address_label_name
        )?.onValueChange("joe")
        assertThat(
            formViewModel.completeFormValues.first()?.fieldValuePairs?.get(EmailSpec.identifier)
                ?.value
        ).isNull()

        getSectionFieldTextControllerWithLabel(
            formViewModel,
            com.stripe.android.ui.core.R.string.email
        )?.onValueChange("joe@gmail.com")
        assertThat(
            formViewModel.completeFormValues.first()?.fieldValuePairs?.get(EmailSpec.identifier)
                ?.value
        ).isNull()

        getSectionFieldTextControllerWithLabel(
            formViewModel,
            com.stripe.android.ui.core.R.string.iban
        )?.onValueChange("DE89370400440532013000")
        assertThat(
            formViewModel.completeFormValues.first()?.fieldValuePairs?.get(EmailSpec.identifier)
                ?.value
        ).isNull()

        // Fill all address values except line2
        val addressControllers = AddressControllers.create(formViewModel)
        val populateAddressControllers = addressControllers.controllers
            .filter { it.label.first() != com.stripe.android.ui.core.R.string.address_label_address_line2 }
        populateAddressControllers
            .forEachIndexed { index, textFieldController ->
                textFieldController.onValueChange("1234")

                if (index == populateAddressControllers.size - 1) {
                    assertThat(
                        formViewModel
                            .completeFormValues
                            .first()
                            ?.fieldValuePairs
                            ?.get(EmailSpec.identifier)
                            ?.value
                    ).isNotNull()
                } else {
                    assertThat(
                        formViewModel
                            .completeFormValues
                            .first()
                            ?.fieldValuePairs
                            ?.get(EmailSpec.identifier)
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
                            com.stripe.android.ui.core.R.string.address_label_address_line1
                        ),
                        getAddressSectionTextControllerWithLabel(
                            formViewModel,
                            com.stripe.android.ui.core.R.string.address_label_address_line2
                        ),
                        getAddressSectionTextControllerWithLabel(
                            formViewModel,
                            com.stripe.android.ui.core.R.string.address_label_city
                        ),
                        getAddressSectionTextControllerWithLabel(
                            formViewModel,
                            com.stripe.android.ui.core.R.string.address_label_state
                        ),
                        getAddressSectionTextControllerWithLabel(
                            formViewModel,
                            com.stripe.android.ui.core.R.string.address_label_zip_code
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

internal suspend fun FormViewModel.setSaveForFutureUse(value: Boolean) {
    elements
        .firstOrNull()
        ?.filterIsInstance<SaveForFutureUseElement>()
        ?.firstOrNull()?.controller?.onValueChange(value)
}
