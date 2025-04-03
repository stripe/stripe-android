package com.stripe.android.networktesting

import okhttp3.mockwebserver.MockResponse
import okio.Buffer
import org.json.JSONException
import org.json.JSONObject

fun MockResponse.testBodyFromFile(
    filename: String,
    replacements: List<ResponseReplacement>,
): MockResponse {
    addHeader("request-id", filename)

    val inputStream = MockResponse::class.java.classLoader!!.getResourceAsStream(filename)
    val textBuilder = StringBuilder()

    val reader = inputStream.reader().buffered()

    reader.forEachLine { line ->
        val replacedText = replacements.fold(line) { acc, replacement ->
            acc.replace(replacement.original, replacement.new)
        }

        textBuilder.append(replacedText)
    }
    val bodyString = textBuilder.toString()
    assertIsValidJsonString(bodyString, filename)
    setBody(bodyString)

    return this
}

fun MockResponse.testBodyFromFile(filename: String): MockResponse {
    addHeader("request-id", filename)

    val inputStream = MockResponse::class.java.classLoader!!.getResourceAsStream(filename)
    val buffer = Buffer()
    buffer.readFrom(inputStream)
    assertIsValidJsonString(buffer.readUtf8(), filename)
    setBody(buffer)

    return this
}

private fun assertIsValidJsonString(json: String, filename: String) {
    try {
        JSONObject(json)
    } catch (_: JSONException) {
        // MockWebServer catches the exception so we need an error to fail the test
        throw AssertionError("Parsing JSON failed for file: $filename")
    }
}
