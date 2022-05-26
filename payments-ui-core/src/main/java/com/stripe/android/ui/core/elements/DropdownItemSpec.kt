package com.stripe.android.ui.core.elements

import android.os.Parcelable
import androidx.annotation.RestrictTo
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@Parcelize
data class DropdownItemSpec(
    @SerialName("api_value")
    val apiValue: String? = null,

    @SerialName("display_text")
    val displayText: String = "Other"
) : Parcelable
