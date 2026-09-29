@file:OptIn(com.stripe.android.paymentelement.CheckoutSessionPreview::class)

package com.stripe.android.paymentsheet.example.playground.checkout.settings

import com.stripe.android.model.PaymentMethod
import com.stripe.android.paymentsheet.example.playground.settings.Currency
import com.stripe.android.paymentsheet.example.playground.settings.Merchant

internal class CheckoutPlaygroundPreset internal constructor(
    internal val serializedValues: Map<CheckoutPlaygroundSettingDefinition.Value<*>, String>,
)

internal class CheckoutPlaygroundPresetBuilder {
    private val values = linkedMapOf<CheckoutPlaygroundSettingDefinition.Value<*>, String>()

    fun <T> set(definition: CheckoutPlaygroundSettingDefinition.Value<T>, value: T) {
        values[definition] = definition.serialize(value)
    }

    fun regional(
        merchant: Merchant,
        currency: Currency,
        vararg paymentMethods: PaymentMethod.Type,
    ) {
        set(CheckoutPlaygroundDefinitions.session.merchant, merchant)
        set(CheckoutPlaygroundDefinitions.session.currency, currency)
        set(CheckoutPlaygroundDefinitions.session.automaticPaymentMethods, false)
        set(CheckoutPlaygroundDefinitions.session.paymentMethodTypes, paymentMethods.map(PaymentMethod.Type::code))
    }

    internal fun build(): CheckoutPlaygroundPreset = CheckoutPlaygroundPreset(values.toMap())
}

internal fun checkoutPlaygroundPreset(
    block: CheckoutPlaygroundPresetBuilder.() -> Unit,
): CheckoutPlaygroundPreset = CheckoutPlaygroundPresetBuilder().apply(block).build()

internal sealed interface CheckoutPlaygroundScenario {
    val key: String
    val displayName: String

    data class Group(
        override val key: String,
        override val displayName: String,
        val children: List<CheckoutPlaygroundScenario>,
    ) : CheckoutPlaygroundScenario

    data class Leaf(
        override val key: String,
        override val displayName: String,
        val preset: CheckoutPlaygroundPreset,
    ) : CheckoutPlaygroundScenario
}

internal object CheckoutPlaygroundScenarios {
    val root: CheckoutPlaygroundScenario.Group by lazy {
        group(
            "scenarios",
            "Run scenario",
            CheckoutLpmScenarios.group,
            CheckoutTaxScenarios.group,
            CheckoutSavedPaymentMethodScenarios.group,
            CheckoutElementScenarios.group,
        )
    }

    val groups: List<CheckoutPlaygroundScenario.Group> by lazy {
        fun CheckoutPlaygroundScenario.Group.flatten(): List<CheckoutPlaygroundScenario.Group> {
            return listOf(this) + children.filterIsInstance<CheckoutPlaygroundScenario.Group>().flatMap { it.flatten() }
        }
        root.flatten()
    }

    val leaves: List<CheckoutPlaygroundScenario.Leaf> by lazy {
        fun CheckoutPlaygroundScenario.Group.flatten(): List<CheckoutPlaygroundScenario.Leaf> {
            return children.flatMap { child ->
                when (child) {
                    is CheckoutPlaygroundScenario.Group -> child.flatten()
                    is CheckoutPlaygroundScenario.Leaf -> listOf(child)
                }
            }
        }
        root.flatten()
    }
}

internal fun group(
    key: String,
    name: String,
    vararg children: CheckoutPlaygroundScenario,
) = CheckoutPlaygroundScenario.Group(key, name, children.toList())

internal fun leaf(
    key: String,
    name: String,
    block: CheckoutPlaygroundPresetBuilder.() -> Unit,
) = CheckoutPlaygroundScenario.Leaf(key, name, checkoutPlaygroundPreset(block))
