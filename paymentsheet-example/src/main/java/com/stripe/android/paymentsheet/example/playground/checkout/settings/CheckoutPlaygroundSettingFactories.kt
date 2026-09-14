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
    defaultValue: Boolean = false,
    isApplicable: (CheckoutPlaygroundSettingValues) -> Boolean = { true },
    onValueChanged: CheckoutPlaygroundSettingUpdateScope.(Boolean) -> Unit = {},
    applyFeatureFlags: (Boolean) -> Unit = {},
) = choice(
    key = key,
    displayName = displayName,
    defaultValue = defaultValue,
    options = listOf("On" to true, "Off" to false),
    isApplicable = isApplicable,
    onValueChanged = onValueChanged,
    applyFeatureFlags = applyFeatureFlags,
)

internal inline fun <reified T : Enum<T>> enumChoice(
    key: String,
    displayName: String,
    defaultValue: T,
    noinline onValueChanged: CheckoutPlaygroundSettingUpdateScope.(T) -> Unit = {},
    noinline applyFeatureFlags: (T) -> Unit = {},
) = choice(
    key = key,
    displayName = displayName,
    defaultValue = defaultValue,
    options = enumValues<T>().map { it.name to it },
    serialize = { it.name },
    onValueChanged = onValueChanged,
    applyFeatureFlags = applyFeatureFlags,
)

internal fun <T> choice(
    key: String,
    displayName: String,
    options: List<Pair<String, T>>,
    defaultValue: T = options.first().second,
    serialize: (T) -> String = { it.toString() },
    isApplicable: (CheckoutPlaygroundSettingValues) -> Boolean = { true },
    onValueChanged: CheckoutPlaygroundSettingUpdateScope.(T) -> Unit = {},
    applyFeatureFlags: (T) -> Unit = {},
): CheckoutPlaygroundSettingDefinition.Value<T> {
    return value(
        key = key,
        displayName = displayName,
        defaultValue = defaultValue,
        options = options,
        isApplicable = isApplicable,
        onValueChanged = onValueChanged,
        applyFeatureFlags = applyFeatureFlags,
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
    onValueChanged: CheckoutPlaygroundSettingUpdateScope.(T) -> Unit = {},
    applyFeatureFlags: (T) -> Unit = {},
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
    onValueChanged = onValueChanged,
    applyFeatureFlags = applyFeatureFlags,
    encode = encode,
    decode = decode,
)
