package com.stripe.android.ui.core.elements

import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class LpmSerializerTest {

    @Test
    fun `Verify lpms json parses with no shared data fields`() {
        val inputStream = SharedDataSpecParcelerTest::class.java.classLoader!!.getResourceAsStream("lpms.json")
        val serializedString = inputStream.bufferedReader().use { it.readText() }

        val result = LpmSerializer.deserializeList(serializedString).getOrThrow()
        val specsWithFields = result.filter { it.fields.isNotEmpty() }

        assertThat(specsWithFields).isEmpty()
    }

    @Test
    fun `Verify supported types api_path parsed correctly`() {
        val types = listOf(
            "email",
            "name",
            "placeholder",
        )

        types.forEach { fieldType ->
            val result = LpmSerializer.deserializeList(
                jsonForField(
                    """
                    {
                      "type": "$fieldType",
                      "api_path": { "v1": "something_bogus" }
                    }
                    """.trimIndent()
                )
            ).getOrThrow().first()

            assertThat(result.fields.first().apiPath.v1).isEqualTo("something_bogus")
        }
    }

    @Test
    fun `Verify supported specs have default api_path parsed correctly`() {
        val types = mapOf(
            "email" to "billing_details[email]",
            "name" to "billing_details[name]",
            "placeholder" to "placeholder",
        )

        types.forEach { (type, expectedApiPath) ->
            val result = LpmSerializer.deserializeList(
                jsonForField("""{ "type": "$type" }""")
            ).getOrThrow().first()

            assertThat(result.fields.first().apiPath.v1).isEqualTo(expectedApiPath)
        }
    }

    @Test
    fun `Verify that unknown field in Json spec deserializes ignoring the field`() {
        val result = LpmSerializer.deserializeList(
            jsonForField(
                """
                {
                  "type": "unknown_field",
                  "unknown_value": { "some_stuff": "some_value" }
                }
                """.trimIndent()
            )
        ).getOrThrow().first()

        assertThat(result.fields).isEqualTo(listOf(PlaceholderSpec()))
    }

    @Test
    fun `Verify that fields default to empty`() {
        val serializedString = """[{ "type": "unknown_lpm" }]"""

        val result = LpmSerializer.deserializeList(serializedString).getOrThrow().first()

        assertThat(result.fields).isEmpty()
    }

    private fun jsonForField(field: String): String {
        return """
            [
              {
                "type": "new_lpm",
                "async": true,
                "fields": [$field]
              }
            ]
        """.trimIndent()
    }
}
