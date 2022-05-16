package com.stripe.android.ui.core.elements

import android.os.Parcelable
import androidx.annotation.RestrictTo
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.Serializable

@Parcelize
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
data class DropdownItemSpec(
    val api_value: String?,
    val display_text: String
) : Parcelable
