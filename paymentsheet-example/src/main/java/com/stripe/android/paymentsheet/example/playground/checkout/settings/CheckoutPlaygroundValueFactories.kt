package com.stripe.android.paymentsheet.example.playground.checkout.settings

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb

internal val optionalEmail: (String) -> String? = { value ->
    if (value.isBlank() || value.matches(Regex("[^@\\s]+@[^@\\s]+\\.[^@\\s]+"))) null else "Invalid email"
}

internal fun text(
    key: String,
    displayName: String,
    defaultValue: String,
    updateRequest: CheckoutPlaygroundRequestUpdater<String> = {},
    validate: (String) -> String? = { null },
): CheckoutPlaygroundSettingDefinition.Value<String> {
    return value(
        key = key,
        displayName = displayName,
        defaultValue = defaultValue,
        updateRequest = updateRequest,
        encode = { it },
        decode = { serialized ->
            validate(serialized)?.let { invalid(message = it) } ?: Result.success(serialized)
        },
    )
}

internal fun optionalText(
    key: String,
    displayName: String,
    updateRequest: CheckoutPlaygroundRequestUpdater<String?> = {},
    validate: (String) -> String? = { null },
): CheckoutPlaygroundSettingDefinition.Value<String?> {
    return optionalText(
        key = key,
        displayName = displayName,
        updateRequest = updateRequest,
        validate = validate,
        isApplicable = { true },
    )
}

internal fun optionalText(
    key: String,
    displayName: String,
    updateRequest: CheckoutPlaygroundRequestUpdater<String?> = {},
    validate: (String) -> String?,
    isApplicable: (CheckoutPlaygroundSettingValues) -> Boolean,
): CheckoutPlaygroundSettingDefinition.Value<String?> {
    return value(
        key = key,
        displayName = displayName,
        defaultValue = null,
        isApplicable = isApplicable,
        updateRequest = updateRequest,
        encode = { it.orEmpty() },
        decode = { serialized ->
            validate(serialized)?.let { invalid(message = it) } ?: Result.success(serialized.trim().ifEmpty { null })
        },
    )
}

internal fun optionalInt(
    key: String,
    displayName: String,
    updateRequest: CheckoutPlaygroundRequestUpdater<Int?> = {},
): CheckoutPlaygroundSettingDefinition.Value<Int?> {
    val minimum = 1
    return value(
        key = key,
        displayName = displayName,
        defaultValue = null,
        input = CheckoutPlaygroundSettingDefinition.Value.Input.Integer,
        updateRequest = updateRequest,
        encode = { it?.toString().orEmpty() },
        decode = { serialized ->
            if (serialized.isBlank()) {
                Result.success(null)
            } else {
                val number = serialized.toIntOrNull() ?: return@value invalid(message = "Enter a whole number")
                if (number < minimum) invalid(message = "Must be at least $minimum") else Result.success(number)
            }
        },
    )
}

internal fun decimal(
    key: String,
    displayName: String,
    defaultValue: Float,
    minimum: Float,
    minimumExclusive: Boolean = false,
    updateRequest: CheckoutPlaygroundRequestUpdater<Float> = {},
): CheckoutPlaygroundSettingDefinition.Value<Float> {
    return value(
        key = key,
        displayName = displayName,
        defaultValue = defaultValue,
        input = CheckoutPlaygroundSettingDefinition.Value.Input.Decimal,
        updateRequest = updateRequest,
        encode = ::formatFloat,
        decode = { serialized ->
            decodeFloat(
                value = serialized,
                minimum = minimum,
                minimumExclusive = minimumExclusive,
            )
        },
    )
}

internal fun optionalFloat(
    key: String,
    displayName: String,
    minimum: Float,
    minimumExclusive: Boolean = false,
    updateRequest: CheckoutPlaygroundRequestUpdater<Float?> = {},
): CheckoutPlaygroundSettingDefinition.Value<Float?> {
    return value(
        key = key,
        displayName = displayName,
        defaultValue = null,
        input = CheckoutPlaygroundSettingDefinition.Value.Input.Decimal,
        updateRequest = updateRequest,
        encode = { it?.let(::formatFloat).orEmpty() },
        decode = { serialized ->
            if (serialized.isBlank()) {
                Result.success(null)
            } else {
                decodeFloat(
                    value = serialized,
                    minimum = minimum,
                    minimumExclusive = minimumExclusive,
                )
            }
        },
    )
}

internal fun optionalColor(
    key: String,
    displayName: String,
    updateRequest: CheckoutPlaygroundRequestUpdater<Color?> = {},
): CheckoutPlaygroundSettingDefinition.Value<Color?> {
    return optionalColor(
        key = key,
        displayName = displayName,
        defaultValue = null,
        updateRequest = updateRequest
    )
}

@Suppress("MagicNumber")
internal fun optionalColor(
    key: String,
    displayName: String,
    defaultValue: Color?,
    updateRequest: CheckoutPlaygroundRequestUpdater<Color?> = {},
): CheckoutPlaygroundSettingDefinition.Value<Color?> {
    return value(
        key = key,
        displayName = displayName,
        defaultValue = defaultValue,
        input = CheckoutPlaygroundSettingDefinition.Value.Input.Color,
        updateRequest = updateRequest,
        encode = { color ->
            color?.toArgb()?.toLong()?.and(0xffffffffL)?.toString(16)?.padStart(8, '0')?.uppercase()
                ?.let { "#$it" }
                .orEmpty()
        },
        decode = { serialized ->
            if (serialized.isBlank()) Result.success(null) else parseColor(value = serialized)
        },
    )
}

internal fun font(
    key: String,
    updateRequest: CheckoutPlaygroundRequestUpdater<CheckoutFont> = {},
) = choice(
    key = key,
    displayName = "Font",
    defaultValue = CheckoutFont.Default,
    options = listOf(
        "Default" to CheckoutFont.Default,
        "Cursive" to CheckoutFont.Cursive,
        "Open Sans" to CheckoutFont.OpenSans,
    ),
    serialize = CheckoutFont::serializedValue,
    updateRequest = updateRequest,
)

internal fun stringCsv(
    key: String,
    displayName: String,
    updateRequest: CheckoutPlaygroundRequestUpdater<List<String>> = {},
): CheckoutPlaygroundSettingDefinition.Value<List<String>> {
    return csv(
        key = key,
        displayName = displayName,
        updateRequest = updateRequest,
        decodeItem = { Result.success(it) },
        encodeItem = { it },
    )
}

internal fun <T> csv(
    key: String,
    displayName: String,
    updateRequest: CheckoutPlaygroundRequestUpdater<List<T>> = {},
    decodeItem: (String) -> Result<T>,
    encodeItem: (T) -> String,
): CheckoutPlaygroundSettingDefinition.Value<List<T>> {
    return value(
        key = key,
        displayName = displayName,
        defaultValue = emptyList(),
        updateRequest = updateRequest,
        encode = { values -> values.joinToString(", ", transform = encodeItem) },
        decode = { serialized ->
            val decoded = serialized
                .split(',')
                .map(String::trim)
                .filter(String::isNotEmpty)
                .distinct()
                .map(decodeItem)
            decoded.firstOrNull { it.isFailure }?.let { failure ->
                Result.failure(requireNotNull(failure.exceptionOrNull()))
            } ?: Result.success(decoded.map { it.getOrThrow() })
        },
    )
}
