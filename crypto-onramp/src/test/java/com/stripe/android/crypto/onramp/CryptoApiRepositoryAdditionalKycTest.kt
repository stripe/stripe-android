package com.stripe.android.crypto.onramp

import app.cash.turbine.Turbine
import com.google.common.truth.Truth.assertThat
import com.stripe.android.core.ApiConfiguration
import com.stripe.android.core.exception.APIException
import com.stripe.android.core.networking.ApiRequest
import com.stripe.android.core.networking.HEADER_STRIPE_VERSION
import com.stripe.android.core.networking.StripeNetworkClient
import com.stripe.android.core.networking.StripeRequest
import com.stripe.android.core.networking.StripeResponse
import com.stripe.android.crypto.onramp.model.AdditionalKycCollectionSubmissionRequest
import com.stripe.android.crypto.onramp.model.AdditionalKycDocumentSubmissionRequest
import com.stripe.android.crypto.onramp.model.AdditionalKycQuestionnaireAnswerRequest
import com.stripe.android.crypto.onramp.model.AdditionalKycQuestionnaireSubmissionRequest
import com.stripe.android.crypto.onramp.model.AdditionalKycRequirementSubmissionRequest
import com.stripe.android.crypto.onramp.repositories.CryptoApiRepository
import com.stripe.android.crypto.onramp.repositories.CryptoApiRepository.Companion.CRYPTO_ONRAMP_API_VERSION
import com.stripe.android.link.LinkController
import com.stripe.android.networking.StripeRepository
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.mock
import org.robolectric.RobolectricTestRunner
import java.io.ByteArrayOutputStream
import java.io.File
import java.net.URLDecoder

@RunWith(RobolectricTestRunner::class)
class CryptoApiRepositoryAdditionalKycTest {
    @Test
    fun `keyed requirements use Link authentication and accept an empty response`() = runScenario {
        val result = repository.fulfillKycRequirements(
            requirements = mapOf(
                "proof_of_address" to requirement(
                    documents = listOf(document("utility_provider", "file_poa")),
                ),
                "source_of_funds" to requirement(
                    documents = listOf(
                        document("payslip", "file_payslip_1", "file_payslip_2"),
                        document("bank_statement", "file_bank_statement"),
                    ),
                    additionalRequirements = questionnaire(),
                ),
            ),
            linkSessionKey = LINK_SESSION_KEY,
        )
        val request = network.requests.awaitItem() as ApiRequest

        assertThat(result.getOrThrow()).isEqualTo(Unit)
        assertSubmissionRequest(request)
    }

    @Test
    fun `document subtype is included and optional additional requirements are omitted`() = runScenario {
        val result = repository.fulfillKycRequirements(
            requirements = mapOf(
                "proof_of_address" to requirement(documents = listOf(document("utility_provider", "file_1")))
            ),
            linkSessionKey = LINK_SESSION_KEY,
        )
        val request = network.requests.awaitItem() as ApiRequest

        assertThat(result.getOrThrow()).isEqualTo(Unit)
        assertThat(request.params).isEqualTo(
            mapOf(
                "requirements" to mapOf(
                    "proof_of_address" to mapOf(
                        "requested_by" to "swapped",
                        "documents" to listOf(
                            mapOf("document_subtype" to "utility_provider", "file_ids" to listOf("file_1"))
                        ),
                    )
                )
            )
        )
    }

    @Test
    fun `questionnaire-only submission omits documents from the request body`() = runScenario {
        val result = repository.fulfillKycRequirements(
            requirements = mapOf("source_of_funds" to requirement(emptyList(), questionnaire())),
            linkSessionKey = LINK_SESSION_KEY,
        )
        val request = network.requests.awaitItem() as ApiRequest

        assertThat(result.getOrThrow()).isEqualTo(Unit)
        assertThat(decodedBody(request).split('&')).containsExactly(
            "requirements[source_of_funds][requested_by]=swapped",
            "requirements[source_of_funds][additional_requirements][questionnaire][answers][0]" +
                "[question_id]=purchase_purpose",
            "requirements[source_of_funds][additional_requirements][questionnaire][answers][0]" +
                "[value]=Personal investment",
        )
    }

    @Test
    fun `unknown success metadata is ignored`() = runScenario {
        network.response = StripeResponse(200, """{"submitted_at":1723264800}""")

        val result = repository.fulfillKycRequirements(emptyMap(), LINK_SESSION_KEY)
        network.requests.awaitItem()

        assertThat(result.getOrThrow()).isEqualTo(Unit)
    }

    @Test
    fun `fulfillment API error is propagated`() = runScenario {
        network.response = StripeResponse(
            400,
            """{"error":{"message":"Invalid file","type":"invalid_request_error"}}""",
        )

        val result = repository.fulfillKycRequirements(emptyMap(), LINK_SESSION_KEY)
        network.requests.awaitItem()

        assertThat(result.exceptionOrNull()).isInstanceOf(APIException::class.java)
        assertThat((result.exceptionOrNull() as APIException).statusCode).isEqualTo(400)
    }

