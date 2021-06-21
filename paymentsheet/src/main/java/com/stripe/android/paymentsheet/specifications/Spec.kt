package com.stripe.android.paymentsheet.specifications

import androidx.annotation.StringRes
import androidx.compose.ui.graphics.Color
import com.stripe.android.paymentsheet.specifications.FormItemSpec.SectionSpec

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
data class LayoutSpec(val items: List<FormItemSpec>) {
    val allFields
        get() = items.filterIsInstance<SectionSpec>().map { it.field }
}

data class IdentifierSpec(val value: String)

/**
 * This is used to define each section in the visual form layout
 */

sealed class FormItemSpec {
    data class SectionSpec(
        val identifier: IdentifierSpec,
        val field: SectionFieldSpec
    ) : FormItemSpec() {

    }

    /**
     * This is for elements that do not receive user input
     */
    data class StaticTextSpec(
        val identifier: IdentifierSpec,
        @StringRes val stringResId: Int,
        val color: Color
    ) : FormItemSpec()
}

sealed class SectionFieldSpec(val identifier: IdentifierSpec) {
    object Name : SectionFieldSpec(IdentifierSpec("name"))

    object Email : SectionFieldSpec(IdentifierSpec("email"))

    object Country : SectionFieldSpec(IdentifierSpec("country"))
}