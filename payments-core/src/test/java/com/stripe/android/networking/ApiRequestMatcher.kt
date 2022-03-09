package com.stripe.android.networking

import com.stripe.android.core.networking.ApiRequest
import com.stripe.android.core.networking.StripeRequest
import org.mockito.ArgumentMatcher

internal class ApiRequestMatcher @JvmOverloads constructor(
    private val method: StripeRequest.Method,
    private val url: String,
    private val options: ApiRequest.Options,
    private val params: Map<String, *>? = null
) : ArgumentMatcher<ApiRequest> {

    override fun matches(request: ApiRequest): Boolean {
        val url = runCatching {
            request.url
        }.getOrNull()

        return this.url == url &&
            method == request.method &&
            options == request.options &&
            (params == null || params == request.params)
    }
}
