package com.stripe.android.financialconnections.example.settings

import com.stripe.android.financialconnections.example.data.model.LinkAccountSessionBody
import com.stripe.android.financialconnections.example.data.model.PaymentIntentBody

internal interface PlaygroundSettingDefinition<T> {
    val defaultValue: T

    val displayName: String
    val options: List<Option<T>>

    fun saveable(): Saveable<T>? {
        @Suppress("UNCHECKED_CAST")
        return this as? Saveable<T>?
    }

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

    fun option(name: String, value: T): Option<T> {
        return Option(name, value)
    }

    data class Option<T>(val name: String, val value: T)

    interface Saveable<T> : PlaygroundSettingDefinition<T> {
        val key: String
        fun convertToString(value: T): String
        fun convertToValue(value: String): T
    }
}
