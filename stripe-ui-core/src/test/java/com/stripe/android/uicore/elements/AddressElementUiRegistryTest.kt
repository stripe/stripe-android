package com.stripe.android.uicore.elements

import app.cash.turbine.Turbine
import com.google.common.truth.Truth.assertThat
import com.stripe.android.uicore.address.AddressSchemaDefinition
import com.stripe.android.uicore.address.CountryAddressSchema
import com.stripe.android.uicore.address.FieldType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class AddressElementUiRegistryTest {
    @Test
    fun `construction does not transform any schemas`() = runScenario {
        schemas.forEach { it.schemaElementsCalls.expectNoEvents() }
    }

    @Test
    fun `requesting US only constructs US fields and reuses their controllers`() = runScenario {
        val fields = requireNotNull(registry.get("US"))
        val line1 = fields.single() as SimpleTextElement

        assertThat(line1.identifier).isEqualTo(FormFieldId.Line1)
        assertThat(registry.get("US")).isSameInstanceAs(fields)
        assertThat((registry.get("US")?.single() as SimpleTextElement).controller)
            .isSameInstanceAs(line1.controller)
        us.schemaElementsCalls.awaitItem()
    }

    @Test
    fun `returning to a country preserves edits and constructs each country once`() = runScenario {
        val usFields = requireNotNull(registry.get("US"))
        val usController = (usFields.single() as SimpleTextElement).controller
        usController.onRawValueChange("123 Main Street")

        val caFields = requireNotNull(registry.get("CA"))
        assertThat((caFields.single() as SimpleTextElement).controller.rawFieldValue.value).isEmpty()
        assertThat(registry.get("US")).isSameInstanceAs(usFields)
        assertThat(usController.rawFieldValue.value).isEqualTo("123 Main Street")
        us.schemaElementsCalls.awaitItem()
        ca.schemaElementsCalls.awaitItem()
    }

    @Test
    fun `separate registries have independent mutable fields`() = runScenario {
        val otherRegistry = AddressElementUiRegistry(schemas.associateBy { it.countryCode }, defaultSchema)
        val controller = (registry.get("US")?.single() as SimpleTextElement).controller
        val otherController = (otherRegistry.get("US")?.single() as SimpleTextElement).controller

        controller.onRawValueChange("123 Main Street")

        assertThat(otherController).isNotSameInstanceAs(controller)
        assertThat(otherController.rawFieldValue.value).isEmpty()
        us.schemaElementsCalls.awaitItem()
        us.schemaElementsCalls.awaitItem()
    }

    @Test
    fun `null country constructs and caches the default schema`() = runScenario {
        val fields = requireNotNull(registry.get(null))

        assertThat(fields.single().identifier).isEqualTo(FormFieldId.Line1)
        assertThat(registry.get(null)).isSameInstanceAs(fields)
        defaultSchema.schemaElementsCalls.awaitItem()
    }

    @Test
    fun `explicit ZZ owns separate fields from null country`() = runScenario {
        val defaultFields = requireNotNull(registry.get(null))
        val explicitFields = requireNotNull(registry.get("ZZ"))
        val defaultController = (defaultFields.single() as SimpleTextElement).controller
        val explicitController = (explicitFields.single() as SimpleTextElement).controller

        defaultController.onRawValueChange("123 Main Street")

        assertThat(explicitController).isNotSameInstanceAs(defaultController)
        assertThat(explicitController.rawFieldValue.value).isEmpty()
        assertThat(registry.get("ZZ")).isSameInstanceAs(explicitFields)
        defaultSchema.schemaElementsCalls.awaitItem()
        defaultSchema.schemaElementsCalls.awaitItem()
    }

    @Test
    fun `unknown country returns null without transforming schemas`() = runScenario {
        assertThat(registry.get("unknown")).isNull()
        assertThat(registry.get("unknown")).isNull()
    }

    @Test
    fun `empty schema is cached`() = runScenario {
        val fields = registry.get("AQ")

        assertThat(fields).isEmpty()
        assertThat(registry.get("AQ")).isSameInstanceAs(fields)
        emptySchema.schemaElementsCalls.awaitItem()
    }

    @Test
    fun `concurrent requests reuse the same fields`() = runScenario {
        val results = coroutineScope {
            List(32) {
                async(Dispatchers.Default) { registry.get("US") }
            }.awaitAll()
        }

        assertThat(results.first()).isNotEmpty()
        results.forEach { assertThat(it).isSameInstanceAs(results.first()) }
        us.schemaElementsCalls.awaitItem()
    }

    private fun runScenario(block: suspend Scenario.() -> Unit) = runTest {
        val schemaElements = listOf(
            CountryAddressSchema(type = FieldType.AddressLine1, required = true, schema = null)
        )
        val us = FakeAddressSchemaDefinition("US", schemaElements)
        val ca = FakeAddressSchemaDefinition("CA", schemaElements)
        val defaultSchema = FakeAddressSchemaDefinition("ZZ", schemaElements)
        val emptySchema = FakeAddressSchemaDefinition("AQ", emptyList())
        val schemas = listOf(us, ca, defaultSchema, emptySchema)
        val registry = AddressElementUiRegistry(schemas.associateBy { it.countryCode }, defaultSchema)

        Scenario(registry, us, ca, defaultSchema, emptySchema, schemas).block()

        schemas.forEach { it.ensureAllEventsConsumed() }
    }

    private data class Scenario(
        val registry: AddressElementUiRegistry,
        val us: FakeAddressSchemaDefinition,
        val ca: FakeAddressSchemaDefinition,
        val defaultSchema: FakeAddressSchemaDefinition,
        val emptySchema: FakeAddressSchemaDefinition,
        val schemas: List<FakeAddressSchemaDefinition>,
    )
}

internal class FakeAddressSchemaDefinition(
    override val countryCode: String,
    private val elements: List<CountryAddressSchema>,
) : AddressSchemaDefinition {
    val schemaElementsCalls = Turbine<Unit>()

    override fun schemaElements(): List<CountryAddressSchema> {
        schemaElementsCalls.add(Unit)
        return elements
    }

    fun ensureAllEventsConsumed() {
        schemaElementsCalls.ensureAllEventsConsumed()
    }
}
