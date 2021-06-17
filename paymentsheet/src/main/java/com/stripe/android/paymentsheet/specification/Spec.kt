package com.stripe.android.paymentsheet.specification

import androidx.annotation.StringRes
import androidx.compose.ui.graphics.Color
import com.stripe.android.paymentsheet.elements.common.OptionalElement


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
data class LayoutSpec(val elements: List<FormElementSpec>)

data class IdentifierSpec(val value: String)

/**
 * This is used to define each section in the visual form layout
 */
sealed class FormElementSpec {
    data class SectionSpec(
        override val identifier: IdentifierSpec,
        val field: SectionFieldSpec
    ) : FormElementSpec(), OptionalElement {

        sealed class SectionFieldSpec(val identifier: IdentifierSpec) {
            object Name : SectionFieldSpec(IdentifierSpec("name"))

            object Email : SectionFieldSpec(IdentifierSpec("email"))

            object Country : SectionFieldSpec(IdentifierSpec("country"))
        }
    }

    /**
     * This is for elements that do not receive user input
     */
    sealed class StaticSpec : FormElementSpec(), OptionalElement {
        data class TextSpec(
            override val identifier: IdentifierSpec,
            @StringRes val stringResId: Int,
            val color: Color
        ) : StaticSpec(), OptionalElement
    }

    /**
     * This is an element that will make elements (as specified by identifer hidden
     * when save for future use is unchecked)
     */
    data class SaveForFutureUseSpec(
        val elementsOptionalOnFutureUse: List<OptionalElement>
    ) : FormElementSpec(), OptionalElement {
        override val identifier = IdentifierSpec("save_for_future_use")
    }
}

