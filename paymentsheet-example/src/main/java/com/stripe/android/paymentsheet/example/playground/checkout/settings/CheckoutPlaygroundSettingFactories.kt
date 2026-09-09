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
    updateRequest: CheckoutPlaygroundRequestUpdater<Boolean> = {},
    isApplicable: (CheckoutPlaygroundSettingValues) -> Boolean = { true },
    applyFeatureFlags: (Boolean) -> Unit = {},
) = choice(
    key = key,
    displayName = displayName,
    defaultValue = defaultValue,
    options = listOf("On" to true, "Off" to false),
    serialize = Boolean::toString,
    updateRequest = updateRequest,
    isApplicable = isApplicable,
    applyFeatureFlags = applyFeatureFlags,
)

internal inline fun <reified T : Enum<T>> enumChoice(
    key: String,
    displayName: String,
    defaultValue: T,
    noinline updateRequest: CheckoutPlaygroundRequestUpdater<T> = {},
    noinline applyFeatureFlags: (T) -> Unit = {},
) = choice(
    key = key,
    displayName = displayName,
    defaultValue = defaultValue,
    options = enumValues<T>().map { it.name to it },
    serialize = { it.name },
    updateRequest = updateRequest,
    applyFeatureFlags = applyFeatureFlags,
)

internal fun <T> choice(
    key: String,
    displayName: String,
    defaultValue: T,
    options: List<Pair<String, T>>,
    serialize: (T) -> String,
    updateRequest: CheckoutPlaygroundRequestUpdater<T> = {},
    isApplicable: (CheckoutPlaygroundSettingValues) -> Boolean = { true },
    applyFeatureFlags: (T) -> Unit = {},
): CheckoutPlaygroundSettingDefinition.Value<T> {
    return value(
        key = key,
        displayName = displayName,
        defaultValue = defaultValue,
        options = options,
        isApplicable = isApplicable,
        updateRequest = updateRequest,
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
    updateRequest: CheckoutPlaygroundRequestUpdater<T> = {},
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
    updateRequest = updateRequest,
    applyFeatureFlags = applyFeatureFlags,
    encode = encode,
    decode = decode,
)
