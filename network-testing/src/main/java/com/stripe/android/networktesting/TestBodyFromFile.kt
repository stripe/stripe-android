package com.stripe.android.networktesting

import okhttp3.mockwebserver.MockResponse
import okio.Buffer

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

    setBody(textBuilder.toString())

    return this
}

fun MockResponse.testBodyFromFile(filename: String): MockResponse {
    addHeader("request-id", filename)

    val inputStream = MockResponse::class.java.classLoader!!.getResourceAsStream(filename)
    val buffer = Buffer()
    buffer.readFrom(inputStream)
    setBody(buffer)

    return this
}
