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
 * This is used to define each section in the visual form layout
 */

sealed class FormElementSpec {
    data class SectionSpec(val field: SectionFieldSpec) : FormElementSpec() {
        sealed class SectionFieldSpec(val identifier: String) {
            object Name : SectionFieldSpec("name")

            object Email : SectionFieldSpec("email")

            object Country : SectionFieldSpec("country")
        }
    }

    data class StaticTextSpec(
        @StringRes val stringResId: Int,
        val color: Color
    ) : FormElementSpec()
}

