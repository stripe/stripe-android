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
        serializersModule = LpmSerializer.module
        encodeDefaults = true
    }

    @Test
    fun `Verify that unknown field in Json spec deserializes ignoring the field`() {
        val serializedString =
            """
                {
                    "type": "au_becs_debit",
                    "async": false,
                    "fields": [
                      {
                        "type": "unknown_field",
                        "api_path": {
                          "v1": "billing_details[name]"
                        },
                        "label": "upe.labels.name.onAccount"
                      }
                    ]
                  }
            """.trimIndent()

        lpmSerializer.deserialize(serializedString)
    }

    @Test
    fun `Verify serialize and deserialize successfully`() {
        NewLpms.values()
            .forEach { lpm ->
                val jsonElement = lpmSerializer.serialize(lpm)
                val serializedString = jsonElement.toString()

                lpmSerializer.deserialize(serializedString)
                    .onSuccess {
                        println(collapsedPrettyPrint(it))
                        assertThat(lpmSerializer.serialize(it).toString())
                            .isEqualTo(serializedString)
                    }
            }
    }

    @Test
    fun deserializeLpmJsonFile() {
        lpmSerializer.deserializeList(
            File("src/main/assets/lpms.json")
                .bufferedReader().use { it.readText() }
        )
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

    @Test
    fun `deserialize unknown property on field`() {
    }

    @Test
    fun `Verify deserialize unknown type`() {
        val lpm = SharedDataSpec(
            "bancontact",
            fields = listOf(
                NameSpec(api_path = IdentifierSpec.Generic("billing_details[name]")) as FormItemSpec,
            )
        )
        val serializedString =
            lpmSerializer.serialize(lpm).toString().replace("name", "unknown type")
        println(serializedString)
        lpmSerializer.deserialize(serializedString)
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
