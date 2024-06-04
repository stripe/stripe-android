package com.stripe.android.stripecardscan.framework.api

import android.util.Log
import com.stripe.android.core.networking.DefaultStripeNetworkClient
import com.stripe.android.core.networking.StripeNetworkClient
import com.stripe.android.core.networking.StripeRequest
import com.stripe.android.core.utils.decodeFromJson
import com.stripe.android.core.utils.encodeToXWWWFormUrl
import com.stripe.android.stripecardscan.framework.api.dto.CardScanFileDownloadRequest
import com.stripe.android.stripecardscan.framework.api.dto.CardScanRequest
import kotlinx.serialization.KSerializer
import java.io.File
import java.net.HttpURLConnection.HTTP_MULT_CHOICE
import java.net.HttpURLConnection.HTTP_OK
import java.net.URL

internal class StripeNetwork internal constructor(
    private val baseUrl: String,
    private val retryStatusCodes: Iterable<Int>,
    private val stripeNetworkClient: StripeNetworkClient
) : Network {

    internal constructor(
        baseUrl: String,
        retryTotalAttempts: Int,
        retryStatusCodes: Iterable<Int>
    ) : this(
        baseUrl,
        retryStatusCodes,
        DefaultStripeNetworkClient(
            maxRetries = retryTotalAttempts
        )
    )

    override suspend fun <Request, Response, Error> postForResult(
        stripePublishableKey: String,
        path: String,
        data: Request,
        requestSerializer: KSerializer<Request>,
        responseSerializer: KSerializer<Response>,
        errorSerializer: KSerializer<Error>
    ): NetworkResult<out Response, out Error> =
        translateNetworkResult(
            networkResult = postDataWithRetries(
                stripePublishableKey = stripePublishableKey,
                retryResponseCodes = retryStatusCodes,
                path = path,
                encodedData = encodeToXWWWFormUrl(requestSerializer, data)
            ),
            responseSerializer = responseSerializer,
            errorSerializer = errorSerializer
        )

    /**
     * Send a post request to a Stripe endpoint and ignore the response.
     */
    override suspend fun <Request> postData(
        stripePublishableKey: String,
        path: String,
        data: Request,
        requestSerializer: KSerializer<Request>
    ) {
        postDataWithRetries(
            stripePublishableKey = stripePublishableKey,
            retryResponseCodes = retryStatusCodes,
            path = path,
            encodedData = encodeToXWWWFormUrl(requestSerializer, data)
        )
    }

    /**
     * Send a get request to a Stripe endpoint and parse the response.
     */
    override suspend fun <Response, Error> getForResult(
        stripePublishableKey: String,
        path: String,
        responseSerializer: KSerializer<Response>,
        errorSerializer: KSerializer<Error>
    ): NetworkResult<out Response, out Error> =
        translateNetworkResult(
            getWithRetries(stripePublishableKey, path, retryStatusCodes),
            responseSerializer,
            errorSerializer
        )

    override suspend fun downloadFileWithRetries(
        url: URL,
        outputFile: File
    ): Int {
        var responseCode = RESPONSE_CODE_UNSET
        return try {
            responseCode =
                stripeNetworkClient.executeRequestForFile(
                    CardScanFileDownloadRequest(url.path),
                    outputFile
                ).code
            responseCode
        } catch (t: Throwable) {
            Log.w(LOG_TAG, "Failed network request to download file $url", t)
            responseCode
        }
    }

    /**
     * Send a post request to a Stripe endpoint with retries.
     */
    private suspend fun postDataWithRetries(
        stripePublishableKey: String,
        retryResponseCodes: Iterable<Int>,
        path: String,
        encodedData: String
    ): NetworkResult<out String, out String> = executeAndConvertToNetworkResult(
        CardScanRequest.createPost(
            stripePublishableKey = stripePublishableKey,
            baseUrl = baseUrl,
            retryResponseCodes = retryResponseCodes,
            path = path,
            encodedPostData = encodedData
        )
    )

    /**
     * Send a get request to a Stripe endpoint with retries.
     */
    private suspend fun getWithRetries(
        stripePublishableKey: String,
        path: String,
        retryResponseCodes: Iterable<Int>
    ): NetworkResult<out String, out String> = executeAndConvertToNetworkResult(
        CardScanRequest.createGet(
            stripePublishableKey = stripePublishableKey,
            baseUrl = baseUrl,
            path = path,
            retryResponseCodes = retryResponseCodes
        )
    )

    private suspend fun executeAndConvertToNetworkResult(request: StripeRequest):
        NetworkResult<out String, out String> {
        var responseCode = RESPONSE_CODE_UNSET
        return try {
            stripeNetworkClient.executeRequest(request).let { stripeResponse ->
                responseCode = stripeResponse.code
                when (responseCode) {
                    in HTTP_OK until HTTP_MULT_CHOICE -> NetworkResult.Success(
                        responseCode,
                        requireNotNull(stripeResponse.body)
                    )
                    else -> NetworkResult.Error(
                        responseCode,
                        requireNotNull(stripeResponse.body)
                    )
                }
            }
        } catch (t: Throwable) {
            Log.w(LOG_TAG, "Failed network request to endpoint ${request.url}", t)
            NetworkResult.Exception(responseCode, t)
        }
    }

    /**
     * Translate a string network result to a response or error.
     */
    private fun <Response, Error> translateNetworkResult(
        networkResult: NetworkResult<out String, out String>,
        responseSerializer: KSerializer<Response>,
        errorSerializer: KSerializer<Error>
    ): NetworkResult<out Response, out Error> = when (networkResult) {
        is NetworkResult.Success ->
            try {
                NetworkResult.Success(
                    responseCode = networkResult.responseCode,
                    body = decodeFromJson(responseSerializer, networkResult.body)
                )
            } catch (t: Throwable) {
                try {
                    NetworkResult.Error(
                        responseCode = networkResult.responseCode,
                        error = decodeFromJson(errorSerializer, networkResult.body)
                    )
                } catch (et: Throwable) {
                    NetworkResult.Exception(networkResult.responseCode, t)
                }
            }
        is NetworkResult.Error ->
            try {
                NetworkResult.Error(
                    responseCode = networkResult.responseCode,
                    error = decodeFromJson(errorSerializer, networkResult.error)
                )
            } catch (t: Throwable) {
                NetworkResult.Exception(networkResult.responseCode, t)
            }
        is NetworkResult.Exception ->
            NetworkResult.Exception(
                responseCode = networkResult.responseCode,
                exception = networkResult.exception
            )
    }

    internal companion object {
        const val RESPONSE_CODE_UNSET = -1

        private val LOG_TAG = StripeNetwork::class.java.simpleName
    }
}
