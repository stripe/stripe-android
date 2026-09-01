package com.stripe.android.paymentsheet.example.playground.checkout.settings

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
        val isVisible: (CheckoutPlaygroundSettings) -> Boolean,
        private val encode: (T) -> String,
        private val decode: (String) -> Result<T>,
    ) : CheckoutPlaygroundSettingDefinition {
        val defaultSerializedValue: String = encode(defaultValue)

        fun serialize(value: T): String = encode(value)

        fun deserialize(value: String): Result<T> = decode(value)

        fun validationError(value: String): String? {
            return decode(value).exceptionOrNull()?.message
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
