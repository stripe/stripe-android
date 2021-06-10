package com.stripe.android.paymentsheet.forms

/**
 * This class is used to define different forms full of fields.
 */
internal data class FormDataObject(
    val elements: List<Section>,
    val paramKey: MutableMap<String, Any?>,
) {
    val allTypes = mutableListOf<Field>().apply {
        elements.forEach {
            add(it.field)
        }
    }
}

internal sealed class Field(
    // This is the key used in the PaymentMethodCreateParams
    val paymentMethodCreateParamsKey: String,
) {
    object NameInput : Field("name")

    object EmailInput : Field("email")
}

internal data class Section(
    val field: Field
)

