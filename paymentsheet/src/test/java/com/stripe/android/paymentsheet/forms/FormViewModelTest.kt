package com.stripe.android.paymentsheet.forms

import android.content.Context
import androidx.annotation.StringRes
import androidx.lifecycle.asLiveData
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import com.stripe.android.paymentsheet.FormElement.SectionElement
import com.stripe.android.paymentsheet.R
import com.stripe.android.paymentsheet.SectionFieldElement
import com.stripe.android.paymentsheet.address.AddressFieldElementRepository
import com.stripe.android.paymentsheet.elements.SaveForFutureUseController
import com.stripe.android.paymentsheet.elements.TextFieldController
import com.stripe.android.paymentsheet.paymentdatacollection.FormFragmentArguments
import com.stripe.android.paymentsheet.specifications.BankRepository
import com.stripe.android.paymentsheet.specifications.FormItemSpec
import com.stripe.android.paymentsheet.specifications.IdentifierSpec
import com.stripe.android.paymentsheet.specifications.LayoutSpec
import com.stripe.android.paymentsheet.specifications.ResourceRepository
import com.stripe.android.paymentsheet.specifications.SectionFieldSpec.Companion.NAME
import com.stripe.android.paymentsheet.specifications.SectionFieldSpec.Country
import com.stripe.android.paymentsheet.specifications.SectionFieldSpec.Email
import com.stripe.android.paymentsheet.specifications.sepaDebit
import com.stripe.android.paymentsheet.specifications.sofort
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.shadows.ShadowLooper

@RunWith(RobolectricTestRunner::class)
internal class FormViewModelTest {
    private val emailSection =
        FormItemSpec.SectionSpec(IdentifierSpec.Generic("email_section"), Email)
    private val countrySection = FormItemSpec.SectionSpec(
        IdentifierSpec.Generic("country_section"),
        Country()
    )

    private val resourceRepository =
        ResourceRepository(
            BankRepository(
                ApplicationProvider.getApplicationContext<Context>().resources
            ),
            AddressFieldElementRepository(
                ApplicationProvider.getApplicationContext<Context>().resources
            )
        )

    @Test
    fun `Verify setting save for future use`() {
        val formViewModel = FormViewModel(
            LayoutSpec(
                listOf(
                    emailSection,
                    countrySection,
                    FormItemSpec.SaveForFutureUseSpec(listOf(emailSection))
                )
            ),
            FormFragmentArguments(
                supportedPaymentMethodName = "Card",
                saveForFutureUseInitialValue = true,
                saveForFutureUseInitialVisibility = true,
                merchantName = "Example, Inc."
            ),
            resourceRepository = resourceRepository
        )

        val values = mutableListOf<Boolean>()
        formViewModel.saveForFutureUse.asLiveData()
            .observeForever {
                values.add(it)
            }
        assertThat(values[0]).isTrue()

        formViewModel.setSaveForFutureUse(false)

        assertThat(values[1]).isFalse()
    }

    @Test
    fun `Verify setting save for future use visibility`() {
        val formViewModel = FormViewModel(
            LayoutSpec(
                listOf(
                    emailSection,
                    countrySection,
                    FormItemSpec.SaveForFutureUseSpec(listOf(emailSection))
                )
            ),
            FormFragmentArguments(
                supportedPaymentMethodName = "Card",
                saveForFutureUseInitialValue = true,
                saveForFutureUseInitialVisibility = true,
                merchantName = "Example, Inc."
            ),
            resourceRepository = resourceRepository
        )

        val values = mutableListOf<List<IdentifierSpec>>()
        formViewModel.hiddenIdentifiers.asLiveData()
            .observeForever {
                values.add(it)
            }
        assertThat(values[0]).isEmpty()

        formViewModel.setSaveForFutureUseVisibility(false)

        ShadowLooper.runUiThreadTasksIncludingDelayedTasks()

        assertThat(values[1][0]).isEqualTo(IdentifierSpec.SaveForFutureUse)
    }

    @Test
    fun `Verify setting section as hidden sets sub-fields as hidden as well`() {
        val formViewModel = FormViewModel(
            LayoutSpec(
                listOf(
                    emailSection,
                    countrySection,
                    FormItemSpec.SaveForFutureUseSpec(listOf(emailSection))
                )
            ),
            FormFragmentArguments(
                supportedPaymentMethodName = "Card",
                saveForFutureUseInitialValue = true,
                saveForFutureUseInitialVisibility = true,
                merchantName = "Example, Inc."
            ),
            resourceRepository = resourceRepository
        )

        val values = mutableListOf<List<IdentifierSpec>>()
        formViewModel.hiddenIdentifiers.asLiveData()
            .observeForever {
                values.add(it)
            }
        assertThat(values[0]).isEmpty()

        formViewModel.setSaveForFutureUse(false)

        ShadowLooper.runUiThreadTasksIncludingDelayedTasks()

        assertThat(values[1][0]).isEqualTo(IdentifierSpec.Generic("email_section"))
        assertThat(values[1][1]).isEqualTo(IdentifierSpec.Email)
    }

