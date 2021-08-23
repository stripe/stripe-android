package com.stripe.android.paymentsheet

import androidx.annotation.VisibleForTesting
import androidx.compose.ui.graphics.Color
import com.stripe.android.paymentsheet.address.AddressFieldElementRepository
import com.stripe.android.paymentsheet.address.FieldType
import com.stripe.android.paymentsheet.elements.AddressController
import com.stripe.android.paymentsheet.elements.Controller
import com.stripe.android.paymentsheet.elements.CountryConfig
import com.stripe.android.paymentsheet.elements.CreditElementController
import com.stripe.android.paymentsheet.elements.CreditNumberTextFieldController
import com.stripe.android.paymentsheet.elements.CvcTextFieldController
import com.stripe.android.paymentsheet.elements.DropdownFieldController
import com.stripe.android.paymentsheet.elements.InputController
import com.stripe.android.paymentsheet.elements.SaveForFutureUseController
import com.stripe.android.paymentsheet.elements.SectionController
import com.stripe.android.paymentsheet.elements.SectionFieldErrorController
import com.stripe.android.paymentsheet.elements.TextFieldController
import com.stripe.android.paymentsheet.forms.FormFieldEntry
import com.stripe.android.paymentsheet.specifications.IdentifierSpec
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map

/**
 * This is used to define each section in the visual form layout.
 * Each item in the layout has an identifier and a controller associated with it.
 */
internal sealed class FormElement {
    abstract val identifier: IdentifierSpec
    abstract val controller: Controller?

    abstract fun getFormFieldValueFlow(): Flow<List<Pair<IdentifierSpec, FormFieldEntry>>>

    /**
     * This is an element that has static text because it takes no user input, it is not
     * outputted from the list of form field values.  If the stringResId contains a %s, the first
     * one will be populated in the form with the merchantName parameter.
     */
    internal data class MandateTextElement(
        override val identifier: IdentifierSpec,
        val stringResId: Int,
        val color: Color,
        val merchantName: String?,
        override val controller: InputController? = null,
    ) : FormElement() {
        override fun getFormFieldValueFlow(): Flow<List<Pair<IdentifierSpec, FormFieldEntry>>> =
            MutableStateFlow(emptyList())
    }

    /**
     * This is an element that will make elements (as specified by identifier) hidden
     * when "save for future" use is unchecked
     */
    data class SaveForFutureUseElement(
        override val identifier: IdentifierSpec,
        override val controller: SaveForFutureUseController,
        val merchantName: String?
    ) : FormElement() {
        override fun getFormFieldValueFlow(): Flow<List<Pair<IdentifierSpec, FormFieldEntry>>> =
            controller.formFieldValue.map {
                listOf(
                    identifier to it
                )
            }
    }

    data class SectionElement(
        override val identifier: IdentifierSpec,
        val fields: List<SectionFieldElement>,
        override val controller: SectionController
    ) : FormElement() {
        internal constructor(
            identifier: IdentifierSpec,
            field: SectionFieldElement,
            controller: SectionController
        ) : this(identifier, listOf(field), controller)

        override fun getFormFieldValueFlow(): Flow<List<Pair<IdentifierSpec, FormFieldEntry>>> =
            combine(fields.map { it.getFormFieldValueFlow() }) {
                it.toList().flatten()
            }
    }
}

internal sealed interface SectionFieldElement {
    val identifier: IdentifierSpec

    fun getFormFieldValueFlow(): Flow<List<Pair<IdentifierSpec, FormFieldEntry>>>
    fun sectionFieldErrorController(): SectionFieldErrorController
}

/**
 * This is an element that is in a section and accepts user input.
 */
internal sealed class SectionSingleFieldElement(
    override val identifier: IdentifierSpec,
) : SectionFieldElement {
    /**
     * Some fields in the section will have a single input controller.
     */
    abstract val controller: InputController

    /**
     * Every item in a section must have a controller that can provide an error
     * message, for the section controller to reduce it to a single error message.
     */
    override fun sectionFieldErrorController(): SectionFieldErrorController = controller

    override fun getFormFieldValueFlow(): Flow<List<Pair<IdentifierSpec, FormFieldEntry>>> {
        return controller.formFieldValue.map { formFieldEntry ->
            listOf(identifier to formFieldEntry)
        }
    }

    data class Email(
        val _identifier: IdentifierSpec,
        override val controller: TextFieldController
    ) : SectionSingleFieldElement(_identifier)

    data class Iban(
        val _identifier: IdentifierSpec,
        override val controller: TextFieldController
    ) : SectionSingleFieldElement(_identifier)

    data class Country(
        val _identifier: IdentifierSpec,
        override val controller: DropdownFieldController
    ) : SectionSingleFieldElement(_identifier)

    data class SimpleText(
        val _identifier: IdentifierSpec,
        override val controller: TextFieldController
    ) : SectionSingleFieldElement(_identifier)

    data class SimpleDropdown(
        val _identifier: IdentifierSpec,
        override val controller: DropdownFieldController
    ) : SectionSingleFieldElement(_identifier)

    data class CvcText(
        val _identifier: IdentifierSpec,
        override val controller: CvcTextFieldController,
    ) : SectionSingleFieldElement(_identifier)

    data class CardNumberText(
        val _identifier: IdentifierSpec,
        override val controller: CreditNumberTextFieldController,
    ) : SectionSingleFieldElement(_identifier)
}

