package com.stripe.android

import com.stripe.android.exception.InvalidRequestException
import java.io.UnsupportedEncodingException
import org.mockito.ArgumentMatcher

internal class ApiRequestMatcher @JvmOverloads constructor(
    private val method: StripeRequest.Method,
    private val url: String,
    private val options: ApiRequest.Options,
    private val params: Map<String, *>? = null
) : ArgumentMatcher<ApiRequest> {

    override fun matches(request: ApiRequest): Boolean {
        val url: String
        try {
            url = request.url
        } catch (e: UnsupportedEncodingException) {
            return false
        } catch (e: InvalidRequestException) {
            return false
        }

        return method == request.method &&
            this.url == url &&
            options == request.options &&
            (params == null || params == request.params)
    }
}
