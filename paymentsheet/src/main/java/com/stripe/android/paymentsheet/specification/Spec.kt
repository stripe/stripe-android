package com.stripe.android.paymentsheet.specification

import androidx.annotation.StringRes
import androidx.compose.ui.graphics.Color
import com.stripe.android.paymentsheet.specification.FormElementSpec.SectionSpec


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

data class IdentifierSpec(val value: String)

/**
 * This is used to define each section in the visual form layout
 */

sealed class FormElementSpec {
    data class SectionSpec(
        val identifier: IdentifierSpec,
        val field: SectionFieldSpec
    ) : FormElementSpec() {

        sealed class SectionFieldSpec(val identifier: IdentifierSpec) {
            object Name : SectionFieldSpec(IdentifierSpec("name"))

            object Email : SectionFieldSpec(IdentifierSpec("email"))

            object Country : SectionFieldSpec(IdentifierSpec("country"))
        }
    }

    /**
     * This is for elements that do not receive user input
     */
    sealed class StaticSpec : FormElementSpec() {
        data class TextSpec(
            val identifier: IdentifierSpec,
            @StringRes val stringResId: Int,
            val color: Color
        ) : StaticSpec()
    }
}

