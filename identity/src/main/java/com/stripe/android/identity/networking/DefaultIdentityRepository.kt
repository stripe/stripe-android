package com.stripe.android.identity.networking

import android.content.Context
import android.util.Log
import androidx.annotation.VisibleForTesting
import com.stripe.android.core.exception.APIConnectionException
import com.stripe.android.core.exception.APIException
import com.stripe.android.core.model.StripeModel
import com.stripe.android.core.model.parsers.ModelJsonParser
import com.stripe.android.core.model.parsers.StripeErrorJsonParser
import com.stripe.android.core.model.parsers.StripeFileJsonParser
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
import kotlin.time.TimeSource

internal class DefaultIdentityRepository @Inject constructor(
    private val stripeNetworkClient: StripeNetworkClient,
//    private val identityIO: IdentityIO,
    private val context: Context
) : IdentityRepository {

    @VisibleForTesting
    internal val json: Json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        encodeDefaults = true
    }

    private val stripeErrorJsonParser = StripeErrorJsonParser()
    private val stripeFileJsonParser = StripeFileJsonParser()

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

//    override suspend fun postVerificationPageData(
//        id: String,
//        ephemeralKey: String,
//        collectedDataParam: CollectedDataParam,
//        clearDataParam: ClearDataParam
//    ): VerificationPageData = executeRequestWithKSerializer(
//        apiRequestFactory.createPost(
//            url = "$BASE_URL/$IDENTITY_VERIFICATION_PAGES/${urlEncode(id)}/$DATA",
//            options = ApiRequest.Options(
//                apiKey = ephemeralKey
//            ),
//            params = mapOf(
//                collectedDataParam.createCollectedDataParamEntry(json),
//                clearDataParam.createCollectedDataParamEntry(json)
//            )
//        ),
//        VerificationPageData.serializer()
//    )

//    override suspend fun postVerificationPageSubmit(
//        id: String,
//        ephemeralKey: String
//    ): VerificationPageData = executeRequestWithKSerializer(
//        apiRequestFactory.createPost(
//            url = "$BASE_URL/$IDENTITY_VERIFICATION_PAGES/${urlEncode(id)}/$SUBMIT",
//            options = ApiRequest.Options(
//                apiKey = ephemeralKey
//            )
//        ),
//        VerificationPageData.serializer()
//    )
//
//    override suspend fun verifyTestVerificationSession(
//        id: String,
//        ephemeralKey: String,
//        simulateDelay: Boolean
//    ) = executeRequestWithKSerializer(
//        apiRequestFactory.createPost(
//            url = "$BASE_URL/$IDENTITY_VERIFICATION_PAGES/${urlEncode(id)}/$TESTING/$VERIFY",
//            options = ApiRequest.Options(
//                apiKey = ephemeralKey
//            ),
//            params = mapOf(
//                SIMULATE_DELAY to simulateDelay
//            )
//        ),
//        VerificationPageData.serializer()
//    )
//
//    override suspend fun unverifyTestVerificationSession(
//        id: String,
//        ephemeralKey: String, // todo - need to add this to the request
//        simulateDelay: Boolean
//    ) = executeRequestWithKSerializer(
//        apiRequestFactory.createPost(
//            url = "$BASE_URL/$IDENTITY_VERIFICATION_PAGES/${urlEncode(id)}/$TESTING/$UNVERIFY",
//            options = ApiRequest.Options(
//                apiKey = ephemeralKey
//            ),
//            params = mapOf(
//                SIMULATE_DELAY to simulateDelay
//            )
//        ),
//        VerificationPageData.serializer()
//    )
//
//    override suspend fun generatePhoneOtp(
//        id: String,
//        ephemeralKey: String
//    ) = executeRequestWithKSerializer(
//        apiRequestFactory.createPost(
//            url = "$BASE_URL/$IDENTITY_VERIFICATION_PAGES/${urlEncode(id)}/$PHONE_OTP/$GENERATE",
//            options = ApiRequest.Options(
//                apiKey = ephemeralKey
//            )
//        ),
//        VerificationPageData.serializer()
//    )
//
//    override suspend fun cannotVerifyPhoneOtp(
//        id: String,
//        ephemeralKey: String
//    ) = executeRequestWithKSerializer(
//        apiRequestFactory.createPost(
//            url = "$BASE_URL/$IDENTITY_VERIFICATION_PAGES/${urlEncode(id)}/$PHONE_OTP/$CANNOT_VERIFY",
//            options = ApiRequest.Options(
//                apiKey = ephemeralKey
//            )
//        ),
//        VerificationPageData.serializer()
//    )
//
//    override suspend fun uploadImage(
//        verificationId: String,
//        ephemeralKey: String,
//        imageFile: File,
//        filePurpose: StripeFilePurpose,
//        onSuccessExecutionTimeBlock: (Long) -> Unit
//    ): StripeFile = executeRequestWithModelJsonParser(
//        request = IdentityFileUploadRequest(
//            fileParams = StripeFileParams(
//                file = imageFile,
//                purpose = filePurpose
//            ),
//            options = ApiRequest.Options(
//                apiKey = ephemeralKey
//            ),
//            verificationId = verificationId
//        ),
//        responseJsonParser = stripeFileJsonParser,
//        onSuccessExecutionTimeBlock = onSuccessExecutionTimeBlock
//    )

//    override suspend fun downloadModel(modelUrl: String) = runCatching {
//        stripeNetworkClient.executeRequestForFile(
//            IdentityFileDownloadRequest(modelUrl),
//            identityIO.createTFLiteFile(modelUrl)
//        )
//    }.fold(
//        onSuccess = { response ->
//            if (response.isError) {
//                throw APIException(
//                    requestId = response.requestId?.value,
//                    statusCode = response.code,
//                    message = "Downloading from $modelUrl returns error response"
//                )
//            } else {
//                response.body ?: run {
//                    throw APIException(
//                        message = "Downloading from $modelUrl returns a null body"
//                    )
//                }
//            }
//        },
//        onFailure = {
//            throw APIConnectionException(
//                "Fail to download file at $modelUrl",
//                cause = it
//            )
//        }
//    )
//
//    override suspend fun downloadFile(fileUrl: String) = runCatching {
//        stripeNetworkClient.executeRequestForFile(
//            IdentityFileDownloadRequest(fileUrl),
//            identityIO.createCacheFile()
//        )
//    }.fold(
//        onSuccess = { response ->
//            if (response.isError) {
//                throw APIException(
//                    requestId = response.requestId?.value,
//                    statusCode = response.code,
//                    message = "Downloading from $fileUrl returns error response"
//                )
//            } else {
//                response.body ?: run {
//                    throw APIException(
//                        message = "Downloading from $fileUrl returns a null body"
//                    )
//                }
//            }
//        },
//        onFailure = {
//            throw APIConnectionException(
//                "Fail to download file at $fileUrl",
//                cause = it
//            )
//        }
//    )

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

    private suspend fun <Response : StripeModel> executeRequestWithModelJsonParser(
        request: StripeRequest,
        responseJsonParser: ModelJsonParser<Response>,
        onSuccessExecutionTimeBlock: (Long) -> Unit = {}
    ): Response {
        val started = TimeSource.Monotonic.markNow()
        return runCatching {
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
                    responseJsonParser.parse(response.responseJson())?.let { response ->
                        onSuccessExecutionTimeBlock(started.elapsedNow().inWholeMilliseconds)
                        response
                    } ?: run {
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

    internal companion object {
        val TAG: String = DefaultIdentityRepository::class.java.simpleName
    }
}
