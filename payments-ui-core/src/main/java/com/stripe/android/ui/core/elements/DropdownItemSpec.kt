package com.stripe.android.ui.core.elements

import android.os.Parcelable
import androidx.annotation.RestrictTo
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.Serializable

@Serializable
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@Parcelize
data class DropdownItemSpec(
    val api_value: String? = null,
    val display_text: String = "Other"
) : Parcelable
