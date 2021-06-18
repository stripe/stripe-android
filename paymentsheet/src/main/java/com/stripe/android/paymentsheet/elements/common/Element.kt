package com.stripe.android.paymentsheet.elements.common

import androidx.compose.ui.graphics.Color
import com.stripe.android.paymentsheet.specifications.IdentifierSpec

internal class FocusRequesterCount {
    private var value = 0

    fun getAndIncrement() = value++

    fun get() = value
}

interface OptionalElement {
    val identifier: IdentifierSpec
}

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
     * This is an element that will make elements (as specified by identifier hidden
     * when save for future use is unchecked)
     */
    data class StaticTextElement(
        override val identifier: IdentifierSpec,
        val stringResId: Int,
        val color: Color,
        override val controller: Controller? = null,
    ) : FormElement(), OptionalElement

    /**
     * This is an element that will make elements (as specified by identifier) hidden
     * when save for future use is unchecked)
     */
    data class SaveForFutureUseElement(
        override val identifier: IdentifierSpec,
        override val controller: SaveForFutureUseController,
    ) : FormElement()

    data class SectionElement(
        override val identifier: IdentifierSpec,
        val field: SectionFieldElementType,
        override val controller: Controller
    ) : FormElement(), OptionalElement {

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