package com.stripe.android.paymentsheet

import androidx.compose.ui.graphics.Color
import com.stripe.android.paymentsheet.elements.Controller
import com.stripe.android.paymentsheet.elements.DropdownFieldController
import com.stripe.android.paymentsheet.elements.SaveForFutureUseController
import com.stripe.android.paymentsheet.elements.SectionController
import com.stripe.android.paymentsheet.elements.TextFieldController
import com.stripe.android.paymentsheet.forms.FormFieldValues
import com.stripe.android.paymentsheet.specifications.IdentifierSpec

/**
 * This is used to track the number of focus requesters in a form
 */
internal class FocusRequesterCount {
    private var value = 0

    fun getAndIncrement() = value++

    fun get() = value
}

/**
 * This is used to define which elements can be made optional
 */
internal interface OptionalElement {
    val identifier: IdentifierSpec
}

/**
 * This interface is used to define the types of elements allowed in a section
 */
internal sealed interface SectionFieldElementType {
    val identifier: IdentifierSpec
    val controller: Controller

    interface TextFieldElement : SectionFieldElementType {
        override val controller: TextFieldController
        val focusIndexOrder: Int
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
    abstract val controller: Controller?
    abstract val identifier: IdentifierSpec

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
        override val controller: Controller? = null,
    ) : FormElement(), OptionalElement

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
        val field: SectionFieldElementType,
        override val controller: SectionController
    ) : FormElement(), OptionalElement
}

internal fun List<FormElement>.idControllerMap() = this
    .filter { it.controller != null }
    .associate { it.identifier to it.controller!! }
    .plus(
        this
            .filterIsInstance<FormElement.SectionElement>()
            .associate { it.field.identifier to it.field.controller }
    )

/**
 * This class defines the type associated with the element or value.   See [FormFieldValues] and [Controller]
 */
enum class ElementType {
    Name,
    Email,
    Country,
    SaveForFutureUse,
    Mandate,
    IdealBank,
    Section,
}

/**
 * This is an element that is in a section and accepts user input.
 */
internal sealed class SectionFieldElement {
    abstract val identifier: IdentifierSpec
    abstract val controller: Controller

    data class Name(
        override val identifier: IdentifierSpec,
        override val controller: TextFieldController,
        override val focusIndexOrder: Int
    ) : SectionFieldElement(), SectionFieldElementType.TextFieldElement

    data class Email(
        override val identifier: IdentifierSpec,
        override val controller: TextFieldController,
        override val focusIndexOrder: Int
    ) : SectionFieldElement(), SectionFieldElementType.TextFieldElement

    data class Country(
        override val identifier: IdentifierSpec,
        override val controller: DropdownFieldController
    ) : SectionFieldElement(), SectionFieldElementType.DropdownFieldElement

    data class IdealBank internal constructor(
        override val identifier: IdentifierSpec,
        override val controller: DropdownFieldController
    ) : SectionFieldElement(), SectionFieldElementType.DropdownFieldElement
}
