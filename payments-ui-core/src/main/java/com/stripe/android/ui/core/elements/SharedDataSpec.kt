package com.stripe.android.ui.core.elements

import android.os.Parcelable
import androidx.annotation.RestrictTo
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@Parcelize
data class SharedDataSpec(
    @SerialName("type")
    val type: String,

    @SerialName("fields")
    val fields: ArrayList<FormItemSpec> = arrayListOf(),

    @SerialName("selector_icon")
    val selectorIcon: SelectorIcon? = null,
) : Parcelable
