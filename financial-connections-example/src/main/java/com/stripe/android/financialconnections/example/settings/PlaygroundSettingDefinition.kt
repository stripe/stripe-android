package com.stripe.android.financialconnections.example.settings

import com.stripe.android.financialconnections.example.data.LinkAccountSessionBody
import com.stripe.android.financialconnections.example.data.PaymentIntentBody

internal interface PlaygroundSettingDefinition<T> {

    fun displayable(): Displayable<T>? {
        return this as? Displayable<T>?
    }

    fun saveable(): Saveable<T>? {
        @Suppress("UNCHECKED_CAST")
        return this as? Saveable<T>?
    }

    fun lasRequest(
        body: LinkAccountSessionBody,
        value: Any?
    ): LinkAccountSessionBody

    fun paymentIntentRequest(
        body: PaymentIntentBody,
        value: Any?
    ): PaymentIntentBody

    interface Saveable<T> {
        val key: String
        val defaultValue: T
        fun convertToString(value: T): String
        fun convertToValue(value: String): T
    }

    interface Displayable<T> : PlaygroundSettingDefinition<T> {
        val displayName: String
        val options: List<Option<T>>

        fun option(name: String, value: T): Option<T> {
            return Option(name, value)
        }

        data class Option<T>(val name: String, val value: T)
    }
}
