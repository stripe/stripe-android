package com.stripe.link.core.data.network

/**
 * Represents an outgoing HTTP request with a target URL and body parameters.
 */
data class ApiRequest(
    val url: String,
    val params: Map<String, String> = emptyMap(),
)
