package com.stripe.android.paymentsheet.elements

import kotlinx.serialization.Serializable

@Serializable
internal data class DropdownItemSpec(
    val value: String,
    val text: String
)
