package com.stripe.android.paymentsheet.forms


data class VisualFieldLayout(val visualFieldLayout: List<Section>) {
    val allFields get() = visualFieldLayout.map { it.field }
}

/**
 * This class is used to define different forms full of fields.
 */
data class FormDataObject(
    val visualFieldLayout: VisualFieldLayout,
    val paramKey: MutableMap<String, Any?>,
)

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

