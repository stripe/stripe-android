package com.stripe.android.identity.networked

import com.stripe.android.core.exception.APIException
import com.stripe.android.core.networking.ApiRequest
import com.stripe.android.core.networking.DefaultStripeNetworkClient
import com.stripe.android.core.networking.StripeNetworkClient
import com.stripe.android.core.utils.urlEncode
import kotlinx.coroutines.delay
import kotlinx.serialization.DeserializationStrategy
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlin.coroutines.CoroutineContext

internal class DefaultNetworkedIdentityRepository(
    private val stripeNetworkClient: StripeNetworkClient,
) : NetworkedIdentityRepository {
    private val apiRequestFactory = ApiRequest.Factory()

    override suspend fun listDocuments(
        credentials: NetworkedIdentityCredentials,
    ): Result<List<NetworkedIdentityDocument>> {
        val result = listDocumentsOnce(credentials)
        if ((result.exceptionOrNull() as? APIException)?.statusCode !in SERVER_ERROR_CODES) return result
        delay(DOCUMENT_LIST_RETRY_DELAY_MS)
        return listDocumentsOnce(credentials)
    }

    private suspend fun listDocumentsOnce(credentials: NetworkedIdentityCredentials) =
        post("consumers/identity_documents/list", credentials, emptyMap(), DocumentList.serializer()).map { it.data }

    override suspend fun createAssociationToken(
        credentials: NetworkedIdentityCredentials,
        documentId: String,
        verificationSessionId: String,
    ): Result<NetworkedIdentityAssociationToken> = post(
        path = "consumers/identity_documents/${urlEncode(documentId)}/association_token",
        credentials = credentials,
        // The token can then only attach this document to this verification session.
        params = mapOf("verification_session" to verificationSessionId),
        deserializer = NetworkedIdentityAssociationToken.serializer(),
    )

    // #TODO - Networked Identity: Confirm the draft save-token endpoint before rollout.
    override suspend fun createSaveAssociationToken(
        credentials: NetworkedIdentityCredentials,
        verificationSessionId: String,
    ): Result<NetworkedIdentityAssociationToken> = post(
        path = "consumers/identity_documents/save_association_token",
        credentials = credentials,
        params = mapOf("verification_session" to verificationSessionId),
        deserializer = NetworkedIdentityAssociationToken.serializer(),
    )

    private suspend fun <T> post(
        path: String,
        credentials: NetworkedIdentityCredentials,
        params: Map<String, Any>,
        deserializer: DeserializationStrategy<T>,
    ): Result<T> = stripeNetworkClient.postNetworkedIdentity(
        apiRequestFactory.createPost(
            url = "${ApiRequest.API_HOST}/v1/$path",
            options = ApiRequest.Options(
                apiKey = credentials.publishableKey,
                stripeAccount = null,
                idempotencyKey = null,
            ),
            params = params + mapOf(
                "credentials" to mapOf("consumer_session_client_secret" to credentials.sessionClientSecret),
                // #TODO - Networked Identity [NI-Contract]: send "android_identity_product" once it's accepted.
                "request_surface" to "web_identity_product",
            ),
            shouldCache = false,
        ),
        deserializer,
    )

    @Serializable
    private class DocumentList(
        @SerialName("data")
        val data: List<NetworkedIdentityDocument>,
    )

    internal companion object {
        private const val DOCUMENT_LIST_RETRY_DELAY_MS = 250L
        private val SERVER_ERROR_CODES = 500..599

        fun create(workContext: CoroutineContext) = DefaultNetworkedIdentityRepository(
            stripeNetworkClient = DefaultStripeNetworkClient(workContext = workContext),
        )
    }
}
