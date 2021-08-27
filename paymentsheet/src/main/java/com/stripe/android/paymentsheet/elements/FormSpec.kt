package com.stripe.android.paymentsheet.elements

/**
 * This class is used to define different forms full of fields.
 */
internal data class FormSpec(
    val layout: LayoutSpec,
    val paramKey: MutableMap<String, Any?>,
)
