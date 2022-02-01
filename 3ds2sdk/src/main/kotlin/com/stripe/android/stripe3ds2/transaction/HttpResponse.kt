package com.stripe.android.stripe3ds2.transaction

internal class HttpResponse(
    val content: String,
    contentType: String?
) {
    val isJsonContentType = contentType?.startsWith("application/json") == true
}
