package com.stripe.android.financialconnections.example.settings

import com.stripe.android.financialconnections.example.data.LinkAccountSessionBody
import com.stripe.android.financialconnections.example.data.PaymentIntentBody

internal interface PlaygroundSettingDefinition<T> {

    val displayName: String
    val options: List<Option<T>>

    val key: String
    val defaultValue: T

    fun lasRequest(
        body: LinkAccountSessionBody,
        value: T
    ): LinkAccountSessionBody

    fun paymentIntentRequest(
        body: PaymentIntentBody,
        value: T
    ): PaymentIntentBody

    fun valueUpdated(
        value: T,
        playgroundSettings: PlaygroundSettings
    ): PlaygroundSettings = playgroundSettings

    fun convertToString(value: T): String
    fun convertToValue(value: String): T

    fun option(name: String, value: T): Option<T> {
        return Option(name, value)
    }

    data class Option<T>(val name: String, val value: T)
}
