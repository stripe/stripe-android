package com.stripe.android.crypto.onramp

import com.google.common.truth.Truth.assertThat
import com.stripe.android.core.networking.ApiRequest
import com.stripe.android.core.networking.HEADER_STRIPE_VERSION
import com.stripe.android.core.networking.StripeNetworkClient
import com.stripe.android.core.networking.StripeRequest
import com.stripe.android.core.networking.StripeResponse
import com.stripe.android.core.version.StripeSdkVersion
import com.stripe.android.crypto.onramp.model.AdditionalKycDocumentSubmissionRequest
import com.stripe.android.crypto.onramp.model.AdditionalKycQuestionnaireAnswerRequest
import com.stripe.android.crypto.onramp.model.AdditionalKycQuestionnaireSubmissionRequest
import com.stripe.android.crypto.onramp.model.AdditionalKycSubmissionResponse
import com.stripe.android.crypto.onramp.repositories.CryptoApiRepository
import com.stripe.android.crypto.onramp.repositories.CryptoApiRepository.Companion.CRYPTO_ONRAMP_API_VERSION
import com.stripe.android.link.LinkController
import com.stripe.android.networking.StripeRepository
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.KArgumentCaptor
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class CryptoApiRepositoryAdditionalKycTest {
    @Test
    fun `document and questionnaire submission is sent and parsed`() = runScenario(
        responseBody = documentSubmissionResponse,
    ) {
        val result = repository.fulfillAdditionalKycRequirement(
            liquidityProvider = "swapped",
            submissionType = "document",
            documents = listOf(
                AdditionalKycDocumentSubmissionRequest(
                    documentType = "source_of_funds",
                    documentSubtype = "bank_statement",
                    fileIds = listOf("file_1", "file_2"),
                )
            ),
            questionnaire = AdditionalKycQuestionnaireSubmissionRequest(
                answers = listOf(
                    AdditionalKycQuestionnaireAnswerRequest(
                        questionId = "purchase_purpose",
                        value = "Personal investment",
                    )
                )
            ),
            consumerSessionClientSecret = "secret_123",
        )

        val request = captureRequest()
        assertDocumentAndQuestionnaireRequest(request)
        assertDocumentAndQuestionnaireResponse(result.getOrThrow())
    }

    @Test
    fun `questionnaire submission omits documents`() = runScenario(
        responseBody = questionnaireSubmissionResponse,
    ) {
        val result = repository.fulfillAdditionalKycRequirement(
            liquidityProvider = "swapped",
            submissionType = "questionnaire",
            documents = null,
            questionnaire = AdditionalKycQuestionnaireSubmissionRequest(
                answers = listOf(
                    AdditionalKycQuestionnaireAnswerRequest(
                        questionId = "funding_sources",
                        value = "Salary",
                    )
                )
            ),
            consumerSessionClientSecret = "secret_123",
        )

        val request = captureRequest()
        assertThat(request.params).isEqualTo(
            mapOf(
                "credentials" to mapOf("consumer_session_client_secret" to "secret_123"),
                "liquidity_provider" to "swapped",
                "submission_type" to "questionnaire",
                "questionnaire" to mapOf(
                    "answers" to listOf(
                        mapOf(
                            "question_id" to "funding_sources",
                            "value" to "Salary",
                        )
                    )
                ),
            )
        )
        assertThat(result.getOrThrow().documents).isNull()
    }

    @Test
    fun `minimal fulfillment response is parsed`() = runScenario(
        responseBody = minimalSubmissionResponse,
    ) {
        val result = repository.fulfillAdditionalKycRequirement(
            liquidityProvider = "swapped",
            submissionType = "questionnaire",
            documents = null,
            questionnaire = null,
            consumerSessionClientSecret = "secret_123",
        ).getOrThrow()

        assertThat(result.id).isEqualTo("submission_123")
        assertThat(result.objectType).isEqualTo("crypto_onramp_kyc_submission")
        assertThat(result.status).isEqualTo("pending_verification")
        assertThat(result.liquidityProvider).isNull()
        assertThat(result.submissionType).isNull()
        assertThat(result.submittedAt).isNull()
        assertThat(result.created).isNull()
    }

    @Test
    fun `optional document subtype and questionnaire are omitted`() = runScenario(
        responseBody = documentSubmissionWithoutOptionalFieldsResponse,
    ) {
        val result = repository.fulfillAdditionalKycRequirement(
            liquidityProvider = "swapped",
            submissionType = "document",
            documents = listOf(
                AdditionalKycDocumentSubmissionRequest(
                    documentType = "proof_of_address",
                    documentSubtype = null,
                    fileIds = listOf("file_1"),
                )
            ),
            questionnaire = null,
            consumerSessionClientSecret = "secret_123",
        )

        val request = captureRequest()
        assertThat(request.params).isEqualTo(
            mapOf(
                "credentials" to mapOf("consumer_session_client_secret" to "secret_123"),
                "liquidity_provider" to "swapped",
                "submission_type" to "document",
                "documents" to listOf(
                    mapOf(
                        "document_type" to "proof_of_address",
                        "file_ids" to listOf("file_1"),
                    )
                ),
            )
        )
        assertThat(requireNotNull(result.getOrThrow().documents).single().documentSubtype).isNull()
    }

    private fun assertDocumentAndQuestionnaireRequest(request: ApiRequest) {
        assertThat(request.method).isEqualTo(StripeRequest.Method.POST)
        assertThat(request.baseUrl).isEqualTo(
            "https://api.stripe.com/v1/crypto/internal/fulfill_additional_kyc_requirement"
        )
        assertThat(request.headers[HEADER_STRIPE_VERSION]).isEqualTo(CRYPTO_ONRAMP_API_VERSION)
        assertThat(request.params).isEqualTo(
            mapOf(
                "credentials" to mapOf("consumer_session_client_secret" to "secret_123"),
                "liquidity_provider" to "swapped",
                "submission_type" to "document",
                "documents" to listOf(
                    mapOf(
                        "document_type" to "source_of_funds",
                        "document_subtype" to "bank_statement",
                        "file_ids" to listOf("file_1", "file_2"),
                    )
                ),
                "questionnaire" to mapOf(
                    "answers" to listOf(
                        mapOf(
                            "question_id" to "purchase_purpose",
                            "value" to "Personal investment",
                        )
                    )
                ),
            )
        )
    }

    private fun assertDocumentAndQuestionnaireResponse(response: AdditionalKycSubmissionResponse) {
        assertThat(response.id).isEqualTo("cks_123")
        assertThat(response.objectType).isEqualTo("crypto.kyc_submission")
        assertThat(response.liquidityProvider).isEqualTo("swapped")
        assertThat(response.submissionType).isEqualTo("document")
        val document = requireNotNull(response.documents).single()
        assertThat(document.documentType).isEqualTo("source_of_funds")
        assertThat(document.documentSubtype).isEqualTo("bank_statement")
        assertThat(document.fileIds).containsExactly("file_1", "file_2").inOrder()
        assertThat(document.status).isEqualTo("pending_verification")
        val questionnaire = requireNotNull(response.questionnaire)
        assertThat(questionnaire.answers.single().questionId).isEqualTo("purchase_purpose")
        assertThat(questionnaire.answers.single().value).isEqualTo("Personal investment")
        assertThat(response.status).isEqualTo("pending_verification")
        assertThat(response.created).isEqualTo(1723264800L)
    }

    private fun runScenario(
        responseBody: String,
        block: suspend Scenario.() -> Unit,
    ) = runTest {
        val stripeNetworkClient = mock<StripeNetworkClient>()
        whenever(stripeNetworkClient.executeRequest(any<ApiRequest>())).thenReturn(
            StripeResponse(
                code = 200,
                body = responseBody,
                headers = emptyMap(),
            )
        )

        val requestCaptor = argumentCaptor<ApiRequest>()
        Scenario(
            repository = CryptoApiRepository(
                stripeNetworkClient = stripeNetworkClient,
                stripeRepository = mock<StripeRepository>(),
                linkController = mock<LinkController>(),
                publishableKeyProvider = { "pk_test_123" },
                stripeAccountIdProvider = { null },
                apiVersion = CRYPTO_ONRAMP_API_VERSION,
                sdkVersion = StripeSdkVersion.VERSION,
                appInfo = null,
            ),
            stripeNetworkClient = stripeNetworkClient,
            requestCaptor = requestCaptor,
        ).block()
    }

    private data class Scenario(
        val repository: CryptoApiRepository,
        val stripeNetworkClient: StripeNetworkClient,
        val requestCaptor: KArgumentCaptor<ApiRequest>,
    ) {
        suspend fun captureRequest(): ApiRequest {
            verify(stripeNetworkClient).executeRequest(requestCaptor.capture())
            return requestCaptor.firstValue
        }
    }

    private companion object {
        val documentSubmissionResponse =
            """
                {
                  "id": "cks_123",
                  "object": "crypto.kyc_submission",
                  "liquidity_provider": "swapped",
                  "submission_type": "document",
                  "status": "pending_verification",
                  "documents": [
                    {
                      "document_type": "source_of_funds",
                      "document_subtype": "bank_statement",
                      "file_ids": ["file_1", "file_2"],
                      "status": "pending_verification"
                    }
                  ],
                  "questionnaire": {
                    "answers": [
                      {
                        "question_id": "purchase_purpose",
                        "value": "Personal investment"
                      }
                    ]
                  },
                  "created": 1723264800
                }
            """.trimIndent()

        val questionnaireSubmissionResponse =
            """
                {
                  "id": "cks_124",
                  "object": "crypto.kyc_submission",
                  "liquidity_provider": "swapped",
                  "submission_type": "questionnaire",
                  "questionnaire": {
                    "answers": [
                      {
                        "question_id": "funding_sources",
                        "value": "Salary"
                      }
                    ]
                  },
                  "submitted_at": 1723264801
                }
            """.trimIndent()

        val documentSubmissionWithoutOptionalFieldsResponse =
            """
                {
                  "id": "cks_125",
                  "object": "crypto.kyc_submission",
                  "liquidity_provider": "swapped",
                  "submission_type": "document",
                  "documents": [
                    {
                      "document_type": "proof_of_address",
                      "file_ids": ["file_1"]
                    }
                  ],
                  "submitted_at": 1723264802
                }
            """.trimIndent()

        val minimalSubmissionResponse =
            """
                {
                  "id": "submission_123",
                  "object": "crypto_onramp_kyc_submission",
                  "status": "pending_verification"
                }
            """.trimIndent()
    }
}
