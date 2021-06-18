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

data class IdentifierSpec(val value: String)

interface OptionalItemSpec {
    val identifier: IdentifierSpec
}
/**
 * This is used to define each section in the visual form layout
 */

sealed class FormItemSpec {
    data class SectionSpec(
        override val identifier: IdentifierSpec,
        val field: SectionFieldSpec
    ) : FormItemSpec(), OptionalItemSpec {

        sealed class SectionFieldSpec(val identifier: IdentifierSpec) {
            object Name : SectionFieldSpec(IdentifierSpec("name"))

            object Email : SectionFieldSpec(IdentifierSpec("email"))

            object Country : SectionFieldSpec(IdentifierSpec("country"))
        }
    }

    /**
     * This is for elements that do not receive user input
     */
    data class StaticTextSpec(
        override val identifier: IdentifierSpec,
        @StringRes val stringResId: Int,
        val color: Color
    ) : FormItemSpec(), OptionalItemSpec

    /**
     * This is an element that will make elements (as specified by identifer hidden
     * when save for future use is unchecked)
     */
    data class SaveForFutureUseSpec(
        val identifierRequiredForFutureUse: List<OptionalItemSpec>
    ) : FormItemSpec(), OptionalItemSpec {
        override val identifier = IdentifierSpec("save_for_future_use")
    }
}
