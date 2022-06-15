package com.stripe.android.ui.core.elements

import androidx.annotation.RestrictTo
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
@Serializable
data class SharedDataSpec(
    @SerialName("type")
    val type: String,

    /** Ignored by the SDK */
    @SerialName("async")
    val async: Boolean = false,

    // If a form is empty, it must still have an EmptyFormSpec
    // field to get the form into a complete state (i.e. PayPal).
    @SerialName("fields")
    val fields: List<FormItemSpec> = listOf(EmptyFormSpec)
)
