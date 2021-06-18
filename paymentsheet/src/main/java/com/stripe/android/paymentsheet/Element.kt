package com.stripe.android.paymentsheet

import androidx.compose.ui.graphics.Color
import com.stripe.android.paymentsheet.elements.common.Controller
import com.stripe.android.paymentsheet.elements.common.DropdownFieldController
import com.stripe.android.paymentsheet.elements.common.TextFieldController
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
 * This abstract is used to define the types of elements allowed in a section
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
 * This is used to define each section in the visual form layout as an element.
 */
internal sealed class FormElement {
    abstract val controller: Controller?
    abstract val identifier: IdentifierSpec

    /**
     * This is an element that has static text because it takes no user input, it is not
     * outputted from the list of form field values.
     */
    internal data class StaticTextElement(
        override val identifier: IdentifierSpec,
        val stringResId: Int,
        val color: Color,
        override val controller: Controller? = null,
    ) : FormElement()

    /**
     * This is an element that contains another element.
     */
    data class SectionElement(
        override val identifier: IdentifierSpec,
        val field: SectionFieldElementType,
        override val controller: Controller
    ) : FormElement() {

        /**
         * This is an element that is in a section and accepts user input.
         */
        sealed class SectionFieldElement {
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
        }
    }
}