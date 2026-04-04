package com.stripe.android.core.networking

import androidx.annotation.RestrictTo
import okio.Path

/**
 * HTTP client to execute different types of [StripeRequest] and return [StripeResponse].
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
interface StripeNetworkClient {
    /**
     * Execute an HTTP request represented by a [StripeRequest] and attempts to parse the HTTP
     * response's body as a [String] and returns it as [StripeResponse].
     *
     * @param request: the request to execute
     *
     * @return response with its body parsed as a String
     *
     */
    @Throws(Exception::class)
    suspend fun executeRequest(
        request: StripeRequest
    ): StripeResponse<String>

    /**
     * Execute an HTTP request represented by a [StripeRequest] and attempts to parse the HTTP
     * response's body as a file [Path] and returns it as [StripeResponse].
     *
     * @param request: the request to execute
     * @param outputFile: the file to save to
     *
     * @return response with its body saved to outputFile
     *
     */
    @Throws(Exception::class)
    suspend fun executeRequestForFile(
        request: StripeRequest,
        outputFile: Path
    ): StripeResponse<Path>
}
