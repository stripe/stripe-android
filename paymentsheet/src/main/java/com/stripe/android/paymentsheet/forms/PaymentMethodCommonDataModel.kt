package com.stripe.android.paymentsheet.forms

/**
 * This class is used to define different forms full of fields.
 */
data class FormDataObject(
    val sections: List<Section>,
    val paramKey: MutableMap<String, Any?>,
) {
    val allTypes get() = sections.map { it.field }
}

sealed class Field(
    // This is the key used in the PaymentMethodCreateParams
    val identifier: String,
) {
    object NameInput : Field("name")

    object EmailInput : Field("email")

    object CountryInput : Field("country")
}

data class Section(
    val field: Field
)

