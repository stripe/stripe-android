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
    assertIsValidJsonString(buffer.clone().readUtf8(), filename)
    setBody(buffer)

    return this
}

/**
 * Validates JSON syntax and fails the test if the provided JSON string is not valid.
 *
 * This method attempts to parse the JSON string using JSONObject to detect syntax errors
 * such as missing brackets, invalid commas, or malformed structure. Note that this validation
 * only checks syntactic correctness, not semantic accuracy (e.g., it won't detect field name
 * typos or missing required fields).
 *
 * @param json The JSON string to validate
 * @param filename The source filename, used in error reporting for easier debugging
 * @throws AssertionError If JSON parsing fails, with information about the source file
 *
 * Note: For detailed error diagnostics, examine the JSON file in Android Studio, which provides
 * built-in JSON formatting and specific error detection capabilities.
 */
private fun assertIsValidJsonString(json: String, filename: String) {
    try {
        JSONObject(json)
    } catch (_: JSONException) {
        // MockWebServer catches the exception so we need an error to fail the test
        throw AssertionError("Parsing JSON failed for file: $filename")
    }
}
