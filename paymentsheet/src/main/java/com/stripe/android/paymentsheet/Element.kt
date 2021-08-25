package com.stripe.android.paymentsheet

import androidx.annotation.VisibleForTesting
import androidx.compose.ui.graphics.Color
import com.stripe.android.paymentsheet.address.AddressFieldElementRepository
import com.stripe.android.paymentsheet.elements.AddressController
import com.stripe.android.paymentsheet.elements.Controller
import com.stripe.android.paymentsheet.elements.CountryConfig
import com.stripe.android.paymentsheet.elements.DropdownFieldController
import com.stripe.android.paymentsheet.elements.InputController
import com.stripe.android.paymentsheet.elements.RowController
import com.stripe.android.paymentsheet.elements.SaveForFutureUseController
import com.stripe.android.paymentsheet.elements.SectionController
import com.stripe.android.paymentsheet.elements.SectionFieldErrorController
import com.stripe.android.paymentsheet.elements.TextFieldController
import com.stripe.android.paymentsheet.paymentdatacollection.FormFragmentArguments
import com.stripe.android.paymentsheet.paymentdatacollection.getValue
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
    fun setRawValue(formFragmentArguments: FormFragmentArguments)
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
     * This will return a controller that abides by the SectionFieldErrorController interface.
     */
    override fun sectionFieldErrorController(): SectionFieldErrorController = controller

    override fun getFormFieldValueFlow(): Flow<List<Pair<IdentifierSpec, FormFieldEntry>>> {
        return controller.formFieldValue.map { formFieldEntry ->
            listOf(identifier to formFieldEntry)
        }
    }

    data class Email(
        override val identifier: IdentifierSpec,
        override val controller: TextFieldController
    ) : SectionSingleFieldElement(identifier) {
        override fun setRawValue(formFragmentArguments: FormFragmentArguments) {
            formFragmentArguments.getValue(identifier)?.let { controller.onRawValueChange(it) }
        }
    }

    data class Iban(
        override val identifier: IdentifierSpec,
        override val controller: TextFieldController
    ) : SectionSingleFieldElement(identifier) {
        override fun setRawValue(formFragmentArguments: FormFragmentArguments) {
            formFragmentArguments.getValue(identifier)?.let { controller.onRawValueChange(it) }
        }
    }

    data class Country(
        override val identifier: IdentifierSpec,
        override val controller: DropdownFieldController
    ) : SectionSingleFieldElement(identifier) {
        override fun setRawValue(formFragmentArguments: FormFragmentArguments) {
            formFragmentArguments.getValue(identifier)?.let { controller.onRawValueChange(it) }
        }
    }

    data class SimpleText(
        override val identifier: IdentifierSpec,
        override val controller: TextFieldController
    ) : SectionSingleFieldElement(identifier) {
        override fun setRawValue(formFragmentArguments: FormFragmentArguments) {
            formFragmentArguments.getValue(identifier)?.let { controller.onRawValueChange(it) }
        }
    }

    data class SimpleDropdown(
        override val identifier: IdentifierSpec,
        override val controller: DropdownFieldController
    ) : SectionSingleFieldElement(identifier) {
        override fun setRawValue(formFragmentArguments: FormFragmentArguments) {
            formFragmentArguments.getValue(identifier)?.let { controller.onRawValueChange(it) }
        }
    }
}

internal sealed class SectionMultiFieldElement(
    override val identifier: IdentifierSpec,
) : SectionFieldElement {

    internal class RowElement constructor(
        _identifier: IdentifierSpec,
        val fields: List<SectionSingleFieldElement>,
        val controller: RowController
    ) : SectionMultiFieldElement(_identifier) {
        override fun getFormFieldValueFlow(): Flow<List<Pair<IdentifierSpec, FormFieldEntry>>> =
            combine(fields.map { it.getFormFieldValueFlow() }) {
                it.toList().flatten()
            }

        override fun sectionFieldErrorController() = controller

        override fun setRawValue(formFragmentArguments: FormFragmentArguments) {
            fields.forEach {
                it.setRawValue(formFragmentArguments)
            }
        }
    }

    internal class AddressElement constructor(
        _identifier: IdentifierSpec,
        private val addressFieldRepository: AddressFieldElementRepository,
        private var args: FormFragmentArguments? = null,
        countryCodes: Set<String> = emptySet(),
        countryDropdownFieldController: DropdownFieldController = DropdownFieldController(
            CountryConfig(countryCodes),
            args?.billingDetails?.address?.country
        ),
    ) : SectionMultiFieldElement(_identifier) {

        @VisibleForTesting
        val countryElement = SectionSingleFieldElement.Country(
            IdentifierSpec.Country,
            countryDropdownFieldController
        )

        private val otherFields = countryElement.controller.rawFieldValue
            .distinctUntilChanged()
            .map { countryCode ->
                addressFieldRepository.get(countryCode)
                    ?: emptyList()
            }
            .map { fields ->
                args?.let {
                    fields.forEach { field ->
                        field.setRawValue(it)
                    }
                }
                fields
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
                    .map {
                        it.getFormFieldValueFlow()
                    }
            ) {
                it.toList().flatten()
            }
        }

        override fun setRawValue(formFragmentArguments: FormFragmentArguments) {
            args = formFragmentArguments
        }
    }
}
