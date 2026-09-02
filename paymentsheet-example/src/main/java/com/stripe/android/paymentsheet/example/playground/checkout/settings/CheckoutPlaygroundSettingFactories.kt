package com.stripe.android.paymentsheet.example.playground.checkout.settings

internal fun configuration(
    key: String,
    displayName: String,
    vararg children: CheckoutPlaygroundSettingDefinition,
) = CheckoutPlaygroundSettingDefinition.Configuration(
    key = key,
    displayName = displayName,
    children = children.toList(),
)

internal fun boolean(
    key: String,
    displayName: String,
    defaultValue: Boolean,
    isApplicable: (CheckoutPlaygroundSettingValues) -> Boolean = { true },
) = choice(
    key = key,
    displayName = displayName,
    defaultValue = defaultValue,
    options = listOf("On" to true, "Off" to false),
    serialize = Boolean::toString,
    isApplicable = isApplicable,
)

internal inline fun <reified T : Enum<T>> enumChoice(
    key: String,
    displayName: String,
    defaultValue: T,
) = choice(
    key = key,
    displayName = displayName,
    defaultValue = defaultValue,
    options = enumValues<T>().map { it.name to it },
    serialize = { it.name },
)

internal fun <T> choice(
    key: String,
    displayName: String,
    defaultValue: T,
    options: List<Pair<String, T>>,
    serialize: (T) -> String,
    isApplicable: (CheckoutPlaygroundSettingValues) -> Boolean = { true },
): CheckoutPlaygroundSettingDefinition.Value<T> {
    return value(
        key = key,
        displayName = displayName,
        defaultValue = defaultValue,
        options = options,
        isApplicable = isApplicable,
        encode = serialize,
        decode = { serialized ->
            options.firstOrNull { (_, option) -> serialize(option) == serialized }
                ?.second
                ?.let(Result.Companion::success)
                ?: invalid(message = "Unknown value: $serialized")
        },
    )
}

internal fun <T> value(
    key: String,
    displayName: String,
    defaultValue: T,
    isApplicable: (CheckoutPlaygroundSettingValues) -> Boolean = { true },
    options: List<Pair<String, T>> = emptyList(),
    input: CheckoutPlaygroundSettingDefinition.Value.Input = CheckoutPlaygroundSettingDefinition.Value.Input.Text,
    encode: (T) -> String,
    decode: (String) -> Result<T>,
) = CheckoutPlaygroundSettingDefinition.Value(
    key = key,
    displayName = displayName,
    defaultValue = defaultValue,
    options = options.map { (name, optionValue) ->
        CheckoutPlaygroundSettingDefinition.Value.Option(
            displayName = name,
            value = optionValue,
        )
    },
    input = input,
    isApplicable = isApplicable,
    encode = encode,
    decode = decode,
)
