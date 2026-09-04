package com.stripe.android.paymentsheet.example.playground.checkout.settings

import android.content.Context
import android.util.Log
import androidx.core.content.edit
import com.stripe.android.paymentsheet.example.Settings
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.SerializationException
import kotlinx.serialization.builtins.MapSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json

internal class CheckoutPlaygroundSettings private constructor(
    private val root: CheckoutPlaygroundSettingDefinition.Configuration,
    defaultValues: Map<String, String>,
    initialValues: Map<String, String>,
    initialReturningCustomerId: String?,
    private val logWarning: (String) -> Unit,
    private val persist: (Map<String, String>) -> Unit,
    private val persistReturningCustomerId: (String?) -> Unit,
) : CheckoutPlaygroundSettingValues {
    private val definitionsByKey = root.values().associateBy { it.key }
    private val definitionDefaults = definitionsByKey.values.associateWith { it.defaultSerializedValue } +
        defaultValues.sanitized()
    var returningCustomerId: String? = initialReturningCustomerId
        private set
    private val _values = MutableStateFlow(currentDefaults() + initialValues.sanitized())
    val values: StateFlow<Map<CheckoutPlaygroundSettingDefinition.Value<*>, String>> = _values.asStateFlow()

    override operator fun <T> get(definition: CheckoutPlaygroundSettingDefinition.Value<T>): T {
        return definition.deserialize(serializedValue(definition)).getOrThrow()
    }

    fun serializedValue(definition: CheckoutPlaygroundSettingDefinition.Value<*>): String {
        return requireNotNull(_values.value[definition])
    }

    fun <T> update(
        definition: CheckoutPlaygroundSettingDefinition.Value<T>,
        value: T,
    ) {
        updateSerialized(definition, definition.serialize(value))
    }

    fun updateSerialized(
        definition: CheckoutPlaygroundSettingDefinition.Value<*>,
        value: String,
    ) {
        _values.value += (definition to value)
        persist(_values.value.serialized())
    }

    fun reset() {
        _values.value = currentDefaults()
        persist(_values.value.serialized())
    }

    fun applyPreset(preset: CheckoutPlaygroundPreset) {
        _values.value = currentDefaults() + preset.serializedValues
        persist(_values.value.serialized())
    }

    fun saveReturningCustomer(customerId: String) {
        returningCustomerId = customerId
        persistReturningCustomerId(customerId)
        _values.value += mapOf(
            CheckoutPlaygroundDefinitions.session.customerId to customerId,
            CheckoutPlaygroundDefinitions.session.customer to
                CheckoutPlaygroundDefinitions.session.customer.serialize(CheckoutCustomer.Returning),
        )
        persist(_values.value.serialized())
    }

    fun validationErrors(): Map<CheckoutPlaygroundSettingDefinition.Value<*>, String> {
        return _values.value.mapNotNull { (definition, value) ->
            definition.validationError(value)?.let { definition to it }
        }.toMap()
    }

    fun snapshot(): Snapshot {
        check(validationErrors().isEmpty()) { "Cannot snapshot invalid checkout playground settings." }
        return Snapshot(_values.value.toMap())
    }

    fun asJsonString(): String {
        return Json { prettyPrint = true }.encodeToString(
            MapSerializer(String.serializer(), String.serializer()),
            _values.value.serialized(),
        )
    }

    fun importJson(json: String): Result<Unit> = runCatching {
        val importedValues = decodeValuesOrThrow(json)
        val unknownKeys = importedValues.keys - definitionsByKey.keys
        if (unknownKeys.isNotEmpty()) {
            logWarning("Ignoring unknown settings: ${unknownKeys.sorted().joinToString()}")
        }

        val invalidSettings = importedValues.mapNotNull { (key, value) ->
            definitionsByKey[key]?.validationError(value)?.let { key to it }
        }
        require(invalidSettings.isEmpty()) {
            invalidSettings.joinToString(
                prefix = "Invalid settings: ",
                transform = { (key, error) -> "$key ($error)" },
            )
        }

        _values.value = currentDefaults() + importedValues.sanitized()
        persist(_values.value.serialized())
    }

    private fun Map<String, String>.sanitized(): Map<CheckoutPlaygroundSettingDefinition.Value<*>, String> {
        return mapNotNull { (key, value) ->
            definitionsByKey[key]
                ?.takeIf { it.validationError(value) == null }
                ?.let { it to value }
        }.toMap()
    }

    private fun currentDefaults(): Map<CheckoutPlaygroundSettingDefinition.Value<*>, String> {
        val customerId = CheckoutPlaygroundDefinitions.session.customerId
        return definitionDefaults + (customerId to customerId.serialize(returningCustomerId))
    }

    @JvmInline
    value class Snapshot internal constructor(
        private val values: Map<CheckoutPlaygroundSettingDefinition.Value<*>, String>,
    ) : CheckoutPlaygroundSettingValues {
        override operator fun <T> get(definition: CheckoutPlaygroundSettingDefinition.Value<T>): T {
            return definition.deserialize(requireNotNull(values[definition])).getOrThrow()
        }
    }

    companion object {
        private const val PREFERENCES_NAME = "CheckoutControllerPlaygroundSettings"
        private const val PREFERENCES_KEY = "settings_v1"
        private const val RETURNING_CUSTOMER_ID_KEY = "returning_customer_id"
        private const val TAG = "CheckoutSettings"

        fun create(context: Context): CheckoutPlaygroundSettings {
            val preferences = context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)
            val values = preferences.getString(PREFERENCES_KEY, null)?.let(::decodeValues).orEmpty()
            return CheckoutPlaygroundSettings(
                root = CheckoutPlaygroundDefinitions.root,
                defaultValues = mapOf(
                    CheckoutPlaygroundDefinitions.session.backendUrl.key to Settings(context).playgroundBackendUrl
                ),
                initialValues = values,
                initialReturningCustomerId = preferences.getString(RETURNING_CUSTOMER_ID_KEY, null),
                logWarning = { message -> Log.w(TAG, message) },
                persist = { updatedValues ->
                    preferences.edit {
                        putString(
                            PREFERENCES_KEY,
                            Json.encodeToString(
                                MapSerializer(String.serializer(), String.serializer()),
                                updatedValues,
                            ),
                        )
                    }
                },
                persistReturningCustomerId = { customerId ->
                    preferences.edit {
                        if (customerId == null) {
                            remove(RETURNING_CUSTOMER_ID_KEY)
                        } else {
                            putString(RETURNING_CUSTOMER_ID_KEY, customerId)
                        }
                    }
                },
            )
        }

        fun createInMemory(
            json: String? = null,
            persist: (Map<String, String>) -> Unit = {},
        ): CheckoutPlaygroundSettings {
            return CheckoutPlaygroundSettings(
                root = CheckoutPlaygroundDefinitions.root,
                defaultValues = emptyMap(),
                initialValues = json?.let(::decodeValues).orEmpty(),
                initialReturningCustomerId = null,
                logWarning = {},
                persist = persist,
                persistReturningCustomerId = {},
            )
        }

        fun createInMemory(
            json: String?,
            defaultBackendUrl: String,
        ): CheckoutPlaygroundSettings {
            return CheckoutPlaygroundSettings(
                root = CheckoutPlaygroundDefinitions.root,
                defaultValues = mapOf(CheckoutPlaygroundDefinitions.session.backendUrl.key to defaultBackendUrl),
                initialValues = json?.let(::decodeValues).orEmpty(),
                initialReturningCustomerId = null,
                logWarning = {},
                persist = {},
                persistReturningCustomerId = {},
            )
        }

        private fun decodeValues(json: String): Map<String, String> {
            return try {
                decodeValuesOrThrow(json)
            } catch (_: SerializationException) {
                emptyMap()
            } catch (_: IllegalArgumentException) {
                emptyMap()
            }
        }

        private fun decodeValuesOrThrow(json: String): Map<String, String> {
            return Json.decodeFromString(MapSerializer(String.serializer(), String.serializer()), json)
        }
    }
}

private fun Map<CheckoutPlaygroundSettingDefinition.Value<*>, String>.serialized(): Map<String, String> {
    return mapKeys { (definition, _) -> definition.key }
}
