package com.stripe.android.paymentsheet.example.playground.checkout

import com.stripe.android.paymentsheet.example.playground.settings.PlaygroundSettingDefinition.Displayable.Option
import kotlinx.serialization.json.JsonObjectBuilder
import kotlinx.serialization.json.put

internal interface CheckoutControllerExampleSettingDefinition<T : Any> {
    val key: CheckoutControllerExampleSettingKey
    val displayName: String
    val defaultValue: T

    fun options(settings: CheckoutControllerExampleSettings): List<Option<T>>

    fun encode(value: T): String

    fun decode(value: String): T?

    fun apply(value: T, requestBuilder: JsonObjectBuilder)

    fun displayValue(value: T): String

    fun displayDetails(value: T): List<CheckoutControllerExampleSettingDetail> = emptyList()

    val childDefinitions: List<CheckoutControllerExampleSettingDefinition<*>>
        get() = emptyList()

    fun activeChildDefinitions(value: T): List<CheckoutControllerExampleSettingDefinition<*>> {
        return childDefinitions
    }
}

internal enum class CheckoutControllerExampleSettingKey(
    val savedStateKey: String,
) {
    Customer("customer"),
    AutomaticTax("automatic_tax"),
    ShippingAddressCollection("shipping_address_collection"),
    BillingAddressCollection("billing_address_collection"),
}

internal data class CheckoutControllerExampleSettingDetail(
    val name: String,
    val value: String,
)

internal sealed interface CheckoutControllerExampleCustomer {
    val displayName: String

    data object Guest : CheckoutControllerExampleCustomer {
        override val displayName = "Guest"
    }

    data object New : CheckoutControllerExampleCustomer {
        override val displayName = "New customer"
    }

    data class Existing(
        val customerId: String,
    ) : CheckoutControllerExampleCustomer {
        override val displayName = "Existing customer"
    }
}

internal object CheckoutControllerExampleSettingsDefinition {
    val rootDefinitions: List<CheckoutControllerExampleSettingDefinition<*>> = listOf(
        Customer,
        AutomaticTax,
    )

    object Customer : CheckoutControllerExampleSettingDefinition<CheckoutControllerExampleCustomer> {
        override val key = CheckoutControllerExampleSettingKey.Customer
        override val displayName = "Customer"
        override val defaultValue = CheckoutControllerExampleCustomer.Guest

        override fun options(
            settings: CheckoutControllerExampleSettings,
        ): List<Option<CheckoutControllerExampleCustomer>> {
            return buildList {
                add(
                    Option(
                        CheckoutControllerExampleCustomer.Guest.displayName,
                        CheckoutControllerExampleCustomer.Guest,
                    )
                )
                add(
                    Option(
                        CheckoutControllerExampleCustomer.New.displayName,
                        CheckoutControllerExampleCustomer.New,
                    )
                )
                settings.storedCustomerId?.let { customerId ->
                    add(
                        Option(
                            CheckoutControllerExampleCustomer.Existing(customerId).displayName,
                            CheckoutControllerExampleCustomer.Existing(customerId),
                        )
                    )
                }
            }
        }

        override fun encode(value: CheckoutControllerExampleCustomer): String {
            return when (value) {
                CheckoutControllerExampleCustomer.Guest -> "guest"
                CheckoutControllerExampleCustomer.New -> "new"
                is CheckoutControllerExampleCustomer.Existing -> "existing:${value.customerId}"
            }
        }

        override fun decode(value: String): CheckoutControllerExampleCustomer? {
            return when (value) {
                "guest" -> CheckoutControllerExampleCustomer.Guest
                "new" -> CheckoutControllerExampleCustomer.New
                else -> value.removePrefix("existing:")
                    .takeIf { value.startsWith("existing:") && it.isNotBlank() }
                    ?.let(CheckoutControllerExampleCustomer::Existing)
            }
        }

        override fun apply(
            value: CheckoutControllerExampleCustomer,
            requestBuilder: JsonObjectBuilder,
        ) {
            val customer = when (value) {
                CheckoutControllerExampleCustomer.Guest -> "guest"
                CheckoutControllerExampleCustomer.New -> "new"
                is CheckoutControllerExampleCustomer.Existing -> require(value.customerId.isNotBlank()) {
                    "No customer ID available for existing customer"
                }.let { value.customerId }
            }
            requestBuilder.put("customer", customer)
            if (
                value == CheckoutControllerExampleCustomer.New ||
                value is CheckoutControllerExampleCustomer.Existing
            ) {
                requestBuilder.put("checkout_session_payment_method_save", "enabled")
            }
        }

        override fun displayValue(value: CheckoutControllerExampleCustomer): String {
            return value.displayName
        }