internal sealed class SectionMultiFieldElement(
    override val identifier: IdentifierSpec,
) : SectionFieldElement {
    internal open class AddressElement constructor(
        _identifier: IdentifierSpec,
        private val addressFieldRepository: AddressFieldElementRepository,
        countryCodes: Set<String> = emptySet(),
        countryDropdownFieldController: DropdownFieldController = DropdownFieldController(
            CountryConfig(countryCodes)
        ),
    ) : SectionMultiFieldElement(_identifier) {

        @VisibleForTesting
        val countryElement = SectionSingleFieldElement.Country(
            IdentifierSpec("country"),
            countryDropdownFieldController
        )

        private val otherFields = countryElement.controller.rawFieldValue
            .distinctUntilChanged()
            .map { countryCode ->
                addressFieldRepository.get(countryCode)
                    ?: emptyList()
            }

        val fields = otherFields.map { listOf(countryElement).plus(it) }

        val controller = AddressController(fields)

        /**
         * This will return a controller that abides by the SectionFieldErrorController interface.
         */
        override fun sectionFieldErrorController(): SectionFieldErrorController =
            controller

        @ExperimentalCoroutinesApi
        override fun getFormFieldValueFlow() = fields.flatMapLatest { fieldElements ->
            combine(
                fieldElements
                    .associate { sectionFieldElement ->
                        sectionFieldElement.identifier to
                            sectionFieldElement.controller
                    }
                    .map {
                        getCurrentFieldValuePair(it.key, it.value)
                    }
            ) {
                it.toList()
            }
        }

        private fun getCurrentFieldValuePair(
            identifier: IdentifierSpec,
            controller: InputController
        ) = combine(
            controller.rawFieldValue,
            controller.isComplete
        ) { rawFieldValue, isComplete ->
            Pair(
                identifier,
                FormFieldEntry(
                    value = rawFieldValue,
                    isComplete = isComplete,
                )
            )
        }
    }

    internal class CreditDetailElement(
        identifier: IdentifierSpec,
        val controller: CreditElementController = CreditElementController(),
    ) : SectionMultiFieldElement(identifier) {

        /**
         * This will return a controller that abides by the SectionFieldErrorController interface.
         */
        override fun sectionFieldErrorController(): SectionFieldErrorController =
            controller

        override fun getFormFieldValueFlow() = combine(
            controller.numberElement.controller.formFieldValue,
            controller.cvcElement.controller.formFieldValue,
            controller.expirationDateElement.controller.formFieldValue
        ) { number, cvc, expirationDate ->
            listOf(
                controller.numberElement.identifier to number,
                controller.cvcElement.identifier to cvc,
                IdentifierSpec("month") to expirationDate.copy(
                    value = expirationDate.value?.take(2)
                ),
                IdentifierSpec("year") to expirationDate.copy(
                    value = expirationDate.value?.takeLast(2)
                )
            )
        }
    }

    /**
     * This is a special type of AddressElement that
     * removes fields from the address based on the country.  It
     * is only intended to be used with the credit payment method.
     */
    internal class CreditBillingElement(
        identifier: IdentifierSpec,
        addressFieldRepository: AddressFieldElementRepository,
        countryCodes: Set<String> = emptySet(),
        countryDropdownFieldController: DropdownFieldController = DropdownFieldController(
            CountryConfig(countryCodes)
        ),
    ) : AddressElement(
        identifier,
        addressFieldRepository,
        countryCodes,
        countryDropdownFieldController
    ) {
        // Save for future use puts this in the controller rather than element
        val hiddenIdentifiers: Flow<List<IdentifierSpec>> =
            countryDropdownFieldController.rawFieldValue.map { countryCode ->
                when (countryCode) {
                    "US", "GB", "CA" -> {
                        FieldType.values()
                            .filterNot { it == FieldType.PostalCode }
                            .map { it.identifierSpec }
                    }
                    else -> {
                        FieldType.values()
                            .map { it.identifierSpec }
                    }
                }
            }
    }
}
