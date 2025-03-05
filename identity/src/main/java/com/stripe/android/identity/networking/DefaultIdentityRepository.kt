package com.stripe.android.identity.networking

import android.content.Context
import android.util.Log
import androidx.annotation.VisibleForTesting
import com.stripe.android.core.exception.APIConnectionException
import com.stripe.android.core.exception.APIException
import com.stripe.android.core.model.parsers.StripeErrorJsonParser
import com.stripe.android.core.networking.AnalyticsRequestV2
import com.stripe.android.core.networking.ApiRequest
import com.stripe.android.core.networking.StripeNetworkClient
import com.stripe.android.core.networking.StripeRequest
import com.stripe.android.core.networking.responseJson
import com.stripe.android.core.utils.urlEncode
import com.stripe.android.identity.networking.models.VerificationPage
import kotlinx.serialization.KSerializer
import kotlinx.serialization.json.Json
import javax.inject.Inject

internal class DefaultIdentityRepository @Inject constructor(
    private val stripeNetworkClient: StripeNetworkClient,
    private val context: Context
) : IdentityRepository {

    @VisibleForTesting
    internal val json: Json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        encodeDefaults = true
    }

    private val stripeErrorJsonParser = StripeErrorJsonParser()

    private val apiRequestFactory = ApiRequest.Factory(
        apiVersion = IDENTITY_STRIPE_API_VERSION_WITH_BETA_HEADER
    )

    override suspend fun retrieveVerificationPage(
        id: String,
        ephemeralKey: String
    ): VerificationPage = executeRequestWithKSerializer(
        apiRequestFactory.createGet(
            url = "$BASE_URL/$IDENTITY_VERIFICATION_PAGES/${urlEncode(id)}",
            options = ApiRequest.Options(
                apiKey = ephemeralKey
            ),
            params = mapOf(
                APP_IDENTIFIER to context.packageName
            )
        ),
        VerificationPage.serializer()
    )

    override suspend fun sendAnalyticsRequest(analyticsRequestV2: AnalyticsRequestV2) {
        runCatching {
            stripeNetworkClient.executeRequest(analyticsRequestV2)
        }.onFailure {
            Log.e(TAG, "Exception while making analytics request")
        }
    }

    private suspend fun <Response> executeRequestWithKSerializer(
        request: StripeRequest,
        responseSerializer: KSerializer<Response>
    ): Response = runCatching {
        stripeNetworkClient.executeRequest(
            request
        )
    }.fold(
        onSuccess = { response ->
            if (response.isError) {
                // TODO(ccen) Parse the response code and throw different exceptions
                throw APIException(
                    stripeError = stripeErrorJsonParser.parse(response.responseJson()),
                    requestId = response.requestId?.value,
                    statusCode = response.code
                )
            } else {
                json.decodeFromString(
                    responseSerializer,
                    requireNotNull(response.body)
                )
            }
        },
        onFailure = {
            throw APIConnectionException(
                "Failed to execute $request",
                cause = it
            )
        }
    )

    internal companion object {
        val TAG: String = DefaultIdentityRepository::class.java.simpleName
    }
}
