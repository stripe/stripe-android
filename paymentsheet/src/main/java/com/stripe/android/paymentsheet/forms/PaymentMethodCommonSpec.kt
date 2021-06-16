package com.stripe.android.paymentsheet.forms


/**
 * This class is used to define different forms full of fields.
 */
data class FormSpec(
    val layout: FieldLayoutSpec,
    val paramKey: MutableMap<String, Any?>,
) {
    val allTypes get() = layout.sections.map { it.field }
}

/**
 * This is a data representation of the layout of UI fields on the screen.
 */
data class FieldLayoutSpec(val sections: List<SectionSpec>) {
    val allFields get() = sections.map { it.field }
}

/**
 * This is used to define each section in the visual form layout
 */
data class SectionSpec(
    val field: SectionFieldSpec
) {
    sealed class SectionFieldSpec(
        // This is the key used in the PaymentMethodCreateParams
        val identifier: String,
    ) {
        object Name : SectionFieldSpec("name")

        object Email : SectionFieldSpec("email")

        object Country : SectionFieldSpec("country")
    }
}

