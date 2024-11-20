package com.stripe.android.connect.webview.serialization

import com.google.common.truth.Truth.assertThat
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.JsonPrimitive
import org.junit.Test

class SetterFunctionCalledMessageTest {
    @Test(expected = IllegalArgumentException::class)
    fun `should error if setter and value class names don't match`() {
        SetterFunctionCalledMessage(
            setter = "foo",
            value = SetOnLoaderStart(elementTagName = "")
        )
    }

    @Test
    fun `should not validate naming for unknown values`() {
        SetterFunctionCalledMessage(
            setter = "foo",
            value = SetterFunctionCalledMessage.UnknownValue(JsonPrimitive(""))
        )
    }

    @Test
    fun `should serialize and deserialize correct`() {
        listOf(
            SetterFunctionCalledMessage(
                setter = "setOnLoaderStart",
                value = SetOnLoaderStart(elementTagName = "foo")
            ) to """{"setter":"setOnLoaderStart","value":{"elementTagName":"foo"}}""",
            SetterFunctionCalledMessage(
                setter = "foo",
                value = SetterFunctionCalledMessage.UnknownValue(value = JsonPrimitive("bar"))
            ) to """{"setter":"foo","value":"bar"}""",
        ).forEach { (obj, expectedJson) ->
            val json = ConnectSdkJson.encodeToString(obj)
            assertThat(json).isEqualTo(expectedJson)
            assertThat(ConnectSdkJson.decodeFromString<SetterFunctionCalledMessage>(json)).isEqualTo(obj)
        }
    }
}
