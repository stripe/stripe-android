package com.stripe.android.networktesting

import okhttp3.mockwebserver.MockResponse
import okio.Buffer

fun MockResponse.testBodyFromFile(filename: String): MockResponse {
    addHeader("request-id", filename)

    val inputStream = MockResponse::class.java.classLoader!!.getResourceAsStream(filename)
    val buffer = Buffer()
    buffer.readFrom(inputStream)
    setBody(buffer)

    return this
}
