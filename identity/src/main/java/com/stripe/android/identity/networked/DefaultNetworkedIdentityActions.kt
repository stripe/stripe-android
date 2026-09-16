package com.stripe.android.identity.networked

import com.stripe.android.core.Logger
import com.stripe.android.core.StripeError
import com.stripe.android.core.exception.APIConnectionException
import com.stripe.android.core.exception.APIException
import com.stripe.android.core.model.parsers.StripeErrorJsonParser
import com.stripe.android.core.networking.ApiRequest
import com.stripe.android.core.networking.DefaultStripeNetworkClient
import com.stripe.android.core.networking.StripeNetworkClient
import com.stripe.android.core.networking.StripeRequest
import com.stripe.android.core.networking.StripeResponse
import com.stripe.android.core.utils.urlEncode
import com.stripe.android.identity.networking.BASE_URL
import com.stripe.android.identity.networking.IDENTITY_STRIPE_API_VERSION_WITH_BETA_HEADER
import com.stripe.android.identity.networking.IDENTITY_VERIFICATION_PAGES
import com.stripe.android.identity.networking.models.VerificationPageData
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.serialization.json.Json
import org.json.JSONObject
import java.io.OutputStream

/** Preview-only draft actions. Single-use tokens must never be replayed after an ambiguous response. */
internal class DefaultNetworkedIdentityActions(
    override val verificationSessionId: String,
    private val ephemeralKey: String,
    private val stripeNetworkClient: StripeNetworkClient,
) : NetworkedIdentityActions {
    private val apiRequestFactory = ApiRequest.Factory(apiVersion = IDENTITY_STRIPE_API_VERSION_WITH_BETA_HEADER)
    private val stripeErrorJsonParser = StripeErrorJsonParser()
    private val json = Json { ignoreUnknownKeys = true }

    override suspend fun attachDocument(associationToken: String): Result<VerificationPageData> = post(
        action = "attach_document",
        params = mapOf("identity_document_association_token" to associationToken),
    )

    override suspend fun prepareDocumentSave(associationToken: String): Result<VerificationPageData> = post(
        action = "prepare_document_save",
        params = mapOf("identity_document_save_association_token" to associationToken),
    )

    override suspend fun skip(): Result<VerificationPageData> = post(action = "skip", params = emptyMap())

    private suspend fun post(action: String, params: Map<String, String>): Result<VerificationPageData> {
        // #TODO - Networked Identity: Confirm these draft v8 paths and VerificationPageData response before rollout.
        val request = NetworkedIdentityActionRequest(
            apiRequestFactory.createPost(
                url = "$BASE_URL/$IDENTITY_VERIFICATION_PAGES/${urlEncode(verificationSessionId)}" +
                    "/networked_identity/$action",
                options = ApiRequest.Options(apiKey = ephemeralKey, stripeAccount = null, idempotencyKey = null),
                params = params,
                shouldCache = false,
            )
        )
        val response = try {
            stripeNetworkClient.executeRequest(request)
        } catch (exception: CancellationException) {
            throw exception
        } catch (_: Exception) {
            return Result.failure(APIConnectionException(message = "Networked Identity action failed."))
        }
        return decodeResponse(response)
    }

    private fun decodeResponse(response: StripeResponse<String>): Result<VerificationPageData> = try {
        if (response.isError) {
            val error = stripeErrorJsonParser.parse(JSONObject(requireNotNull(response.body)))
            Result.failure(
                APIException(
                    stripeError = StripeError(type = error.type, code = error.code),
                    requestId = response.requestId?.value,
                    statusCode = response.code,
                    message = "Networked Identity action failed.",
                )
            )
        } else {
            Result.success(json.decodeFromString(VerificationPageData.serializer(), requireNotNull(response.body)))
        }
    } catch (_: Exception) {
        Result.failure(
            APIException(
                requestId = response.requestId?.value,
                statusCode = response.code,
                message = "Networked Identity action response was invalid.",
            )
        )
    }

    internal companion object {
        fun create(verificationSessionId: String, ephemeralKey: String): DefaultNetworkedIdentityActions =
            DefaultNetworkedIdentityActions(
                verificationSessionId = verificationSessionId,
                ephemeralKey = ephemeralKey,
                stripeNetworkClient = DefaultStripeNetworkClient(
                    workContext = Dispatchers.IO,
                    maxRetries = 0,
                    logger = Logger.noop(),
                ),
            )
    }
}

private class NetworkedIdentityActionRequest(private val request: ApiRequest) : StripeRequest() {
    override val method: Method = request.method
    override val mimeType: MimeType = request.mimeType
    override val retryResponseCodes: Iterable<Int> = emptyList()
    override val url: String = request.url
    override val headers: Map<String, String> = request.headers
    override var postHeaders: Map<String, String>? = request.postHeaders

    override fun writePostBody(outputStream: OutputStream) = request.writePostBody(outputStream)

    override fun toString(): String = "NetworkedIdentityActionRequest(POST, [redacted])"
}
