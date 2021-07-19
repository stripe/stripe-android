package com.stripe.android.paymentsheet

import androidx.compose.ui.graphics.Color
import com.stripe.android.paymentsheet.elements.AddressController
import com.stripe.android.paymentsheet.elements.Controller
import com.stripe.android.paymentsheet.elements.DropdownFieldController
import com.stripe.android.paymentsheet.elements.InputController
import com.stripe.android.paymentsheet.elements.SaveForFutureUseController
import com.stripe.android.paymentsheet.elements.SectionController
import com.stripe.android.paymentsheet.elements.SectionFieldController
import com.stripe.android.paymentsheet.elements.TextFieldController
import com.stripe.android.paymentsheet.specifications.IdentifierSpec
import kotlinx.coroutines.flow.Flow

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

    interface AddressElement : SectionFieldElementType {
        override val controller: AddressController
        val fields: Flow<List<SectionFieldElement>>
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
        val fields: List<SectionFieldElement>,
        override val controller: SectionController
    ) : FormElement(), OptionalElement {
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
            .associate { it.identifier to it.controller }
    )

enum class ControllerType {
    Dropdown,
    Address,
    Text,
    Other
}

enum class ElementType(
    val controllerType: ControllerType
) {
    Name(ControllerType.Text),
    Email(ControllerType.Text),
    Country(ControllerType.Dropdown),
    SaveForFutureUse(ControllerType.Other),
    Mandate(ControllerType.Other),
    IdealBank(ControllerType.Dropdown),
    GenericText(ControllerType.Text),
    Address(ControllerType.Address),
    Section(ControllerType.Other)
}

/**
 * This is an element that is in a section and accepts user input.
 */
internal sealed class SectionFieldElement {
    abstract val identifier: IdentifierSpec
    abstract val controller: SectionFieldController
    abstract fun controllerType(): SectionFieldElementType

    data class Name(
        override val identifier: IdentifierSpec,
        override val controller: TextFieldController,
        override val focusIndexOrder: Int,
    ) : SectionFieldElement(), SectionFieldElementType.TextFieldElement {
        override fun controllerType(): SectionFieldElementType = this
    }

    data class Email(
        override val identifier: IdentifierSpec,
        override val controller: TextFieldController,
        override val focusIndexOrder: Int
    ) : SectionFieldElement(), SectionFieldElementType.TextFieldElement {
        override fun controllerType(): SectionFieldElementType = this
    }

    data class Country(
        override val identifier: IdentifierSpec,
        override val controller: DropdownFieldController
    ) : SectionFieldElement(), SectionFieldElementType.DropdownFieldElement {
        override fun controllerType(): SectionFieldElementType = this
    }

    data class IdealBank internal constructor(
        override val identifier: IdentifierSpec,
        override val controller: DropdownFieldController
    ) : SectionFieldElement(), SectionFieldElementType.DropdownFieldElement {
        override fun controllerType(): SectionFieldElementType = this
    }

    data class SimpleText internal constructor(
        override val identifier: IdentifierSpec,
        override val controller: TextFieldController,
        override val focusIndexOrder: Int
    ) : SectionFieldElement(), SectionFieldElementType.TextFieldElement {
        override fun controllerType(): SectionFieldElementType = this
    }
}
