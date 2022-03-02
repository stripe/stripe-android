package com.stripe.android.identity.networking.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
internal data class ConsentParam(
    @SerialName("biometric")
    val biometric: kotlin.Boolean? = null
)