    @ExperimentalCoroutinesApi
    @Test
    fun `Verify if a field is hidden and valid it is not in the completeFormValues`() =
        runBlocking {
            // Here we have one hidden and one required field, country will always be in the result,
            //  and name only if saveForFutureUse is true
            val formViewModel = FormViewModel(
                LayoutSpec(
                    listOf(
                        emailSection,
                        countrySection,
                        FormItemSpec.SaveForFutureUseSpec(listOf(emailSection))
                    )
                ),
                FormFragmentArguments(
                    supportedPaymentMethodName = "Card",
                    saveForFutureUseInitialValue = true,
                    saveForFutureUseInitialVisibility = true,
                    merchantName = "Example, Inc."
                ),
                resourceRepository = resourceRepository
            )

            val saveForFutureUseController = formViewModel.elements.map { it.controller }
                .filterIsInstance(SaveForFutureUseController::class.java).first()
            val emailController = formViewModel.elements
                .asSequence()
                .filterIsInstance<SectionElement>()
                .flatMap { it.fields }
                .map { it.controller }
                .filterIsInstance(TextFieldController::class.java)
                .first()

            // Add text to the name to make it valid
            emailController.onValueChange("email@valid.com")

            // Verify formFieldValues contains email
            assertThat(
                formViewModel.completeFormValues.first()?.fieldValuePairs
            ).containsKey(
                emailSection.fields[0].identifier
            )

            saveForFutureUseController.onValueChange(false)

            // Verify formFieldValues does not contain email
            assertThat(formViewModel.completeFormValues.first()?.fieldValuePairs).doesNotContainKey(
                emailSection.identifier
            )
        }

    @ExperimentalCoroutinesApi
    @Test
    fun `Hidden invalid fields arent in the formViewValue and has no effect on complete state`() {
        runBlocking {
            // Here we have one hidden and one required field, country will always be in the result,
            //  and name only if saveForFutureUse is true
            val formViewModel = FormViewModel(
                LayoutSpec(
                    listOf(
                        emailSection,
                        countrySection,
                        FormItemSpec.SaveForFutureUseSpec(listOf(emailSection))
                    )
                ),
                FormFragmentArguments(
                    supportedPaymentMethodName = "Card",
                    saveForFutureUseInitialValue = true,
                    saveForFutureUseInitialVisibility = true,
                    merchantName = "Example, Inc."
                ),
                resourceRepository = resourceRepository
            )

            val saveForFutureUseController = formViewModel.elements.map { it.controller }
                .filterIsInstance(SaveForFutureUseController::class.java).first()
            val emailController = formViewModel.elements
                .asSequence()
                .filterIsInstance<SectionElement>()
                .flatMap { it.fields }
                .map { it.controller }
                .filterIsInstance(TextFieldController::class.java).first()

            // Add text to the email to make it invalid
            emailController.onValueChange("email is invalid")

            // Verify formFieldValues is null because the email is required and invalid
            assertThat(formViewModel.completeFormValues.first()).isNull()

            saveForFutureUseController.onValueChange(false)

            // Verify formFieldValues is not null even though the email is invalid
            // (because it is not required)
            assertThat(
                formViewModel.completeFormValues.first()
            ).isNotNull()
            assertThat(formViewModel.completeFormValues.first()?.fieldValuePairs).doesNotContainKey(
                emailSection.identifier
            )
        }
    }

    /**
     * This is serving as more of an integration test of forms from
     * spec to FormFieldValues.
     */
    @ExperimentalCoroutinesApi
    @Test
    fun `Verify params are set when element flows are complete`() {
        runBlocking {
            /**
             * Using sofort as a complex enough example to test the form view model class.
             */
            val formViewModel = FormViewModel(
                sofort.layout,
                FormFragmentArguments(
                    supportedPaymentMethodName = "Card",
                    saveForFutureUseInitialValue = true,
                    saveForFutureUseInitialVisibility = true,
                    merchantName = "Example, Inc."
                ),
                resourceRepository = resourceRepository
            )

            val nameElement = (formViewModel.elements[0] as SectionElement)
                .fields[0].controller as TextFieldController
            val emailElement = (formViewModel.elements[1] as SectionElement)
                .fields[0].controller as TextFieldController

            nameElement.onValueChange("joe")
            assertThat(
                formViewModel.completeFormValues.first()?.fieldValuePairs?.get(NAME.identifier)
            ).isNull()

            emailElement.onValueChange("joe@gmail.com")
            assertThat(
                formViewModel.completeFormValues.first()?.fieldValuePairs?.get(Email.identifier)
                    ?.value
            ).isEqualTo("joe@gmail.com")
            assertThat(
                formViewModel.completeFormValues.first()?.fieldValuePairs?.get(NAME.identifier)
                    ?.value
            ).isEqualTo("joe")

            emailElement.onValueChange("invalid.email@IncompleteDomain")

            assertThat(
                formViewModel.completeFormValues.first()?.fieldValuePairs?.get(NAME.identifier)
            ).isNull()
        }
    }

