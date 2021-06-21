package com.stripe.android.paymentsheet.specifications

import androidx.annotation.StringRes
import androidx.compose.ui.graphics.Color
import com.stripe.android.paymentsheet.specifications.FormElementSpec.SectionSpec

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
data class LayoutSpec(val elements: List<FormElementSpec>) {
    val allFields
        get() = elements.filterIsInstance<SectionSpec>().map { it.field }
}

/**
 * This uniquely identifies a element in the form.
 */
data class IdentifierSpec(val value: String)

/**
 * This is used to define each section in the visual form layout
 */
sealed class FormElementSpec {
    data class SectionSpec(
        val identifier: IdentifierSpec,
        val field: SectionFieldSpec
    ) : FormElementSpec()

    /**
     * This is for elements that do not receive user input
     */
    data class StaticTextSpec(
        val identifier: IdentifierSpec,
        @StringRes val stringResId: Int,
        val color: Color
    ) : FormElementSpec()
}

/**
 * This represents a field in a section.
 */
sealed class SectionFieldSpec(val identifier: IdentifierSpec) {
    object Name : SectionFieldSpec(IdentifierSpec("name"))

    object Email : SectionFieldSpec(IdentifierSpec("email"))

    object Country : SectionFieldSpec(IdentifierSpec("country"))
}