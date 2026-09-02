package com.stripe.android.paymentsheet.example.playground.checkout.settings

import kotlinx.serialization.json.JsonObjectBuilder

internal sealed interface CheckoutPlaygroundSettingDefinition {
    val key: String
    val displayName: String

    class Configuration(
        override val key: String,
        override val displayName: String,
        val children: List<CheckoutPlaygroundSettingDefinition>,
    ) : CheckoutPlaygroundSettingDefinition

    class Value<T>(
        override val key: String,
        override val displayName: String,
        val defaultValue: T,
        val options: List<Option<T>>,
        val input: Input,
        val isApplicable: (CheckoutPlaygroundSettingValues) -> Boolean,
        private val updateRequest: CheckoutPlaygroundRequestUpdater<T> = {},
        private val encode: (T) -> String,
        private val decode: (String) -> Result<T>,
    ) : CheckoutPlaygroundSettingDefinition {
        val defaultSerializedValue: String = encode(defaultValue)

        fun serialize(value: T): String = encode(value)

        fun deserialize(value: String): Result<T> = decode(value)

        fun validationError(value: String): String? {
            return decode(value).exceptionOrNull()?.message
        }

        fun updateRequest(
            request: JsonObjectBuilder,
            settings: CheckoutPlaygroundSettingValues,
        ) {
            updateRequest.invoke(request, settings[this])
        }

        data class Option<T>(
            val displayName: String,
            val value: T,
        )

        enum class Input {
            Text,
            Email,
            Integer,
            Decimal,
            Color,
        }
    }
}

internal typealias CheckoutPlaygroundRequestUpdater<T> = JsonObjectBuilder.(value: T) -> Unit

internal interface CheckoutPlaygroundSettingValues {
    operator fun <T> get(definition: CheckoutPlaygroundSettingDefinition.Value<T>): T
}

internal fun CheckoutPlaygroundSettingDefinition.Configuration.values():
    List<CheckoutPlaygroundSettingDefinition.Value<*>> {
    return children.flatMap { child ->
        when (child) {
            is CheckoutPlaygroundSettingDefinition.Configuration -> child.values()
            is CheckoutPlaygroundSettingDefinition.Value<*> -> listOf(child)
        }
    }
}

internal fun CheckoutPlaygroundSettingDefinition.Configuration.configurations():
    List<CheckoutPlaygroundSettingDefinition.Configuration> {
    return listOf(this) + children
        .filterIsInstance<CheckoutPlaygroundSettingDefinition.Configuration>()
        .flatMap { it.configurations() }
}
