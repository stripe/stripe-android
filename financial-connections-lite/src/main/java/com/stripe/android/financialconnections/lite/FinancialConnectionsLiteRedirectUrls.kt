package com.stripe.android.financialconnections.lite

import java.net.URI
import java.net.URLDecoder

internal object FinancialConnectionsLiteRedirectUrls {
    fun matchesTerminalUrl(url: String, expectedUrl: String): Boolean {
        val actual = parse(url) ?: return false
        val expected = parse(expectedUrl) ?: return false

        return hasSafeEndpoint(actual) &&
            hasSafeEndpoint(expected) &&
            actual.scheme == expected.scheme &&
            actual.rawAuthority == expected.rawAuthority &&
            actual.rawPath == expected.rawPath &&
            containsExpectedQuery(actual.rawQuery, expected.rawQuery)
    }

    fun isBankAuthReturn(url: String, applicationId: String): Boolean {
        val actual = parse(url) ?: return false
        return hasSafeEndpoint(actual) &&
            actual.scheme == "stripe" &&
            actual.rawAuthority == "financial-connections-lite" &&
            actual.rawPath == "/$applicationId/auth_redirect"
    }

    private fun parse(url: String): URI? = runCatching { URI(url) }.getOrNull()

    private fun hasSafeEndpoint(uri: URI): Boolean =
        !uri.isOpaque &&
            uri.scheme != null &&
            uri.host != null &&
            uri.rawUserInfo == null &&
            uri.rawFragment == null

    private fun containsExpectedQuery(actual: String?, expected: String?): Boolean {
        if (expected == null) return true

        val actualParameters = actual?.split('&') ?: return false
        val actualKeys = actualParameters.map { decodedKey(it) ?: return false }
        val requiredParameters = expected.split('&')
        val requiredKeys = requiredParameters.map { decodedKey(it) ?: return false }
        if (requiredKeys.distinct().size != requiredKeys.size) return false

        return requiredParameters.all { required ->
            val key = decodedKey(required) ?: return false
            actualKeys.count { it == key } == 1 &&
                required in actualParameters
        }
    }

    private fun decodedKey(parameter: String): String? = runCatching {
        URLDecoder.decode(parameter.substringBefore('='), Charsets.UTF_8.name())
    }.getOrNull()
}
