package com.stripe.android.paymentsheet

import androidx.compose.ui.graphics.Color
import com.stripe.android.paymentsheet.elements.Controller
import com.stripe.android.paymentsheet.elements.DropdownFieldController
import com.stripe.android.paymentsheet.elements.InputController
import com.stripe.android.paymentsheet.elements.SaveForFutureUseController
import com.stripe.android.paymentsheet.elements.SectionController
import com.stripe.android.paymentsheet.elements.TextFieldController
import com.stripe.android.paymentsheet.specifications.IdentifierSpec

/**
 * This interface is used to define the types of elements allowed in a section
 */
internal sealed interface SectionFieldElementType {
    val identifier: IdentifierSpec
    val controller: InputController

    interface TextFieldElement : SectionFieldElementType {
        override val controller: TextFieldController
    }

    interface DropdownFieldElement : SectionFieldElementType {
        override val controller: DropdownFieldController
    }
}

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
        val fields: List<SectionFieldElementType>,
        override val controller: SectionController
    ) : FormElement() {
        internal constructor(
            identifier: IdentifierSpec,
            field: SectionFieldElementType,
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
            .associate { it.identifier to it.controller }
    )

/**
 * This is an element that is in a section and accepts user input.
 */
internal sealed class SectionFieldElement {
    abstract val identifier: IdentifierSpec
    abstract val controller: InputController

    data class Name(
        override val identifier: IdentifierSpec,
        override val controller: TextFieldController
    ) : SectionFieldElement(), SectionFieldElementType.TextFieldElement

    data class Email(
        override val identifier: IdentifierSpec,
        override val controller: TextFieldController
    ) : SectionFieldElement(), SectionFieldElementType.TextFieldElement

    data class Iban(
        override val identifier: IdentifierSpec,
        override val controller: TextFieldController
    ) : SectionFieldElement(), SectionFieldElementType.TextFieldElement

    data class Country(
        override val identifier: IdentifierSpec,
        override val controller: DropdownFieldController
    ) : SectionFieldElement(), SectionFieldElementType.DropdownFieldElement

    data class IdealBank internal constructor(
        override val identifier: IdentifierSpec,
        override val controller: DropdownFieldController
    ) : SectionFieldElement(), SectionFieldElementType.DropdownFieldElement

    data class SimpleText internal constructor(
        override val identifier: IdentifierSpec,
        override val controller: TextFieldController
    ) : SectionFieldElement(), SectionFieldElementType.TextFieldElement
}
