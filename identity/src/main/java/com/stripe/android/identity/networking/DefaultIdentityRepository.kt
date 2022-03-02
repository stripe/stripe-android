package com.stripe.android.identity.networking

import androidx.annotation.VisibleForTesting
import com.stripe.android.core.exception.APIConnectionException
import com.stripe.android.core.exception.APIException
import com.stripe.android.core.model.parsers.StripeErrorJsonParser
import com.stripe.android.core.networking.StripeNetworkClient
import com.stripe.android.core.networking.StripeRequest
import com.stripe.android.core.networking.responseJson
import com.stripe.android.identity.networking.models.CollectedDataParam
import com.stripe.android.identity.networking.models.CollectedDataParam.Companion.createCollectedDataParam
import com.stripe.android.identity.networking.models.VerificationPage
import com.stripe.android.identity.networking.models.VerificationPageData
import kotlinx.serialization.KSerializer
import kotlinx.serialization.json.Json

internal class DefaultIdentityRepository(
    private val stripeNetworkClient: StripeNetworkClient
) : IdentityRepository {

    @VisibleForTesting
    internal val json: Json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        encodeDefaults = true
    }

    private val stripeErrorJsonParser = StripeErrorJsonParser()

    override suspend fun retrieveVerificationPage(
        id: String,
        ephemeralKey: String
    ): VerificationPage = executeRequest(
        RetrieveVerificationPageRequest(id, ephemeralKey),
        VerificationPage.serializer()
    )

    override suspend fun postVerificationPageData(
        id: String,
        ephemeralKey: String,
        collectedDataParam: CollectedDataParam
    ): VerificationPageData = executeRequest(
        PostVerificationPageDataRequest(
            id,
            ephemeralKey,
            collectedDataParam.createCollectedDataParam(json)
        ),
        VerificationPageData.serializer()
    )

    override suspend fun postVerificationPageSubmit(
        id: String,
        ephemeralKey: String
    ): VerificationPageData = executeRequest(
        PostVerificationPageSubmitRequest(id, ephemeralKey),
        VerificationPageData.serializer()
    )

    private suspend fun <Response> executeRequest(
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
}
