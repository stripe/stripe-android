package com.stripe.android.networktesting

import okhttp3.Headers
import okhttp3.mockwebserver.RecordedRequest
import okio.Buffer
import java.net.Socket
import java.net.URLEncoder

internal class MockRecordedRequestBuilder {
    private var path: String = "/v1/test"
    private var body: String = ""
    private var method: String = "GET"

    fun path(path: String) = apply { this.path = path }
    fun body(body: String) = apply { this.body = body }
    fun method(method: String) = apply { this.method = method }

    fun formBody(vararg params: Pair<String, String>) = apply {
        this.body = params.joinToString("&") { (key, value) ->
            "${URLEncoder.encode(key, "UTF-8")}=${URLEncoder.encode(value, "UTF-8")}"
        }
    }

    fun build(): RecordedRequest {
        val buffer = Buffer().writeUtf8(body)
        return RecordedRequest(
            "$method $path HTTP/1.1",
            Headers.headersOf(),
            emptyList(),
            body.length.toLong(),
            buffer,
            0,
            Socket(),
        )
    }
}
