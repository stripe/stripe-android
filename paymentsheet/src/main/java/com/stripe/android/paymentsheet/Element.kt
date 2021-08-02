package com.stripe.android.paymentsheet

import androidx.annotation.VisibleForTesting
import androidx.compose.ui.graphics.Color
import com.stripe.android.paymentsheet.address.AddressFieldElementRepository
import com.stripe.android.paymentsheet.elements.AddressController
import com.stripe.android.paymentsheet.elements.Controller
import com.stripe.android.paymentsheet.elements.CountryConfig
import com.stripe.android.paymentsheet.elements.DropdownFieldController
import com.stripe.android.paymentsheet.elements.InputController
import com.stripe.android.paymentsheet.elements.SaveForFutureUseController
import com.stripe.android.paymentsheet.elements.SectionController
import com.stripe.android.paymentsheet.elements.SectionFieldErrorController
import com.stripe.android.paymentsheet.elements.TextFieldController
import com.stripe.android.paymentsheet.specifications.IdentifierSpec
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

/**
 * This is used to define each section in the visual form layout.
 * Each item in the layout has an identifier and a controller associated with it.
 */
internal sealed class FormElement {
    abstract val identifier: IdentifierSpec
    abstract val controller: Controller?

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
    ) : FormElement()

    /**
     * This is an element that will make elements (as specified by identifier) hidden
     * when "save for future" use is unchecked
     */
    data class SaveForFutureUseElement(
        override val identifier: IdentifierSpec,
        override val controller: SaveForFutureUseController,
        val merchantName: String?
    ) : FormElement()

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
    }
}

/**
 * This will get a map of all pairs of identifier to inputControllers, including the section
 * fields, but not the sections themselves.
 */
internal fun List<FormElement>.getIdInputControllerMap() = this
    .filter { it.controller is InputController }
    .associate { it.identifier to (it.controller as InputController) }
    .plus(
        this
            .filterIsInstance<FormElement.SectionElement>()
            .flatMap { it.fields }
            .filter { it.controller is InputController }
            .associate { it.identifier to it.controller as InputController }
    )

/**
 * This is an element that is in a section and accepts user input.
 */
internal sealed class SectionFieldElement {
    abstract val identifier: IdentifierSpec

    /**
     * Every item in a section must have a controller that can provide an error
     * message, for the section controller to reduce it to a single error message.
     */
    abstract val controller: SectionFieldErrorController

    /**
     * This will return a controller that abides by the SectionFieldErrorController interface.
     */
    fun sectionFieldErrorController(): SectionFieldErrorController = controller

    data class Email(
        override val identifier: IdentifierSpec,
        override val controller: TextFieldController
    ) : SectionFieldElement()

    data class Iban(
        override val identifier: IdentifierSpec,
        override val controller: TextFieldController,
    ) : SectionFieldElement()

    data class Country(
        override val identifier: IdentifierSpec,
        override val controller: DropdownFieldController
    ) : SectionFieldElement()

    data class SimpleText(
        override val identifier: IdentifierSpec,
        override val controller: TextFieldController
    ) : SectionFieldElement()

    data class SimpleDropdown(
        override val identifier: IdentifierSpec,
        override val controller: DropdownFieldController,
    ) : SectionFieldElement()

    internal class AddressElement @VisibleForTesting constructor(
        override val identifier: IdentifierSpec,
        private val addressFieldRepository: AddressFieldElementRepository,
        countryCodes: Set<String> = emptySet(),
        countryDropdownFieldController: DropdownFieldController = DropdownFieldController(
            CountryConfig(countryCodes)
        ),
    ) : SectionFieldElement() {

        @VisibleForTesting
        val countryElement = Country(
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

        override val controller = AddressController(fields)
    }
}
