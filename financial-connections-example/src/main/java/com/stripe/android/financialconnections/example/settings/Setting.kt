package com.stripe.android.financialconnections.example.settings

import com.stripe.android.financialconnections.example.Experience
import com.stripe.android.financialconnections.example.Flow
import com.stripe.android.financialconnections.example.data.model.LinkAccountSessionBody
import com.stripe.android.financialconnections.example.data.model.Merchant
import com.stripe.android.financialconnections.example.data.model.PaymentIntentBody

sealed class Setting<T> {
    abstract fun lasRequest(body: LinkAccountSessionBody): LinkAccountSessionBody
    abstract fun paymentIntentRequest(body: PaymentIntentBody): PaymentIntentBody

    abstract fun valueUpdated(currentSettings: List<Setting<*>>, value: T): List<Setting<*>>

    fun replace(currentSettings: List<Setting<*>>, newSetting: Setting<*>): List<Setting<*>> {
        val settingsReplaced = currentSettings.map { if (it::class == newSetting::class) newSetting else it }
        return if (settingsReplaced.contains(newSetting)) settingsReplaced else settingsReplaced + newSetting
    }

    fun saveable(): Saveable<T>? {
        @Suppress("UNCHECKED_CAST")
        return this as? Saveable<T>?
    }

    open fun shouldDisplay(
        merchant: Merchant,
        flow: Flow,
        experience: Experience,
    ): Boolean = true

    abstract val displayName: String
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
    override val selectedOption: T,
    open val options: List<Option<T>>,
) : Setting<T>()

// Multiple choice settings
sealed class MultipleChoiceSetting<T>(
    override val displayName: String,
    override val selectedOption: List<T>,
    open val options: List<Option<T>>
) : Setting<List<T>>()

// Define the option
data class Option<T>(val name: String, val value: T)
