package com.stripe.android.crypto.onramp

import android.app.Application
import androidx.lifecycle.SavedStateHandle
import com.google.common.truth.Truth.assertThat
import com.stripe.android.core.model.StripeFile
import com.stripe.android.crypto.onramp.analytics.OnrampAnalyticsService
import com.stripe.android.crypto.onramp.exception.MissingAdditionalKycFileIdException
import com.stripe.android.crypto.onramp.exception.MissingConsumerSecretException
import com.stripe.android.crypto.onramp.exception.OnrampErrorLogger
import com.stripe.android.crypto.onramp.model.AdditionalKycDocumentSubmission
import com.stripe.android.crypto.onramp.model.AdditionalKycDocumentSubmissionRequest
import com.stripe.android.crypto.onramp.model.AdditionalKycQuestionnaireAnswer
import com.stripe.android.crypto.onramp.model.AdditionalKycQuestionnaireAnswerRequest
import com.stripe.android.crypto.onramp.model.AdditionalKycQuestionnaireSubmission
import com.stripe.android.crypto.onramp.model.AdditionalKycQuestionnaireSubmissionRequest
import com.stripe.android.crypto.onramp.model.AdditionalKycSubmission
import com.stripe.android.crypto.onramp.model.AdditionalKycSubmissionResponse
import com.stripe.android.crypto.onramp.model.OnrampSessionClientSecretProvider
import com.stripe.android.crypto.onramp.repositories.CryptoApiRepository
import com.stripe.android.link.LinkController
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.inOrder
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import java.io.File

@RunWith(RobolectricTestRunner::class)
class OnrampInteractorFulfillAdditionalKycRequirementTest {
    @Test
    fun `documents are uploaded in order before submission`() = runScenario {
        val response = submissionResponse()
        val expectedDocuments = documentRequests(fileIds = listOf("file_1", "file_2"))
        val expectedQuestionnaire = questionnaireRequest()
        whenever(cryptoApiRepository.uploadAdditionalKycDocument(firstFile))
            .thenReturn(Result.success(StripeFile(id = "file_1")))
        whenever(cryptoApiRepository.uploadAdditionalKycDocument(secondFile))
            .thenReturn(Result.success(StripeFile(id = "file_2")))
        whenever(
            cryptoApiRepository.fulfillAdditionalKycRequirement(
                liquidityProvider = "swapped",
                submissionType = "document",
                documents = expectedDocuments,
                questionnaire = expectedQuestionnaire,
                consumerSessionClientSecret = CONSUMER_SESSION_CLIENT_SECRET,
            )
        ).thenReturn(Result.success(response))

        val result = interactor.fulfillAdditionalKycRequirement(
            documentSubmission(files = listOf(firstFile, secondFile))
        )

        assertThat(result.getOrThrow()).isSameInstanceAs(response)
        inOrder(cryptoApiRepository) {
            verify(cryptoApiRepository).uploadAdditionalKycDocument(firstFile)
            verify(cryptoApiRepository).uploadAdditionalKycDocument(secondFile)
            verify(cryptoApiRepository).fulfillAdditionalKycRequirement(
                liquidityProvider = "swapped",
                submissionType = "document",
                documents = expectedDocuments,
                questionnaire = expectedQuestionnaire,
                consumerSessionClientSecret = CONSUMER_SESSION_CLIENT_SECRET,
            )
        }
    }