    @Test
    fun `malformed fulfillment response fails parsing`() = runScenario {
        network.response = StripeResponse(200, "not JSON")

        val result = repository.fulfillKycRequirements(emptyMap(), LINK_SESSION_KEY)
        network.requests.awaitItem()

        assertThat(result.exceptionOrNull()).isInstanceOf(APIException::class.java)
    }

    private fun assertSubmissionRequest(request: ApiRequest) {
        assertThat(request.method).isEqualTo(StripeRequest.Method.POST)
        assertThat(request.baseUrl).isEqualTo(
            "https://api.stripe.com/v1/crypto/internal/fulfill_kyc_requirements"
        )
        assertThat(request.headers["Authorization"]).isEqualTo("Bearer $LINK_SESSION_KEY")
        assertThat(request.headers["Stripe-Account"]).isEqualTo("acct_123")
        assertThat(request.headers[HEADER_STRIPE_VERSION]).isEqualTo(CRYPTO_ONRAMP_API_VERSION)
        assertThat(request.params).isEqualTo(
            mapOf(
                "requirements" to mapOf(
                    "proof_of_address" to mapOf(
                        "requested_by" to "swapped",
                        "documents" to listOf(
                            mapOf("document_subtype" to "utility_provider", "file_ids" to listOf("file_poa"))
                        ),
                    ),
                    "source_of_funds" to mapOf(
                        "requested_by" to "swapped",
                        "documents" to listOf(
                            mapOf(
                                "document_subtype" to "payslip",
                                "file_ids" to listOf("file_payslip_1", "file_payslip_2"),
                            ),
                            mapOf(
                                "document_subtype" to "bank_statement",
                                "file_ids" to listOf("file_bank_statement"),
                            ),
                        ),
                        "additional_requirements" to mapOf(
                            "questionnaire" to mapOf(
                                "answers" to listOf(
                                    mapOf("question_id" to "purchase_purpose", "value" to "Personal investment")
                                )
                            )
                        ),
                    ),
                )
            )
        )
        val decodedBody = decodedBody(request)
        assertThat(decodedBody).contains("requirements[source_of_funds][documents][0][file_ids][]=file_payslip_1")
        assertThat(decodedBody).doesNotContain("consumer_session_client_secret")
        assertThat(decodedBody).doesNotContain("liquidity_provider")
        assertThat(decodedBody).doesNotContain("[document_type]")
    }

    private fun decodedBody(request: ApiRequest): String {
        val body = ByteArrayOutputStream().also(request::writePostBody).toString(Charsets.UTF_8.name())
        return URLDecoder.decode(body, Charsets.UTF_8.name())
    }

    private fun runScenario(block: suspend Scenario.() -> Unit) = runTest {
        val network = FakeKycNetworkClient()
        Scenario(
            repository = CryptoApiRepository(
                stripeNetworkClient = network,
                stripeRepository = mock<StripeRepository>(),
                linkController = mock<LinkController>(),
                apiConfigProvider = { ApiConfiguration.State("pk_test_123", "acct_123") },
                apiVersion = CRYPTO_ONRAMP_API_VERSION,
                appInfo = null,
            ),
            network = network,
        ).block()
        network.requests.ensureAllEventsConsumed()
    }

    private data class Scenario(
        val repository: CryptoApiRepository,
        val network: FakeKycNetworkClient,
    )

    internal class FakeKycNetworkClient : StripeNetworkClient {
        val requests = Turbine<StripeRequest>()
        var response = StripeResponse(200, "{}")

        override suspend fun executeRequest(request: StripeRequest): StripeResponse<String> {
            requests.add(request)
            return response
        }

        override suspend fun executeRequestForFile(request: StripeRequest, outputFile: File): StripeResponse<File> {
            error("Unexpected download")
        }
    }

    private companion object {
        const val LINK_SESSION_KEY = "lsk_test_123"

        fun requirement(
            documents: List<AdditionalKycDocumentSubmissionRequest>,
            additionalRequirements: AdditionalKycCollectionSubmissionRequest? = null,
        ) = AdditionalKycRequirementSubmissionRequest("swapped", documents, additionalRequirements)

        fun document(subtype: String, vararg fileIds: String) =
            AdditionalKycDocumentSubmissionRequest(subtype, fileIds.toList())

        fun questionnaire() = AdditionalKycCollectionSubmissionRequest(
            questionnaire = AdditionalKycQuestionnaireSubmissionRequest(
                answers = listOf(AdditionalKycQuestionnaireAnswerRequest("purchase_purpose", "Personal investment"))
            )
        )
    }
}
