package com.stripe.android.networktesting

import okhttp3.Headers
import okhttp3.HttpUrl
import okhttp3.mockwebserver.RecordedRequest
import java.util.concurrent.atomic.AtomicReference

class TestRecordedRequest(private val recordedRequest: RecordedRequest) {
    private val _bodyText = AtomicReference<String?>()

    val method: String? = recordedRequest.method
    val headers: Headers = recordedRequest.headers
    val path: String = recordedRequest.path ?: ""

    val bodyText: String
        get() {
            if (_bodyText.get() == null) {
                synchronized(_bodyText) {
                    if (_bodyText.get() == null) {
                        val actual = recordedRequest.body.readUtf8()
                        _bodyText.set(actual)
                    }
                }
            }
            return _bodyText.get()!!
        }

    fun queryParameterValues(name: String): List<String?> {
        val url = HttpUrl.Builder()
            .scheme("https")
            .host("example.com")
            .encodedPath(path.substringBefore("?"))
            .encodedQuery(path.substringAfter("?"))
            .build()
        return url.queryParameterValues(name)
    }
}
