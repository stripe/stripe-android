package com.stripe.android.identity.networked

import com.stripe.android.core.networking.ApiRequest
import com.stripe.android.core.networking.DefaultStripeNetworkClient
import com.stripe.android.core.networking.StripeNetworkClient
import com.stripe.android.core.utils.urlEncode
import com.stripe.android.identity.networking.BASE_URL
import com.stripe.android.identity.networking.IDENTITY_STRIPE_API_VERSION_WITH_BETA_HEADER
import com.stripe.android.identity.networking.IDENTITY_VERIFICATION_PAGES
import com.stripe.android.identity.networking.IdentityRepository
import com.stripe.android.identity.networking.models.NetworkedIdentityRoute
import com.stripe.android.identity.networking.models.VerificationPage
import com.stripe.android.identity.networking.models.VerificationPageData
import kotlinx.coroutines.CancellationException
import kotlin.coroutines.CoroutineContext

/** Preview-only draft actions. */
internal class DefaultNetworkedIdentityActions(
    override val verificationSessionId: String,
    private val ephemeralKey: String,
    private val retrieveVerificationPage: suspend () -> VerificationPage,
    private val stripeNetworkClient: StripeNetworkClient,
) : NetworkedIdentityActions {
    private val apiRequestFactory = ApiRequest.Factory(apiVersion = IDENTITY_STRIPE_API_VERSION_WITH_BETA_HEADER)

    override suspend fun attachDocument(associationToken: String): Result<VerificationPageData> = post(
        action = "attach_document",
        params = mapOf("identity_document_association_token" to associationToken),
    )

    override suspend fun prepareDocumentSave(associationToken: String): Result<VerificationPageData> = post(
        action = "prepare_document_save",
        params = mapOf("identity_document_save_association_token" to associationToken),
    )

    override suspend fun skip(): Result<VerificationPageData> = post(action = "skip", params = emptyMap())

    override suspend fun verificationIfCommitted(route: NetworkedIdentityRoute): Result<VerificationPageData?> = try {
        Result.success(retrieveVerificationPage().verificationIfCommitted(route))
    } catch (exception: CancellationException) {
        throw exception
    } catch (@Suppress("TooGenericExceptionCaught") exception: Exception) {
        Result.failure(exception)
    }

    private suspend fun post(action: String, params: Map<String, String>): Result<VerificationPageData> {
        // #TODO - Networked Identity: Confirm these draft v8 paths and VerificationPageData response before rollout.
        val request = apiRequestFactory.createPost(
            url = "$BASE_URL/$IDENTITY_VERIFICATION_PAGES/${urlEncode(verificationSessionId)}" +
                "/networked_identity/$action",
            options = ApiRequest.Options(apiKey = ephemeralKey, stripeAccount = null, idempotencyKey = null),
            params = params,
            shouldCache = false,
        )
        return stripeNetworkClient.postNetworkedIdentity(request, VerificationPageData.serializer())
    }

    internal companion object {
        fun create(
            verificationSessionId: String,
            ephemeralKey: String,
            identityRepository: IdentityRepository,
            workContext: CoroutineContext,
        ): DefaultNetworkedIdentityActions =
            DefaultNetworkedIdentityActions(
                verificationSessionId = verificationSessionId,
                ephemeralKey = ephemeralKey,
                retrieveVerificationPage = {
                    identityRepository.retrieveVerificationPage(verificationSessionId, ephemeralKey)
                },
                stripeNetworkClient = DefaultStripeNetworkClient(workContext = workContext),
            )
    }
}
