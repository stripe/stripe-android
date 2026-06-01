package com.stripe.link.core.data.network

import com.stripe.link.core.analytics.Analytics
import io.ktor.client.statement.HttpResponse

/**
 * HTTP client for the Link API. All methods are suspend functions that return [Result].
 */
open class LinkApiClient(
    val apiRequestFactory: ApiRequestFactory,
) {

    /**
     * Fire-and-forget: record an email CTA click server-side.
     * Swallows all errors — server returns 200 regardless of outcome.
     */
    open suspend fun recordEmailClick(ref: String, eid: String, link: String) {
        post<HttpResponse>(retryOnAuthFailure = false) {
            apiRequestFactory.emailClick(ref = ref, eid = eid, link = link)
        }.onFailure { error ->
            Analytics.logBreadcrumb("Email click recording failed (ignored): ${error.message}")
        }
    }

    /**
     * Executes a POST request as defined by [requestBuilder] and returns a [Result].
     *
     * @param retryOnAuthFailure when true the client may refresh auth credentials and retry once.
     * @param requestBuilder lambda that produces the [ApiRequest] to execute.
     */
    protected open suspend fun <T> post(
        retryOnAuthFailure: Boolean = true,
        requestBuilder: () -> ApiRequest,
    ): Result<T> {
        return runCatching {
            @Suppress("UNCHECKED_CAST")
            executePost(requestBuilder(), retryOnAuthFailure) as T
        }
    }

    /** Performs the actual HTTP POST. Overridable for testing. */
    protected open suspend fun executePost(request: ApiRequest, retryOnAuthFailure: Boolean): Any {
        // Real Ktor HTTP execution goes here
        throw NotImplementedError("executePost must be implemented by a concrete subclass")
    }
}
