package com.stripe.android.ui.core.elements

import kotlinx.serialization.Serializable

@Serializable
internal data class SharedDataSpec(
    val type: String,

    /** Ignored by the SDK */
    val async: Boolean = false,

    // If a form is empty, it must still have an EmptyFormSpec
    // field to get the form into a complete state (i.e. PayPal).
    val fields: List<FormItemSpec> = listOf(EmptyFormSpec)
)
