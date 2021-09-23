package com.stripe.android.paymentsheet.forms

import android.content.Context
import androidx.annotation.StringRes
import androidx.lifecycle.asLiveData
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import com.stripe.android.paymentsheet.R
import com.stripe.android.paymentsheet.address.AddressFieldElementRepository
import com.stripe.android.paymentsheet.elements.AddressElement
import com.stripe.android.paymentsheet.elements.SaveForFutureUseController
import com.stripe.android.paymentsheet.elements.SectionElement
import com.stripe.android.paymentsheet.elements.SectionSingleFieldElement
import com.stripe.android.paymentsheet.elements.TextFieldController
import com.stripe.android.paymentsheet.paymentdatacollection.FormFragmentArguments
import com.stripe.android.paymentsheet.elements.BankRepository
import com.stripe.android.paymentsheet.elements.IdentifierSpec
import com.stripe.android.paymentsheet.elements.LayoutSpec
import com.stripe.android.paymentsheet.elements.ResourceRepository
import com.stripe.android.paymentsheet.elements.CountrySpec
import com.stripe.android.paymentsheet.elements.EmailSpec
import com.stripe.android.paymentsheet.elements.RowElement
import com.stripe.android.paymentsheet.elements.SaveForFutureUseSpec
import com.stripe.android.paymentsheet.elements.SectionSpec
import com.stripe.android.paymentsheet.elements.SimpleTextSpec.Companion.NAME
import com.stripe.android.paymentsheet.model.SupportedPaymentMethod
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
        SectionSpec(IdentifierSpec.Generic("email_section"), EmailSpec)
    private val countrySection = SectionSpec(
        IdentifierSpec.Generic("country_section"),
        CountrySpec()
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
                    SaveForFutureUseSpec(listOf(emailSection))
                )
            ),
            FormFragmentArguments(
                supportedPaymentMethod = SupportedPaymentMethod.Card,
                displayUIRequiredForSaving = true,
                allowUserInitiatedReuse = true,
                merchantName = "Example, Inc."
            ),
            resourceRepository = resourceRepository
        )

        val values = mutableListOf<Boolean>()
        formViewModel.saveForFutureUseElement?.controller?.saveForFutureUse?.asLiveData()
            ?.observeForever {
                values.add(it)
            }
        assertThat(values[0]).isTrue()

        formViewModel.setUserInitiatedReuseValue(false)

        assertThat(values[1]).isFalse()
    }

    @Test
    fun `Verify setting save for future use visibility`() {
        val formViewModel = FormViewModel(
            LayoutSpec(
                listOf(
                    emailSection,
                    countrySection,
                    SaveForFutureUseSpec(listOf(emailSection))
                )
            ),
            FormFragmentArguments(
                supportedPaymentMethod = SupportedPaymentMethod.Card,
                displayUIRequiredForSaving = true,
                allowUserInitiatedReuse = true,
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

        formViewModel.setUserInitiatedReuseVisibility(false)

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
                    SaveForFutureUseSpec(listOf(emailSection))
                )
            ),
            FormFragmentArguments(
                supportedPaymentMethod = SupportedPaymentMethod.Card,
                displayUIRequiredForSaving = true,
                allowUserInitiatedReuse = true,
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

        formViewModel.setUserInitiatedReuseValue(false)

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
                        SaveForFutureUseSpec(listOf(emailSection))
                    )
                ),
                FormFragmentArguments(
                    supportedPaymentMethod = SupportedPaymentMethod.Card,
                    displayUIRequiredForSaving = true,
                    allowUserInitiatedReuse = true,
                    merchantName = "Example, Inc."
                ),
                resourceRepository = resourceRepository
            )

            val saveForFutureUseController = formViewModel.elements.map { it.controller }
                .filterIsInstance(SaveForFutureUseController::class.java).first()
            val emailController =
                getSectionFieldTextControllerWithLabel(formViewModel, R.string.email)

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
                        SaveForFutureUseSpec(listOf(emailSection))
                    )
                ),
                FormFragmentArguments(
                    supportedPaymentMethod = SupportedPaymentMethod.Card,
                    displayUIRequiredForSaving = true,
                    allowUserInitiatedReuse = true,
                    merchantName = "Example, Inc."
                ),
                resourceRepository = resourceRepository
            )

            val saveForFutureUseController = formViewModel.elements.map { it.controller }
                .filterIsInstance(SaveForFutureUseController::class.java).first()
            val emailController =
                getSectionFieldTextControllerWithLabel(formViewModel, R.string.email)

            // Add text to the email to make it invalid
            emailController?.onValueChange("email is invalid")

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
                    supportedPaymentMethod = SupportedPaymentMethod.Card,
                    displayUIRequiredForSaving = true,
                    allowUserInitiatedReuse = true,
                    merchantName = "Example, Inc."
                ),
                resourceRepository = resourceRepository
            )

            val nameElement =
                getSectionFieldTextControllerWithLabel(formViewModel, R.string.address_label_name)
            val emailElement =
                getSectionFieldTextControllerWithLabel(formViewModel, R.string.email)

            nameElement?.onValueChange("joe")
            assertThat(
                formViewModel.completeFormValues.first()?.fieldValuePairs?.get(NAME.identifier)
            ).isNull()

            emailElement?.onValueChange("joe@gmail.com")
            assertThat(
                formViewModel.completeFormValues.first()?.fieldValuePairs?.get(EmailSpec.identifier)
                    ?.value
            ).isEqualTo("joe@gmail.com")
            assertThat(
                formViewModel.completeFormValues.first()?.fieldValuePairs?.get(NAME.identifier)
                    ?.value
            ).isEqualTo("joe")

            emailElement?.onValueChange("invalid.email@IncompleteDomain")

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
                    supportedPaymentMethod = SupportedPaymentMethod.Card,
                    displayUIRequiredForSaving = true,
                    allowUserInitiatedReuse = true,
                    merchantName = "Example, Inc."
                ),
                resourceRepository = resourceRepository
            )

            getSectionFieldTextControllerWithLabel(
                formViewModel,
                R.string.address_label_name
            )?.onValueChange("joe")
            assertThat(
                formViewModel.completeFormValues.first()?.fieldValuePairs?.get(EmailSpec.identifier)
                    ?.value
            ).isNull()

            getSectionFieldTextControllerWithLabel(
                formViewModel,
                R.string.email
            )?.onValueChange("joe@gmail.com")
            assertThat(
                formViewModel.completeFormValues.first()?.fieldValuePairs?.get(EmailSpec.identifier)
                    ?.value
            ).isNull()

            getSectionFieldTextControllerWithLabel(
                formViewModel,
                R.string.iban
            )?.onValueChange("DE89370400440532013000")
            assertThat(
                formViewModel.completeFormValues.first()?.fieldValuePairs?.get(EmailSpec.identifier)
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
                    supportedPaymentMethod = SupportedPaymentMethod.Card,
                    displayUIRequiredForSaving = true,
                    allowUserInitiatedReuse = true,
                    merchantName = "Example, Inc."
                ),
                resourceRepository = resourceRepository
            )

            getSectionFieldTextControllerWithLabel(
                formViewModel,
                R.string.address_label_name
            )?.onValueChange("joe")
            assertThat(
                formViewModel.completeFormValues.first()?.fieldValuePairs?.get(EmailSpec.identifier)
                    ?.value
            ).isNull()

            getSectionFieldTextControllerWithLabel(
                formViewModel,
                R.string.email
            )?.onValueChange("joe@gmail.com")
            assertThat(
                formViewModel.completeFormValues.first()?.fieldValuePairs?.get(EmailSpec.identifier)
                    ?.value
            ).isNull()

            getSectionFieldTextControllerWithLabel(
                formViewModel,
                R.string.iban
            )?.onValueChange("DE89370400440532013000")
            assertThat(
                formViewModel.completeFormValues.first()?.fieldValuePairs?.get(EmailSpec.identifier)
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
    }

    private fun getSectionFieldTextControllerWithLabel(
        formViewModel: FormViewModel,
        @StringRes label: Int
    ) =
        formViewModel.elements
            .filterIsInstance<SectionElement>()
            .flatMap { it.fields }
            .filterIsInstance<SectionSingleFieldElement>()
            .map { it.controller }
            .filterIsInstance<TextFieldController>()
            .firstOrNull { it.label == label }

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
            val addressElementFields = formViewModel.elements
                .filterIsInstance<SectionElement>()
                .flatMap { it.fields }
                .filterIsInstance<AddressElement>()
                .firstOrNull()
                ?.fields
                ?.first()
            return addressElementFields
                ?.filterIsInstance<SectionSingleFieldElement>()
                ?.map { (it.controller as? TextFieldController) }
                ?.firstOrNull { it?.label == label }
                ?: addressElementFields
                    ?.filterIsInstance<RowElement>()
                    ?.map { it.fields }
                    ?.flatten()
                    ?.map { (it.controller as? TextFieldController) }
                    ?.firstOrNull { it?.label == label }
        }
    }
}
