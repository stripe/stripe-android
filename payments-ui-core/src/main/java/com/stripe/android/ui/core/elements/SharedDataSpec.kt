package com.stripe.android.ui.core.elements

import androidx.annotation.RestrictTo
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
data class SharedDataSpec(
    @SerialName("type")
    val type: String,

    /** Ignored by the SDK */
    @SerialName("async")
    val async: Boolean = false,

    // If a form is empty, it must still have an EmptyFormSpec
    // field to get the form into a complete state (i.e. PayPal).
    @SerialName("fields")
    val fields: ArrayList<FormItemSpec> = arrayListOf(EmptyFormSpec),

    @SerialName("next_action_spec")
    val nextActionSpec: NextActionSpec? = null,

    @SerialName("selector_icon")
    val selectorIcon: SelectorIcon? = null,
)