    @Test
    fun `questionnaire-only submission bypasses uploads`() = runScenario {
        val response = submissionResponse(submissionType = "questionnaire")
        val expectedQuestionnaire = AdditionalKycQuestionnaireSubmissionRequest(
            answers = listOf(
                AdditionalKycQuestionnaireAnswerRequest(
                    questionId = "purchase_purpose",
                    value = "Long-term savings",
                )
            )
        )
        whenever(
            cryptoApiRepository.fulfillAdditionalKycRequirement(
                liquidityProvider = "swapped",
                submissionType = "questionnaire",
                documents = null,
                questionnaire = expectedQuestionnaire,
                consumerSessionClientSecret = CONSUMER_SESSION_CLIENT_SECRET,
            )
        ).thenReturn(Result.success(response))

        val result = interactor.fulfillAdditionalKycRequirement(questionnaireSubmission())

        assertThat(result.getOrThrow()).isSameInstanceAs(response)
        verify(cryptoApiRepository, never()).uploadAdditionalKycDocument(any())
        verify(cryptoApiRepository).fulfillAdditionalKycRequirement(
            liquidityProvider = "swapped",
            submissionType = "questionnaire",
            documents = null,
            questionnaire = expectedQuestionnaire,
            consumerSessionClientSecret = CONSUMER_SESSION_CLIENT_SECRET,
        )
    }

    @Test
    fun `missing consumer secret fails before uploading`() = runScenario(
        consumerSessionClientSecret = null,
    ) {
        val result = interactor.fulfillAdditionalKycRequirement(
            documentSubmission(files = listOf(firstFile))
        )

        assertThat(result.exceptionOrNull()).isInstanceOf(MissingConsumerSecretException::class.java)
        verify(cryptoApiRepository, never()).uploadAdditionalKycDocument(any())
        verifyFulfillmentWasNotRequested()
    }

    @Test
    fun `upload failure stops remaining uploads and submission`() = runScenario {
        val uploadError = IllegalStateException("Upload failed")
        whenever(cryptoApiRepository.uploadAdditionalKycDocument(firstFile))
            .thenReturn(Result.failure(uploadError))

        val result = interactor.fulfillAdditionalKycRequirement(
            documentSubmission(files = listOf(firstFile, secondFile))
        )

        assertThat(result.exceptionOrNull()).isSameInstanceAs(uploadError)
        verify(cryptoApiRepository, never()).uploadAdditionalKycDocument(secondFile)
        verifyFulfillmentWasNotRequested()
    }

    @Test
    fun `uploaded file without an ID fails before submission`() = runScenario {
        whenever(cryptoApiRepository.uploadAdditionalKycDocument(firstFile))
            .thenReturn(Result.success(StripeFile(id = null)))

        val result = interactor.fulfillAdditionalKycRequirement(
            documentSubmission(files = listOf(firstFile))
        )

        assertThat(result.exceptionOrNull()).isInstanceOf(MissingAdditionalKycFileIdException::class.java)
        verifyFulfillmentWasNotRequested()
    }

    @Test
    fun `submission failure is propagated`() = runScenario {
        val submissionError = IllegalStateException("Submission failed")
        val questionnaire = AdditionalKycQuestionnaireSubmissionRequest(
            answers = listOf(
                AdditionalKycQuestionnaireAnswerRequest(
                    questionId = "purchase_purpose",
                    value = "Long-term savings",
                )
            )
        )
        whenever(
            cryptoApiRepository.fulfillAdditionalKycRequirement(
                liquidityProvider = "swapped",
                submissionType = "questionnaire",
                documents = null,
                questionnaire = questionnaire,
                consumerSessionClientSecret = CONSUMER_SESSION_CLIENT_SECRET,
            )
        ).thenReturn(Result.failure(submissionError))

        val result = interactor.fulfillAdditionalKycRequirement(questionnaireSubmission())

        assertThat(result.exceptionOrNull()).isSameInstanceAs(submissionError)
    }

