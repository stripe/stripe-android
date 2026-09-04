package com.stripe.android.networktesting

fun interface RequestMatcher {
    fun matches(request: TestRecordedRequest): Boolean
}

private class ToStringRequestMatcher(
    private val friendlyName: String,
    private val requestMatcher: RequestMatcher,
) : RequestMatcher by requestMatcher {
    override fun toString(): String {
        return friendlyName
    }
}

internal class CompositeRequestMatcher(
    private val matchers: List<RequestMatcher>,
) : RequestMatcher {
    override fun matches(request: TestRecordedRequest): Boolean {
        return matchers.all { it.matches(request) }
    }

    fun passCount(request: TestRecordedRequest): Int {
        return matchers.count { it.matches(request) }
    }

    fun diagnose(request: TestRecordedRequest): String {
        val results = matchers.map { matcher ->
            val matched = matcher.matches(request)
            val prefix = if (matched) "  + PASS" else "  - FAIL"
            "$prefix: $matcher"
        }
        return results.joinToString("\n")
    }

    override fun toString(): String {
        return "composite(${matchers.joinToString { it.toString() }})"
    }
}

object RequestMatchers {
    fun stripeApiKey(
        publishableKey: String = TestApiKeys.PUBLISHABLE,
        ephemeralKey: String = TestApiKeys.EPHEMERAL,
    ): RequestMatcher {
        return ToStringRequestMatcher("stripeApiKey") { request ->
            when (request.headers[ORIGINAL_HOST_HEADER]) {
                API_HOST -> {
                    val authorization = request.headers[AUTHORIZATION_HEADER]
                    if (request.requiresEphemeralKey()) {
                        authorization == "Bearer $ephemeralKey"
                    } else {
                        authorization == "Bearer $publishableKey" ||
                            authorization == "Bearer ${TestApiKeys.LIVE_PUBLISHABLE}"
                    }
                }
                ANALYTICS_HOST -> {
                    request.headers[AUTHORIZATION_HEADER] == null &&
                        request.queryParams[PUBLISHABLE_KEY_QUERY] == publishableKey
                }
                else -> true
            }
        }
    }

    fun host(host: String): RequestMatcher {
        return header("original-host", host)
    }

    fun header(key: String, value: String): RequestMatcher {
        return ToStringRequestMatcher("header($key, $value)") { request ->
            request.headers[key] == value
        }
    }

    fun doesNotContainHeaderWithValue(key: String, value: String): RequestMatcher {
        return ToStringRequestMatcher(
            friendlyName = "doesNotContainHeaderWithValue($key, $value)"
        ) { request ->
            for (v in request.headers.values(key)) {
                if (v == value) {
                    return@ToStringRequestMatcher false // Fail the check, since it does contain the header.
                }
            }
            true // Pass the check, since we didn't find the header.
        }
    }

    fun not(requestMatcher: RequestMatcher): RequestMatcher {
        return ToStringRequestMatcher("not($requestMatcher)") { request ->
            !requestMatcher.matches(request)
        }
    }

    fun doesNotContainHeader(key: String): RequestMatcher {
        return ToStringRequestMatcher("doesNotContainHeader($key)") { request ->
            !request.headers.names().contains(key)
        }
    }

    fun path(path: String): RequestMatcher {
        return ToStringRequestMatcher("path($path)") { request ->
            var requestPath = request.path
            val queryIndex = requestPath.indexOf("?")
            if (queryIndex > -1) {
                // Remove the query params.
                requestPath = requestPath.substring(0, queryIndex)
            }
            requestPath.endsWith(path)
        }
    }

    fun query(query: String): RequestMatcher {
        return ToStringRequestMatcher("query($query)") { request ->
            val requestPath = request.path
            val queryIndex = requestPath.indexOf("?")
            if (queryIndex > -1) {
                requestPath.substring(queryIndex + 1) == query
            } else {
                false
            }
        }
    }

    fun query(name: String, value: String?): RequestMatcher {
        return ToStringRequestMatcher("query($name, $value)") { request ->
            request.queryParams[name] == value || request.queryParams[urlDecode(name)] == urlDecode(value ?: "")
        }
    }

    fun analyticsPayloadField(key: String, value: String): RequestMatcher {
        return query(key, value)
    }

    fun method(method: String): RequestMatcher {
        return ToStringRequestMatcher("method($method)") { request ->
            request.method == method
        }
    }

    fun body(body: String): RequestMatcher {
        return ToStringRequestMatcher("body($body)") { request ->
            val actual = request.bodyText
            actual == body
        }
    }

    fun hasBodyPart(name: String): RequestMatcher {
        return ToStringRequestMatcher("hasBodyPart($name)") { request ->
            request.bodyParams.containsKey(name) || request.bodyParams.containsKey(urlDecode(name))
        }
    }

    fun doesNotContainBodyPartsWithPrefix(prefix: String): RequestMatcher {
        return ToStringRequestMatcher("doesNotContainBodyPartsWithPrefix($prefix)") { request ->
            request.bodyParams.keys.none { it.startsWith(prefix) }
        }
    }

    fun bodyPart(name: String, value: String): RequestMatcher {
        return ToStringRequestMatcher("bodyPart($name, $value)") { request ->
            request.bodyParams[name] == value ||
                request.bodyParams[urlDecode(name)] == urlDecode(value)
        }
    }

    fun bodyPart(name: String, regex: Regex): RequestMatcher {
        return ToStringRequestMatcher("bodyPart($name, $regex)") { request ->
            request.bodyParams.getOrElse(name) {
                request.bodyParams.getOrElse(urlDecode(name)) { "" }
            }.matches(regex)
        }
    }

    fun hasQueryParam(param: String): RequestMatcher {
        return ToStringRequestMatcher("queryParam($param)") { request ->
            request.queryParameterValues(param).size == 1
        }
    }

    fun composite(vararg matchers: RequestMatcher): RequestMatcher {
        return CompositeRequestMatcher(matchers.toList())
    }

    private fun TestRecordedRequest.requiresEphemeralKey(): Boolean {
        val pathWithoutQuery = path.substringBefore('?')
        return when {
            method == "GET" && pathWithoutQuery == PAYMENT_METHODS_PATH -> true
            pathWithoutQuery.startsWith("$PAYMENT_METHODS_PATH/") -> true
            pathWithoutQuery.startsWith(ELEMENTS_PAYMENT_METHODS_PATH) -> true
            pathWithoutQuery.startsWith(CUSTOMERS_PATH) -> true
            pathWithoutQuery.startsWith(ELEMENTS_CUSTOMERS_PATH) -> true
            pathWithoutQuery == CONFIRMATION_TOKENS_PATH && bodyParams.containsKey(PAYMENT_METHOD_PARAM) -> true
            else -> false
        }
    }

    private const val ORIGINAL_HOST_HEADER = "original-host"
    private const val AUTHORIZATION_HEADER = "Authorization"
    private const val API_HOST = "api.stripe.com"
    private const val ANALYTICS_HOST = "q.stripe.com"
    private const val PUBLISHABLE_KEY_QUERY = "publishable_key"
    private const val PAYMENT_METHODS_PATH = "/v1/payment_methods"
    private const val ELEMENTS_PAYMENT_METHODS_PATH = "/v1/elements/payment_methods/"
    private const val CUSTOMERS_PATH = "/v1/customers/"
    private const val ELEMENTS_CUSTOMERS_PATH = "/v1/elements/customers/"
    private const val CONFIRMATION_TOKENS_PATH = "/v1/confirmation_tokens"
    private const val PAYMENT_METHOD_PARAM = "payment_method"
}
