package com.stripe.android.paymentsheet.example.playground.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
enum class FeatureState {
    @SerialName("enabled")
    Enabled,

    @SerialName("disabled")
    Disabled;

    companion object {
        fun fromBoolean(value: Boolean): FeatureState {
            return if (value) Enabled else Disabled
        }
    }
}

@Serializable
enum class AllowRedisplayFilter {
    @SerialName("unspecified")
    Unspecified,

    @SerialName("limited")
    Limited,

    @SerialName("always")
    Always
}