    @ExperimentalCoroutinesApi
    @Test
    fun `Verify params are set when element address fields are complete`() {
        runBlocking {
            /**
             * Using sepa debit as a complex enough example to test the address portion.
             */
            val formViewModel = FormViewModel(
                sepaDebit.layout,
                FormFragmentArguments(
                    supportedPaymentMethodName = "Card",
                    saveForFutureUseInitialValue = true,
                    saveForFutureUseInitialVisibility = true,
                    merchantName = "Example, Inc."
                ),
                resourceRepository = resourceRepository
            )

            getSectionFieldTextControllerWithLabel(
                formViewModel,
                R.string.address_label_name
            )?.onValueChange("joe")
            assertThat(
                formViewModel.completeFormValues.first()?.fieldValuePairs?.get(Email.identifier)
                    ?.value
            ).isNull()

            getSectionFieldTextControllerWithLabel(
                formViewModel,
                R.string.email
            )?.onValueChange("joe@gmail.com")
            assertThat(
                formViewModel.completeFormValues.first()?.fieldValuePairs?.get(Email.identifier)
                    ?.value
            ).isNull()

            getSectionFieldTextControllerWithLabel(
                formViewModel,
                R.string.iban
            )?.onValueChange("DE89370400440532013000")
            assertThat(
                formViewModel.completeFormValues.first()?.fieldValuePairs?.get(Email.identifier)
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
                            ?.get(Email.identifier)
                            ?.value
                    ).isNotNull()
                } else {
                    assertThat(
                        formViewModel
                            .completeFormValues
                            .first()
                            ?.fieldValuePairs
                            ?.get(Email.identifier)
                            ?.value
                    ).isNull()
                }
            }
        }
    }

    @ExperimentalCoroutinesApi
    @Test
    fun `Verify params are set when required address fields are complete`() {
        runBlocking {
            /**
             * Using sepa debit as a complex enough example to test the address portion.
             */
            val formViewModel = FormViewModel(
                sepaDebit.layout,
                FormFragmentArguments(
                    supportedPaymentMethodName = "Card",
                    saveForFutureUseInitialValue = true,
                    saveForFutureUseInitialVisibility = true,
                    merchantName = "Example, Inc."
                ),
                resourceRepository = resourceRepository
            )

            getSectionFieldTextControllerWithLabel(
                formViewModel,
                R.string.address_label_name
            )?.onValueChange("joe")
            assertThat(
                formViewModel.completeFormValues.first()?.fieldValuePairs?.get(Email.identifier)
                    ?.value
            ).isNull()

            getSectionFieldTextControllerWithLabel(
                formViewModel,
                R.string.email
            )?.onValueChange("joe@gmail.com")
            assertThat(
                formViewModel.completeFormValues.first()?.fieldValuePairs?.get(Email.identifier)
                    ?.value
            ).isNull()

            getSectionFieldTextControllerWithLabel(
                formViewModel,
                R.string.iban
            )?.onValueChange("DE89370400440532013000")
            assertThat(
                formViewModel.completeFormValues.first()?.fieldValuePairs?.get(Email.identifier)
                    ?.value
            ).isNull()

            // Fill all address values except line2
            val addressControllers = AddressControllers.create(formViewModel)
            val populateAddressControllers = addressControllers.controllers
                .filter { it.label != R.string.address_label_address_line2 }
            populateAddressControllers
                .forEachIndexed { index, textFieldController ->
                    textFieldController.onValueChange("1234")

                    if (index == populateAddressControllers.size - 1) {
                        assertThat(
                            formViewModel
                                .completeFormValues
                                .first()
                                ?.fieldValuePairs
                                ?.get(Email.identifier)
                                ?.value
                        ).isNotNull()
                    } else {
                        assertThat(
                            formViewModel
                                .completeFormValues
                                .first()
                                ?.fieldValuePairs
                                ?.get(Email.identifier)
                                ?.value
                        ).isNull()
                    }
                }
        }
    }

    private fun getSectionFieldTextControllerWithLabel(
        formViewModel: FormViewModel,
        @StringRes label: Int
    ) =
        formViewModel.elements.map {
            (
                (it as? SectionElement)
                    ?.fields
                    ?.get(0)
                    ?.controller as? TextFieldController
                )
        }.firstOrNull {
            it?.label == label
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
                        ),
                    )
                )
        }
    }

    companion object {
        private suspend fun getAddressSectionTextControllerWithLabel(
            formViewModel: FormViewModel,
            @StringRes label: Int
        ) =
            formViewModel.elements
                .filterIsInstance<SectionElement>()
                .flatMap { it.fields }
                .filterIsInstance<SectionFieldElement.AddressElement>()
                .firstOrNull()
                ?.fields
                ?.first()
                ?.map { (it.controller as? TextFieldController) }
                ?.firstOrNull { it?.label == label }
    }
}
