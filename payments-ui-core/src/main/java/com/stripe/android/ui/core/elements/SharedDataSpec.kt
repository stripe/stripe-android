package com.stripe.android.ui.core.elements

import kotlinx.serialization.Serializable

@Serializable
internal data class SharedDataSpec(
    val type: String,
    val async: Boolean = false,
    val fields: List<FormItemSpec> = emptyList()
)