        override fun displayDetails(
            value: CheckoutControllerExampleCustomer,
        ): List<CheckoutControllerExampleSettingDetail> {
            return (value as? CheckoutControllerExampleCustomer.Existing)?.let { customer ->
                listOf(CheckoutControllerExampleSettingDetail("Customer ID", customer.customerId))
            }.orEmpty()
        }
    }

    object AutomaticTax : CheckoutControllerExampleSettingDefinition<Boolean> {
        override val key = CheckoutControllerExampleSettingKey.AutomaticTax
        override val displayName = "Automatic tax"
        override val defaultValue = false
        override val childDefinitions = listOf(ShippingAddressCollection, BillingAddressCollection)

        override fun options(settings: CheckoutControllerExampleSettings): List<Option<Boolean>> {
            return listOf(
                Option("On", true),
                Option("Off", false),
            )
        }

        override fun encode(value: Boolean): String = value.toString()

        override fun decode(value: String): Boolean? = value.toBooleanStrictOrNull()

        override fun apply(value: Boolean, requestBuilder: JsonObjectBuilder) {
            requestBuilder.put("automatic_tax", value)
        }

        override fun displayValue(value: Boolean): String = if (value) "On" else "Off"

        override fun activeChildDefinitions(
            value: Boolean,
        ): List<CheckoutControllerExampleSettingDefinition<*>> {
            return if (value) childDefinitions else emptyList()
        }
    }

    object ShippingAddressCollection : CheckoutControllerExampleSettingDefinition<Boolean> {
        override val key = CheckoutControllerExampleSettingKey.ShippingAddressCollection
        override val displayName = "Shipping"
        override val defaultValue = true

        override fun options(settings: CheckoutControllerExampleSettings): List<Option<Boolean>> {
            return listOf(
                Option("On", true),
                Option("Off", false),
            )
        }

        override fun encode(value: Boolean): String = value.toString()

        override fun decode(value: String): Boolean? = value.toBooleanStrictOrNull()

        override fun apply(value: Boolean, requestBuilder: JsonObjectBuilder) {
            requestBuilder.put("shipping_address_collection", value)
        }

        override fun displayValue(value: Boolean): String = if (value) "On" else "Off"
    }

    object BillingAddressCollection : CheckoutControllerExampleSettingDefinition<Boolean> {
        override val key = CheckoutControllerExampleSettingKey.BillingAddressCollection
        override val displayName = "Billing"
        override val defaultValue = false

        override fun options(settings: CheckoutControllerExampleSettings): List<Option<Boolean>> {
            return listOf(
                Option("On", true),
                Option("Off", false),
            )
        }

        override fun encode(value: Boolean): String = value.toString()

        override fun decode(value: String): Boolean? = value.toBooleanStrictOrNull()

        override fun apply(value: Boolean, requestBuilder: JsonObjectBuilder) {
            if (value) {
                requestBuilder.put("billing_address_collection", true)
            }
        }

        override fun displayValue(value: Boolean): String = if (value) "On" else "Off"
    }
}

