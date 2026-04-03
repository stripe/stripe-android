package com.stripe.android.core.networking

import androidx.annotation.RestrictTo
import com.stripe.android.core.exception.APIConnectionException
import java.io.File

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
     * @throws APIConnectionException if there connection fails
     */
    @Throws(APIConnectionException::class)
    suspend fun executeRequest(
        request: StripeRequest
    ): StripeResponse<String>

    /**
     * Execute an HTTP request represented by a [StripeRequest] and attempts to parse the HTTP
     * response's body as a [File] and returns it as [StripeResponse].
     *
     * @param request: the request to execute
     * @param outputFile: the file to save to
     *
     * @return response with its body saved to outputFile
     *
     * @throws APIConnectionException if there connection fails
     */
    @Throws(APIConnectionException::class)
    suspend fun executeRequestForFile(
        request: StripeRequest,
        outputFile: File
    ): StripeResponse<File>
}
