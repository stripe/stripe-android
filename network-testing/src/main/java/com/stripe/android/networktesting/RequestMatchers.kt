package com.stripe.android.networktesting

typealias RequestMatcher = (request: TestRecordedRequest) -> Boolean

object RequestMatchers {
    fun header(key: String, value: String): RequestMatcher {
        return { request ->
            request.headers[key] == value
        }
    }

    fun doesNotContainHeaderWithValue(key: String, value: String): RequestMatcher {
        return matcher@{ request ->
            for (v in request.headers.values(key)) {
                if (v == value) {
                    return@matcher false // Fail the check, since it does contain the header.
                }
            }
            true // Pass the check, since we didn't find the header.
        }
    }

    fun not(requestMatcher: RequestMatcher): RequestMatcher {
        return { request ->
            !requestMatcher.invoke(request)
        }
    }

    fun doesNotContainHeader(key: String): RequestMatcher {
        return { request ->
            !request.headers.names().contains(key)
        }
    }

    fun path(path: String): RequestMatcher {
        return { request ->
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
        return { request ->
            val requestPath = request.path
            val queryIndex = requestPath.indexOf("?")
            if (queryIndex > -1) {
                requestPath.substring(queryIndex + 1) == query
            } else {
                false
            }
        }
    }

    fun query(name: String, value: String): RequestMatcher = { request ->
        request.path.substringAfter("?")
            .split("&")
            .associate { Pair(it.substringBefore("="), it.substringAfter("=")) }[name] == value
    }

    fun method(method: String): RequestMatcher {
        return { request -> request.method == method }
    }

    fun body(body: String): RequestMatcher {
        return { request ->
            val actual = request.bodyText
            actual == body
        }
    }

    fun bodyPart(name: String, value: String): RequestMatcher = { request ->
        request.bodyText.substringAfter("?")
            .split("&")
            .associate { Pair(it.substringBefore("="), it.substringAfter("=")) }[name] == value
    }

    fun composite(vararg matchers: RequestMatcher): RequestMatcher {
        return { request ->
            matchers.all { it(request) }
        }
    }
}