internal class CheckoutControllerExampleSettings private constructor(
    private val values: Map<CheckoutControllerExampleSettingDefinition<*>, Any>,
    val storedCustomerId: String?,
) {
    fun <T : Any> withValue(
        definition: CheckoutControllerExampleSettingDefinition<T>,
        value: T,
    ): CheckoutControllerExampleSettings {
        return CheckoutControllerExampleSettings(
            values = values + (definition to value),
            storedCustomerId = storedCustomerId,
        )
    }

    fun withStoredCustomerId(customerId: String): CheckoutControllerExampleSettings {
        return CheckoutControllerExampleSettings(
            values = values,
            storedCustomerId = customerId,
        )
    }

    fun snapshot(): Snapshot {
        return Snapshot(values)
    }

    fun encodedValues(): Map<String, String> {
        return allDefinitions.associate { definition ->
            definition.key.savedStateKey to definition.encodeValue(values.getValue(definition))
        }
    }

    fun activeSettings(): List<ActiveSetting> {
        return activeSettings(CheckoutControllerExampleSettingsDefinition.rootDefinitions, indentation = 0)
    }

    operator fun <T : Any> get(definition: CheckoutControllerExampleSettingDefinition<T>): T {
        return values.valueFor(definition)
    }

    private fun activeSettings(
        definitions: List<CheckoutControllerExampleSettingDefinition<*>>,
        indentation: Int,
    ): List<ActiveSetting> {
        return definitions.flatMap { definition ->
            activeSettings(definition, indentation)
        }
    }

    private fun <T : Any> activeSettings(
        definition: CheckoutControllerExampleSettingDefinition<T>,
        indentation: Int,
    ): List<ActiveSetting> {
        val value = values.valueFor(definition)
        return listOf(
            ActiveSetting(
                definition = definition,
                value = value,
                indentation = indentation,
                displayDetails = definition.displayDetails(value),
            )
        ) + activeSettings(definition.activeChildDefinitions(value), indentation + 1)
    }

    internal data class ActiveSetting(
        val definition: CheckoutControllerExampleSettingDefinition<*>,
        val value: Any,
        val indentation: Int,
        val displayDetails: List<CheckoutControllerExampleSettingDetail>,
    )

    internal class Snapshot internal constructor(
        private val values: Map<CheckoutControllerExampleSettingDefinition<*>, Any>,
    ) {
        fun applyTo(requestBuilder: JsonObjectBuilder) {
            activeSettings().forEach { setting ->
                setting.definition.applyValue(setting.value, requestBuilder)
            }
        }

        fun summaryLines(): List<String> {
            return activeSettings().map { setting ->
                "${setting.definition.displayName}: ${setting.definition.displayValueFor(setting.value)}"
            }
        }

        operator fun <T : Any> get(definition: CheckoutControllerExampleSettingDefinition<T>): T {
            return values.valueFor(definition)
        }

        private fun activeSettings(): List<ActiveSetting> {
            return activeSettings(CheckoutControllerExampleSettingsDefinition.rootDefinitions)
        }

        private fun activeSettings(
            definitions: List<CheckoutControllerExampleSettingDefinition<*>>,
        ): List<ActiveSetting> {
            return definitions.flatMap { definition ->
                activeSettings(definition)
            }
        }

        private fun <T : Any> activeSettings(
            definition: CheckoutControllerExampleSettingDefinition<T>,
        ): List<ActiveSetting> {
            val value = values.valueFor(definition)
            return listOf(
                ActiveSetting(
                    definition = definition,
                    value = value,
                    indentation = 0,
                    displayDetails = definition.displayDetails(value),
                )
            ) + activeSettings(definition.activeChildDefinitions(value))
        }
    }

    companion object {
        fun create(
            persistedValues: Map<String, String>?,
            storedCustomerId: String?,
        ): CheckoutControllerExampleSettings {
            val customerId = storedCustomerId?.takeIf(String::isNotBlank)
            val values = allDefinitions.associateWith { definition ->
                val savedValue = persistedValues?.get(definition.key.savedStateKey)
                when {
                    savedValue != null -> definition.decodeValue(savedValue) ?: definition.defaultValueValue
                    definition == CheckoutControllerExampleSettingsDefinition.Customer && customerId != null -> {
                        CheckoutControllerExampleCustomer.Existing(customerId)
                    }
                    else -> definition.defaultValueValue
                }
            }
            return CheckoutControllerExampleSettings(values, customerId)
        }

        private val allDefinitions: List<CheckoutControllerExampleSettingDefinition<*>> =
            allDefinitions(CheckoutControllerExampleSettingsDefinition.rootDefinitions)

        private fun allDefinitions(
            definitions: List<CheckoutControllerExampleSettingDefinition<*>>,
        ): List<CheckoutControllerExampleSettingDefinition<*>> {
            return definitions.flatMap { definition ->
                listOf(definition) + allDefinitions(definition.childDefinitions)
            }
        }
    }
}

private fun <T : Any> Map<CheckoutControllerExampleSettingDefinition<*>, Any>.valueFor(
    definition: CheckoutControllerExampleSettingDefinition<T>,
): T {
    @Suppress("UNCHECKED_CAST")
    return getValue(definition) as T
}

private fun CheckoutControllerExampleSettingDefinition<*>.encodeValue(value: Any): String {
    @Suppress("UNCHECKED_CAST")
    return (this as CheckoutControllerExampleSettingDefinition<Any>).encode(value)
}

private fun CheckoutControllerExampleSettingDefinition<*>.applyValue(
    value: Any,
    requestBuilder: JsonObjectBuilder,
) {
    @Suppress("UNCHECKED_CAST")
    (this as CheckoutControllerExampleSettingDefinition<Any>).apply(value, requestBuilder)
}

private fun CheckoutControllerExampleSettingDefinition<*>.displayValueFor(value: Any): String {
    @Suppress("UNCHECKED_CAST")
    return (this as CheckoutControllerExampleSettingDefinition<Any>).displayValue(value)
}

private fun CheckoutControllerExampleSettingDefinition<*>.decodeValue(value: String): Any? {
    @Suppress("UNCHECKED_CAST")
    return (this as CheckoutControllerExampleSettingDefinition<Any>).decode(value)
}

private val CheckoutControllerExampleSettingDefinition<*>.defaultValueValue: Any
    get() {
        @Suppress("UNCHECKED_CAST")
        return (this as CheckoutControllerExampleSettingDefinition<Any>).defaultValue
    }
