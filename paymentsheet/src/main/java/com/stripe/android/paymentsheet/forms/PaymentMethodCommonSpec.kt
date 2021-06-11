package com.stripe.android.paymentsheet.forms

/**
 * This class is used to define different forms full of fields.
 */
data class FormSpec(
    val sectionSpecs: List<SectionSpec>,
    val paramKey: MutableMap<String, Any?>,
) {
    val allTypes get() = sectionSpecs.map { it.sectionField }
}

data class SectionSpec(
    val sectionField: SectionFieldSpec
) {
    sealed class SectionFieldSpec(
        // This is the key used in the PaymentMethodCreateParams
        val paymentMethodCreateParamsKey: String,
    ) {
        object Name : SectionFieldSpec("name")

        object Email : SectionFieldSpec("email")

        object Country : SectionFieldSpec("country")
    }
}

