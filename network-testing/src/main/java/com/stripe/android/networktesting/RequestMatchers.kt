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
}
