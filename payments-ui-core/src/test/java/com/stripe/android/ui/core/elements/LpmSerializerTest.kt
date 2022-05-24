package com.stripe.android.ui.core.elements

import com.google.common.truth.Truth.assertThat
import kotlinx.serialization.json.Json
import org.junit.Test
import java.io.File

class LpmSerializerTest {

    private val lpmSerializer = LpmSerializer()

    private val format = Json {
        ignoreUnknownKeys = true
        prettyPrint = true
        encodeDefaults = true
    }

    @Test
    fun `Verify that unknown field in Json spec deserializes - ignoring the field`() {
        val serializedString =
            """
                {
                    "type": "au_becs_debit",
                    "fields": [
                      {
                        "type": "unknown_field",
                        "unknown_value": {
                          "some_stuff": "some_value"
                        }
                      }
                    ]
                  }
            """.trimIndent()

        val result = lpmSerializer.deserialize(serializedString)
        assertThat(result.isSuccess).isTrue()
        result.onSuccess {
            assertThat(it.fields).isEqualTo(
                listOf(EmptyFormSpec)
            )
        }
    }

    @Test
    fun `Verify that async defaults to false and fields to empty`() {
        // TODO: Test this with LPM Repository too
        val serializedString =
            """
                {
                    "type": "unknown_lpm"
                }
            """.trimIndent()

        val result = lpmSerializer.deserialize(serializedString)
        assertThat(result.isSuccess).isTrue()
        result.onSuccess {
            assertThat(it.async).isFalse()
            assertThat(it.fields).isEmpty()
        }
    }

    @Test
    fun `Verify serialize and deserialize successfully`() {
//        NewLpms.values()
//            .forEach { lpm ->
                val jsonElement = lpmSerializer.serialize(NewLpms.AfterpayClearpayJson)
                val serializedString = jsonElement.toString()

                lpmSerializer.deserialize(serializedString)
                    .onSuccess {
                        println(collapsedPrettyPrint(it))
                        assertThat(lpmSerializer.serialize(it).toString())
                            .isEqualTo(serializedString)
                    }
//            }
    }

    @Test
    fun deserializeLpmJsonFile() {
        assertThat(
            lpmSerializer.deserializeList(
                File("src/main/assets/lpms.json")
                    .bufferedReader().use { it.readText() }
            ).size
        ).isEqualTo(14)
    }

    @Test
    fun `Print serialized LPMs`() {
        println("[")
        NewLpms.values()
            .forEach { lpm ->
                println(collapsedPrettyPrint(lpm) + ", ")
            }
        println("]")
    }

    private fun collapsedPrettyPrint(lpm: SharedDataSpec): String {
        val fieldTypeRx = "\\{\\s+\"type\": \"([^\"]*)\"\\s*\\}".toRegex()
        val selectorsRx = (
            "\\{" +
                "\\s*\"api_value\": \"([^\"]*)\",\\s*" +
                "\\s*\"display_text\": \"([^\"]*)\"\\s*" +
                "\\s*\\}"
            ).toRegex()

        var json = format.encodeToString(SharedDataSpec.serializer(), lpm)
        json = fieldTypeRx.replace(
            json,
            "{\"type\":\"$1\"}"
        )
        json = selectorsRx.replace(
            json,
            "{\"display_text\":\"$2\", \"api_value\":\"$1\"}"
        )

        json = (
            "\"api_path\": \\{" +
                "\\s*\"v1\": \"([^\"]*)\"\\s*" +
                "\\s*\\}"
            ).toRegex().replace(
                json,
                "\"api_path\": { \"v1\": \"\$1\"}"
            )

        return json
    }
}
