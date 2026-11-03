package com.stripe.android.crypto.onramp

import app.cash.turbine.Turbine
import com.stripe.android.core.networking.ApiRequest
import com.stripe.android.core.networking.StripeNetworkClient
import com.stripe.android.core.networking.StripeRequest
import com.stripe.android.core.networking.StripeResponse
import com.stripe.android.model.Token
import com.stripe.android.model.TokenParams
import com.stripe.android.testing.AbsFakeStripeRepository
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.withContext
import java.io.File

/** Holds responses below the real interactor and crypto repository until the host has been destroyed. */
internal class FakeLifecycleNetworkClient : StripeNetworkClient {
    val requests = Turbine<String>()
    val responses = Turbine<Unit>()
    val release = CompletableDeferred<Unit>()

    suspend fun awaitResponse(path: String) {
        requests.add(path)
        // Exercise a late response even if the calling host's scope has been cancelled.
        withContext(NonCancellable) { release.await() }
        responses.add(Unit)
    }

    override suspend fun executeRequest(request: StripeRequest): StripeResponse<String> {
        val path = request.url.substringBefore('?').substringAfterLast('/')
        val body = when (path) {
            "start_identity_verification" ->
                """{"id":"vs_test","url":"https://example.com","ephemeral_key":"ek_test"}"""
            "kyc_data_retrieve" -> """{
                "first_name":"Test","last_name":"User","id_number_last4":"0000",
                "id_type":"social_security_number","dob":{"day":1,"month":1,"year":1990},
                "address":{"country":"US"}
            }"""
            "crs_carf_declaration" -> """{"text":"Attestation","version":"1"}"""
            "kyc_requirements" -> """{"requirements":{"source_of_funds":{
                "description":"Source of funds","requested_by":"swapped","awaiting_action_from":"user",
                "errors":[],"additional_requirements":{"questionnaire":{"questions":[{
                    "id":"purpose","prompt":"Purpose?","answer_type":"free_text","required":true,
                    "max_questionnaire_answer_characters":5000
                }]}}
            }}}"""
            "platform_settings" -> """{"publishable_key":"pk_test_platform"}"""
            "customers" -> """{"id":"cc_test"}"""
            "refresh_consumer_person" -> "{}"
            else -> error("Unexpected request: ${request.url}")
        }
        awaitResponse(path)
        return StripeResponse(200, body)
    }

    override suspend fun executeRequestForFile(request: StripeRequest, outputFile: File): StripeResponse<File> =
        error("Unexpected file request: ${request.url}")

    fun ensureAllEventsConsumed() {
        requests.ensureAllEventsConsumed()
        responses.ensureAllEventsConsumed()
    }
}

internal class FakeLifecycleStripeRepository(
    private val network: FakeLifecycleNetworkClient,
) : AbsFakeStripeRepository() {
    override suspend fun createToken(tokenParams: TokenParams, options: ApiRequest.Options): Result<Token> {
        network.awaitResponse("tokens")
        return Result.failure(IllegalStateException("Test credential rejected"))
    }
}
