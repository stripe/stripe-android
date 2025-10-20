package com.stripe.android.uicore.elements

import androidx.annotation.RestrictTo
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@Serializable
enum class AddressFieldConfiguration {
    @SerialName("hidden")
    HIDDEN,

    @SerialName("optional")
    OPTIONAL,

    @SerialName("required")
    REQUIRED
}
