package com.stripe.android.paymentsheet.specifications

import androidx.annotation.StringRes
import androidx.compose.ui.graphics.Color

/**
 * This class is used to define different forms full of fields.
 */
data class FormSpec(
    val layout: LayoutSpec,
    val paramKey: MutableMap<String, Any?>,
)

/**
 * This is a data representation of the layout of UI fields on the screen.
 */
data class LayoutSpec(val items: List<FormItemSpec>)

/**
 * This uniquely identifies a element in the form.
 */
data class IdentifierSpec(val value: String)

/**
 * Identifies a field that can be made optional.
 */
interface OptionalItemSpec {
    val identifier: IdentifierSpec
}

/**
 * This is used to define each section in the visual form layout specification
 */

sealed class FormItemSpec {

    /**
     * This represents a section in a form that contains other elements
     */
    data class SectionSpec(
        override val identifier: IdentifierSpec,
        val fields: List<SectionFieldSpec>,
        @StringRes val title: Int? = null,
    ) : FormItemSpec(), OptionalItemSpec {
        constructor(
            identifier: IdentifierSpec,
            field: SectionFieldSpec,
            title: Int? = null,
        ) : this(identifier, listOf(field), title)
    }

    data class BillingSectionSpec(
        override val identifier: IdentifierSpec,
        //JSON file to read and convert to a spec?
    ) : FormItemSpec(), OptionalItemSpec

    /**
     * This is for elements that do not receive user input
     */
    data class MandateTextSpec(
        override val identifier: IdentifierSpec,
        @StringRes val stringResId: Int,
        val color: Color
    ) : FormItemSpec(), OptionalItemSpec

    /**
     * This is an element that will make elements (as specified by identifier hidden
     * when save for future use is unchecked)
     */
    data class SaveForFutureUseSpec(
        val identifierRequiredForFutureUse: List<OptionalItemSpec>
    ) : FormItemSpec(), OptionalItemSpec {
        override val identifier = IdentifierSpec("save_for_future_use")
    }
}

/**
 * This represents a field in a section.
 */
sealed class SectionFieldSpec(open val identifier: IdentifierSpec) {
    object Name : SectionFieldSpec(IdentifierSpec("name"))

    object Email : SectionFieldSpec(IdentifierSpec("email"))

    /**
     * This is the specification for a country field.
     * @property onlyShowCountryCodes: a list of country code that should be shown.  If empty all
     * countries will be shown.
     */
    data class Country(val onlyShowCountryCodes: Set<String> = emptySet()) :
        SectionFieldSpec(IdentifierSpec("country"))

    object IdealBank : SectionFieldSpec(IdentifierSpec("bank"))

    data class GenericText(
        override val identifier: IdentifierSpec,
        @StringRes val label: Int,
        val isRequired: Boolean
    ) : SectionFieldSpec(identifier)
}
