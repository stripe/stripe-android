package com.stripe.android.uicore.elements

import app.cash.turbine.Turbine
import com.google.common.truth.Truth.assertThat
import com.stripe.android.uicore.address.AddressSchemaRegistry
import com.stripe.android.uicore.forms.FormFieldEntry
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
internal class AddressElementUiRegistryTest {
    @Test
    fun `fields are only created for requested countries`() = runTest {
        val requests = Turbine<String?>()
        val registry = AddressElementUiRegistry { countryCode ->
            requests.add(countryCode)
            emptyList()
        }
        requests.expectNoEvents()

        registry.get("US")
        assertThat(requests.awaitItem()).isEqualTo("US")
        registry.get("US")
        requests.expectNoEvents()

        registry.get("CA")
        assertThat(requests.awaitItem()).isEqualTo("CA")
        registry.get("US")
        requests.ensureAllEventsConsumed()
    }

    @Test
    fun `country fields retain values when switching countries`() {
        val registry = AddressElementUiRegistry(AddressSchemaRegistry)
        val usFields = requireNotNull(registry.get("US"))
        usFields.forEach { it.setRawValue(mapOf(FormFieldId.Line1 to "123 Main Street")) }

        registry.get("CA")

        val returnedFields = requireNotNull(registry.get("US"))
        assertThat(returnedFields).isSameInstanceAs(usFields)
        assertThat(returnedFields.flatMap { it.getFormFieldValueFlow().value })
            .contains(FormFieldId.Line1 to FormFieldEntry("123 Main Street", true))
    }

    @Test
    fun `fields are isolated between address forms`() {
        val first = AddressElementUiRegistry(AddressSchemaRegistry)
        val second = AddressElementUiRegistry(AddressSchemaRegistry)
        val firstFields = requireNotNull(first.get("US"))
        val secondFields = requireNotNull(second.get("US"))
        firstFields.forEach { it.setRawValue(mapOf(FormFieldId.Line1 to "123 Main Street")) }

        assertThat(secondFields).isNotSameInstanceAs(firstFields)
        assertThat(secondFields.flatMap { it.getFormFieldValueFlow().value }.map { it.second.value })
            .doesNotContain("123 Main Street")
    }

    @Test
    fun `unknown countries have no fields`() {
        val registry = AddressElementUiRegistry(AddressSchemaRegistry)
        assertThat(registry.get("unknown")).isNull()
        assertThat(registry.get(null)).isNotEmpty()
    }
}
