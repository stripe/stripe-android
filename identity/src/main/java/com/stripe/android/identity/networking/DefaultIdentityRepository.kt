package com.stripe.android.identity.networking

import android.content.Context
import androidx.annotation.VisibleForTesting
import com.stripe.android.core.exception.APIConnectionException
import com.stripe.android.core.exception.APIException
import com.stripe.android.core.model.InternalStripeFile
import com.stripe.android.core.model.InternalStripeFileParams
import com.stripe.android.core.model.InternalStripeFilePurpose
import com.stripe.android.core.model.StripeModel
import com.stripe.android.core.model.parsers.ModelJsonParser
import com.stripe.android.core.model.parsers.StripeErrorJsonParser
import com.stripe.android.core.model.parsers.StripeFileJsonParser
import com.stripe.android.core.networking.ApiRequest
import com.stripe.android.core.networking.DefaultStripeNetworkClient
import com.stripe.android.core.networking.QueryStringFactory
import com.stripe.android.core.networking.StripeNetworkClient
import com.stripe.android.core.networking.StripeRequest
import com.stripe.android.core.networking.responseJson
import com.stripe.android.identity.networking.models.ClearDataParam
import com.stripe.android.identity.networking.models.ClearDataParam.Companion.createCollectedDataParamEntry
import com.stripe.android.identity.networking.models.CollectedDataParam
import com.stripe.android.identity.networking.models.CollectedDataParam.Companion.createCollectedDataParamEntry
import com.stripe.android.identity.networking.models.VerificationPage
import com.stripe.android.identity.networking.models.VerificationPageData
import com.stripe.android.identity.utils.createTFLiteFile
import kotlinx.serialization.KSerializer
import kotlinx.serialization.json.Json
import java.io.File

internal class DefaultIdentityRepository(
    private val context: Context,
    private val stripeNetworkClient: StripeNetworkClient = DefaultStripeNetworkClient()
) : IdentityRepository {

    @VisibleForTesting
    internal val json: Json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        encodeDefaults = true
    }

    private val stripeErrorJsonParser = StripeErrorJsonParser()
    private val stripeFileJsonParser = StripeFileJsonParser()

    override suspend fun retrieveVerificationPage(
        id: String,
        ephemeralKey: String
    ): VerificationPage = executeRequestWithKSerializer(
        RetrieveVerificationPageRequest(id, ephemeralKey),
        VerificationPage.serializer()
    )

    override suspend fun postVerificationPageData(
        id: String,
        ephemeralKey: String,
        collectedDataParam: CollectedDataParam,
        clearDataParam: ClearDataParam
    ): VerificationPageData = executeRequestWithKSerializer(
        PostVerificationPageDataRequest(
            id,
            ephemeralKey,
            QueryStringFactory.createFromParamsWithEmptyValues(
                mapOf(
                    collectedDataParam.createCollectedDataParamEntry(json),
                    clearDataParam.createCollectedDataParamEntry(json)
                )
            )
        ),
        VerificationPageData.serializer()
    )

    override suspend fun postVerificationPageSubmit(
        id: String,
        ephemeralKey: String
    ): VerificationPageData = executeRequestWithKSerializer(
        PostVerificationPageSubmitRequest(id, ephemeralKey),
        VerificationPageData.serializer()
    )

    override suspend fun uploadImage(
        verificationId: String,
        ephemeralKey: String,
        imageFile: File,
        filePurpose: InternalStripeFilePurpose
    ): InternalStripeFile = executeRequestWithModelJsonParser(
        request = IdentityFileUploadRequest(
            fileParams = InternalStripeFileParams(
                file = imageFile,
                purpose = filePurpose
            ),
            options = ApiRequest.Options(
                apiKey = ephemeralKey
            ),
            verificationId = verificationId
        ),
        responseJsonParser = stripeFileJsonParser
    )

    override suspend fun downloadModel(modelUrl: String) = runCatching {
        stripeNetworkClient.executeRequestForFile(
            IdentityModelDownloadRequest(modelUrl),
            createTFLiteFile(context)
        )
    }.fold(
        onSuccess = { response ->
            if (response.isError) {
                throw APIException(
                    requestId = response.requestId?.value,
                    statusCode = response.code,
                    message = "Downloading from $modelUrl returns error response"
                )
            } else {
                response.body ?: run {
                    throw APIException(
                        message = "Downloading from $modelUrl returns a null body"
                    )
                }
            }
        },
        onFailure = {
            throw APIConnectionException(
                "Fail to download file at $modelUrl",
                cause = it
            )
        }
    )

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

    private suspend fun <Response : StripeModel> executeRequestWithModelJsonParser(
        request: StripeRequest,
        responseJsonParser: ModelJsonParser<Response>
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
                responseJsonParser.parse(response.responseJson()) ?: run {
                    throw APIException(
                        message = "$responseJsonParser returns null for ${response.responseJson()}"
                    )
                }
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
