package com.stripe.android.connect.webview.serialization

import com.google.common.truth.Truth.assertThat
import com.stripe.android.connect.EmbeddedComponentError
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
    fun `should serialize and deserialize correctly`() {
        listOf(
            SetterFunctionCalledMessage(SetOnLoaderStart(elementTagName = "foo")) to
                """{"setter":"setOnLoaderStart","value":{"elementTagName":"foo"}}""",
            SetterFunctionCalledMessage(SetOnExit) to
                """{"setter":"setOnExit","value":{}}""",
            SetterFunctionCalledMessage(
                setter = "foo",
                value = SetterFunctionCalledMessage.UnknownValue(value = JsonPrimitive("bar"))
            ) to """{"setter":"foo","value":"bar"}""",
        ).forEach { (obj, expectedJson) ->
            val json = ConnectJson.encodeToString(obj)
            assertThat(json).isEqualTo(expectedJson)
            assertThat(ConnectJson.decodeFromString<SetterFunctionCalledMessage>(json)).isEqualTo(obj)
        }
    }

    @Test
    fun `should serialize and deserialize SetOnLoadError correctly`() {
        val obj = SetterFunctionCalledMessage(
            SetOnLoadError(SetOnLoadError.LoadError(EmbeddedComponentError.ErrorType.API_ERROR, "Test error"))
        )
        val expectedJson =
            """{"setter":"setOnLoadError","value":{"error":{"type":"api_error","message":"Test error"}}}"""
        val json = ConnectJson.encodeToString(obj)
        assertThat(json).isEqualTo(expectedJson)
        assertThat(ConnectJson.decodeFromString<SetterFunctionCalledMessage>(json)).isEqualTo(obj)
    }

    @Test
    fun `should deserialize all EmbeddedComponentError ErrorType values correctly`() {
        EmbeddedComponentError.ErrorType.entries.forEach { errorType ->
            val json = """{"setter":"setOnLoadError","value":{"error":{"type":"${errorType.value}","message":"msg"}}}"""
            val result = ConnectJson.decodeFromString<SetterFunctionCalledMessage>(json)
            val expected = SetterFunctionCalledMessage(
                SetOnLoadError(SetOnLoadError.LoadError(errorType, "msg"))
            )
            assertThat(result).isEqualTo(expected)
        }
    }

    @Test
    fun `should deserialize unknown error type as API_ERROR`() {
        val json = """{"setter":"setOnLoadError","value":{"error":{"type":"unknown_future_error","message":"msg"}}}"""
        val result = ConnectJson.decodeFromString<SetterFunctionCalledMessage>(json)
        val expected = SetterFunctionCalledMessage(
            SetOnLoadError(SetOnLoadError.LoadError(EmbeddedComponentError.ErrorType.API_ERROR, "msg"))
        )
        assertThat(result).isEqualTo(expected)
    }

    @Test
    fun `should deserialize null error type as API_ERROR`() {
        val json = """{"setter":"setOnLoadError","value":{"error":{"type":null,"message":"msg"}}}"""
        val result = ConnectJson.decodeFromString<SetterFunctionCalledMessage>(json)
        val expected = SetterFunctionCalledMessage(
            SetOnLoadError(SetOnLoadError.LoadError(EmbeddedComponentError.ErrorType.API_ERROR, "msg"))
        )
        assertThat(result).isEqualTo(expected)
    }

    @Test
    fun `should deserialize render_error type correctly`() {
        val json = """{"setter":"setOnLoadError","value":{"error":{"type":"render_error","message":"Render failed"}}}"""
        val result = ConnectJson.decodeFromString<SetterFunctionCalledMessage>(json)
        val expected = SetterFunctionCalledMessage(
            SetOnLoadError(SetOnLoadError.LoadError(EmbeddedComponentError.ErrorType.RENDER_ERROR, "Render failed"))
        )
        assertThat(result).isEqualTo(expected)
    }
}
