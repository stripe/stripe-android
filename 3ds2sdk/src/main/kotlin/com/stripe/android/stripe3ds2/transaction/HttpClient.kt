package com.stripe.android.stripe3ds2.transaction

import com.stripe.android.stripe3ds2.exceptions.SDKRuntimeException
import java.io.InputStream

internal interface HttpClient {
    suspend fun doGetRequest(): InputStream?

    @Throws(SDKRuntimeException::class)
    suspend fun doPostRequest(requestBody: String, contentType: String): HttpResponse
}
