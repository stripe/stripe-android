package com.stripe.android.networktesting

import java.net.URLDecoder

internal fun urlDecode(value: String): String {
    return URLDecoder.decode(value, Charsets.UTF_8.name())
}

internal fun parseUrlEncodedParams(input: String): Map<String, String> {
    if (input.isBlank()) return emptyMap()
    return input.split("&")
        .filter { it.contains("=") }
        .associate {
            urlDecode(it.substringBefore("=")) to urlDecode(it.substringAfter("="))
        }
}
