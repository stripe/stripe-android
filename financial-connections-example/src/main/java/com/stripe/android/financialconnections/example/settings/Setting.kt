package com.stripe.android.financialconnections.example.settings

import com.stripe.android.financialconnections.example.data.model.LinkAccountSessionBody
import com.stripe.android.financialconnections.example.data.model.PaymentIntentBody

sealed class Setting<T> {
    abstract fun lasRequest(body: LinkAccountSessionBody): LinkAccountSessionBody
    abstract fun paymentIntentRequest(body: PaymentIntentBody): PaymentIntentBody

    abstract fun valueUpdated(currentSettings: List<Setting<*>>, value: T): List<Setting<*>>

    fun replace(currentSettings: List<Setting<*>>, newSetting: Setting<*>): List<Setting<*>> = currentSettings
        .map { if (it::class == newSetting::class) { newSetting } else { it } }

    fun saveable(): Saveable<T>? {
        return this as? Saveable<T>?
    }

    abstract val displayName: String
    abstract val options: List<Option<T>>
    abstract val selectedOption: T
}

interface Saveable<T> {
    val key: String
    fun convertToString(value: T): String?
    fun convertToValue(value: String): T

}

// Single choice settings
sealed class SingleChoiceSetting<T>(
    override val displayName: String,
    override val options: List<Option<T>>,
    override val selectedOption: T,
) : Setting<T>()

// Multiple choice settings
sealed class MultipleChoiceSetting<T>(
    override val displayName: String,
    override val options: List<Option<List<T>>>,
    override val selectedOption: List<T>
) : Setting<List<T>>()

// Define the option
data class Option<T>(val name: String, val value: T)

