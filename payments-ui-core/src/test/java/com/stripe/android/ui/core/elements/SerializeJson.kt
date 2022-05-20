package com.stripe.android.ui.core.elements

import com.google.common.truth.Truth.assertThat
import kotlinx.serialization.json.Json
import org.junit.Test

class SerializeJson {

    private val serializer = Serializer()

    private val format = Json {
        ignoreUnknownKeys = true
        prettyPrint = true
        serializersModule = Serializer.module
        encodeDefaults = true
    }

    @Test
    fun `Verify serialize and deserialize successfully`() {
        NewLpms.values()
            .forEach { lpm ->
                val jsonElement = serializer.serialize(lpm)
                val serializedString = jsonElement.toString()

                serializer.deserialize(serializedString)
                    .onSuccess {
                        println(collapsedPrettyPrint(it))
                        assertThat(serializer.serialize(it).toString())
                            .isEqualTo(serializedString)
                    }
            }
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
            async = false,
            fields = listOf(
                NameSpec(api_path = IdentifierSpec.Generic("billing_details[name]")) as FormItemSpec,
            )
        )
        val serializedString = serializer.serialize(lpm).toString().replace("name", "unknown type")
        println(serializedString)
        serializer.deserialize(serializedString)
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

        json = ("\"api_path\": \\{" +
            "\\s*\"v1\": \"([^\"]*)\"\\s*" +
            "\\s*\\}").toRegex().replace(
            json,
            "\"api_path\": { \"v1\": \"\$1\"}"
        )

        return json
    }
}