    private fun runScenario(
        consumerSessionClientSecret: String? = CONSUMER_SESSION_CLIENT_SECRET,
        block: suspend Scenario.() -> Unit,
    ) = runTest {
        val application: Application = RuntimeEnvironment.getApplication()
        val linkController = mock<LinkController>()
        val cryptoApiRepository = mock<CryptoApiRepository>()
        whenever(linkController.state(any())).thenReturn(
            MutableStateFlow(linkState(consumerSessionClientSecret))
        )

        Scenario(
            interactor = OnrampInteractor(
                application = application,
                linkController = linkController,
                cryptoApiRepository = cryptoApiRepository,
                analyticsServiceFactory = mock<OnrampAnalyticsService.Factory>(),
                errorLogger = mock<OnrampErrorLogger>(),
                checkoutHandler = OnrampSessionClientSecretProvider { "unused" },
                savedStateHandle = SavedStateHandle(),
            ),
            cryptoApiRepository = cryptoApiRepository,
            firstFile = File("first.pdf"),
            secondFile = File("second.pdf"),
        ).block()
    }

    private data class Scenario(
        val interactor: OnrampInteractor,
        val cryptoApiRepository: CryptoApiRepository,
        val firstFile: File,
        val secondFile: File,
    ) {
        suspend fun verifyFulfillmentWasNotRequested() {
            verify(cryptoApiRepository, never()).fulfillAdditionalKycRequirement(
                liquidityProvider = any(),
                submissionType = any(),
                documents = anyOrNull(),
                questionnaire = anyOrNull(),
                consumerSessionClientSecret = any(),
            )
        }
    }

    private companion object {
        const val CONSUMER_SESSION_CLIENT_SECRET = "secret_123"
        fun documentRequests(fileIds: List<String>): List<AdditionalKycDocumentSubmissionRequest> {
            return listOf(
                AdditionalKycDocumentSubmissionRequest(
                    documentType = "source_of_funds",
                    documentSubtype = "bank_statement",
                    fileIds = fileIds,
                )
            )
        }

        fun questionnaireRequest(): AdditionalKycQuestionnaireSubmissionRequest {
            return AdditionalKycQuestionnaireSubmissionRequest(
                answers = listOf(
                    AdditionalKycQuestionnaireAnswerRequest(
                        questionId = "purchase_purpose",
                        value = "Long-term savings",
                    )
                )
            )
        }

        fun documentSubmission(files: List<File>): AdditionalKycSubmission {
            return AdditionalKycSubmission(
                liquidityProvider = "swapped",
                submissionType = "document",
                documents = listOf(
                    AdditionalKycDocumentSubmission(
                        documentType = "source_of_funds",
                        documentSubtype = "bank_statement",
                        files = files,
                    )
                ),
                questionnaire = AdditionalKycQuestionnaireSubmission(
                    answers = listOf(
                        AdditionalKycQuestionnaireAnswer(
                            questionId = "purchase_purpose",
                            value = "Long-term savings",
                        )
                    )
                ),
            )
        }

        fun questionnaireSubmission(): AdditionalKycSubmission {
            return AdditionalKycSubmission(
                liquidityProvider = "swapped",
                submissionType = "questionnaire",
                documents = null,
                questionnaire = AdditionalKycQuestionnaireSubmission(
                    answers = listOf(
                        AdditionalKycQuestionnaireAnswer(
                            questionId = "purchase_purpose",
                            value = "Long-term savings",
                        )
                    )
                ),
            )
        }

        fun submissionResponse(
            submissionType: String = "document",
        ): AdditionalKycSubmissionResponse {
            return AdditionalKycSubmissionResponse(
                id = "kyc_submission_123",
                objectType = "crypto_onramp_kyc_submission",
                liquidityProvider = "swapped",
                submissionType = submissionType,
                submittedAt = 1_786_998_400,
            )
        }

        fun linkState(consumerSessionClientSecret: String?): LinkController.State {
            return LinkController.State(
                internalLinkAccount = LinkController.LinkAccount(
                    email = "test@example.com",
                    redactedPhoneNumber = "***-***-1234",
                    sessionState = LinkController.SessionState.LoggedIn,
                    consumerSessionClientSecret = consumerSessionClientSecret,
                ),
                merchantLogoUrl = null,
                selectedPaymentMethodPreview = null,
                createdPaymentMethod = null,
                elementsSessionId = null,
            )
        }
    }
}
