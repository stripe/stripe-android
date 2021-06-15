package com.stripe.android.paymentsheet.forms

import androidx.annotation.StringRes
import androidx.compose.ui.graphics.Color
import com.stripe.android.paymentsheet.forms.FormElementSpec.SectionSpec
import com.stripe.android.paymentsheet.forms.FormElementSpec.SectionSpec.SectionFieldSpec


/**
 * This class is used to define different forms full of fields.
 */
data class FormSpec(
    val layout: Layout,
    val paramKey: MutableMap<String, Any?>,
)

/**
 * This is a data representation of the layout of UI fields on the screen.
 */
data class Layout(val elements: List<FormElementSpec>) {
    val allFields
        get() = mutableListOf<SectionFieldSpec>().apply {
            elements.filterIsInstance<SectionSpec>().map { add(it.field) }
        }
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

    sealed class StaticSpec : FormElementSpec() {
        data class TextSpec(
            @StringRes val stringResId: Int,
            val color: Color
        ) : StaticSpec()
    }
}

