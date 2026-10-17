package com.stripe.android.paymentsheet.example.playground.model

import com.stripe.android.paymentsheet.example.playground.settings.ValueEnum
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
enum class FeatureState(override val value: String) : ValueEnum {
    @SerialName("enabled")
    Enabled("enabled"),

    @SerialName("disabled")
    Disabled("disabled"),

    @SerialName("unset")
    Unset("unset");

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
