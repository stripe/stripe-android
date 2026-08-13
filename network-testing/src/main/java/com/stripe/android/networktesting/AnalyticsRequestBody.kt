package com.stripe.android.networktesting

import com.stripe.android.core.networking.AnalyticsRequest
import java.io.ByteArrayOutputStream

/**
 * The raw form-encoded POST body of an [AnalyticsRequest].
 */
fun AnalyticsRequest.formBodyText(): String {
    val outputStream = ByteArrayOutputStream()
    outputStream.use { writePostBody(it) }
    return outputStream.toString("UTF-8")
}

/**
 * The form-encoded POST body of an [AnalyticsRequest], decoded into params.
 *
 * Repeated keys, such as `product_usage[]`, are collapsed to the last occurrence. Use
 * [formBodyText] when the raw body matters.
 */
fun AnalyticsRequest.formBody(): Map<String, String> = parseUrlEncodedParams(formBodyText())
