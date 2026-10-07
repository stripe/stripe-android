package com.stripe.android.crypto.onramp

import android.app.Application
import androidx.lifecycle.SavedStateHandle
import com.google.common.truth.Truth.assertThat
import com.stripe.android.core.model.StripeFile
import com.stripe.android.crypto.onramp.analytics.OnrampAnalyticsService
import com.stripe.android.crypto.onramp.exception.LinkAccountNotVerifiedException
import com.stripe.android.crypto.onramp.exception.MissingKycFileIdException
import com.stripe.android.crypto.onramp.exception.MissingLinkSessionKeyException
import com.stripe.android.crypto.onramp.exception.OnrampErrorLogger
import com.stripe.android.crypto.onramp.exception.UnexpectedException
import com.stripe.android.crypto.onramp.model.KycCollectionSubmissionRequest
import com.stripe.android.crypto.onramp.model.KycDocumentSubmission
import com.stripe.android.crypto.onramp.model.KycDocumentSubmissionRequest
import com.stripe.android.crypto.onramp.model.KycQuestionnaireAnswer
import com.stripe.android.crypto.onramp.model.KycQuestionnaireAnswerRequest
import com.stripe.android.crypto.onramp.model.KycQuestionnaireSubmission
import com.stripe.android.crypto.onramp.model.KycQuestionnaireSubmissionRequest
import com.stripe.android.crypto.onramp.model.KycRequirementSubmission
import com.stripe.android.crypto.onramp.model.KycRequirementSubmissionRequest
import com.stripe.android.crypto.onramp.model.KycSubmission
import com.stripe.android.crypto.onramp.model.OnrampSessionClientSecretProvider
import com.stripe.android.crypto.onramp.repositories.CryptoApiRepository
import com.stripe.android.link.LinkController
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.any
import org.mockito.kotlin.inOrder
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import java.io.File

@RunWith(RobolectricTestRunner::class)
class OnrampInteractorFulfillKycRequirementsTest {
    @Test
    fun `uploaded file IDs are reused when retrying fulfillment`() = runScenario {
        val original = documentSubmission(emptyList())
        val requirement = original.requirements.getValue("source_of_funds")
        val submission = original.copy(
            requirements = mapOf(
                "source_of_funds" to requirement.copy(
                    documents = requirement.documents.map { it.copy(uploadedFileIds = listOf("file_existing")) }
                )
            )
        )
        val requests = requirementRequests(documentRequests(listOf("file_existing")), questionnaireRequest())
        whenever(cryptoApiRepository.fulfillKycRequirements(requests, LINK_SESSION_KEY))
            .thenReturn(Result.failure(IllegalStateException("Temporary failure")))
            .thenReturn(Result.success(Unit))

        assertThat(interactor.fulfillKycRequirements(submission).isFailure).isTrue()
        assertThat(interactor.fulfillKycRequirements(submission).isSuccess).isTrue()
        verify(cryptoApiRepository, never()).uploadKycDocument(any(), any())
    }

    @Test
    fun `selected document uploads with Link authentication and returns file ID`() = runScenario {
        whenever(cryptoApiRepository.uploadKycDocument(firstFile, LINK_SESSION_KEY))
            .thenReturn(Result.success(StripeFile(id = "file_selected")))

        assertThat(interactor.uploadKycDocument(firstFile).getOrThrow()).isEqualTo("file_selected")
        verify(cryptoApiRepository).uploadKycDocument(firstFile, LINK_SESSION_KEY)
        verify(cryptoApiRepository, never()).fulfillKycRequirements(any(), any())
    }

    @Test
    fun `documents are uploaded in order before submission`() = runScenario {
        val expectedDocuments = documentRequests(fileIds = listOf("file_1", "file_2"))
        val expectedQuestionnaire = questionnaireRequest()
        whenever(cryptoApiRepository.uploadKycDocument(firstFile, LINK_SESSION_KEY))
            .thenReturn(Result.success(StripeFile(id = "file_1")))
        whenever(cryptoApiRepository.uploadKycDocument(secondFile, LINK_SESSION_KEY))
            .thenReturn(Result.success(StripeFile(id = "file_2")))
        whenever(
            cryptoApiRepository.fulfillKycRequirements(
                requirements = requirementRequests(expectedDocuments, expectedQuestionnaire),
                linkSessionKey = LINK_SESSION_KEY,
            )
        ).thenReturn(Result.success(Unit))

        val result = interactor.fulfillKycRequirements(
            documentSubmission(files = listOf(firstFile, secondFile))
        )

        assertThat(result.getOrThrow()).isEqualTo(Unit)
        inOrder(cryptoApiRepository) {
            verify(cryptoApiRepository).uploadKycDocument(firstFile, LINK_SESSION_KEY)
            verify(cryptoApiRepository).uploadKycDocument(secondFile, LINK_SESSION_KEY)
            verify(cryptoApiRepository).fulfillKycRequirements(
                requirements = requirementRequests(expectedDocuments, expectedQuestionnaire),
                linkSessionKey = LINK_SESSION_KEY,
            )
        }
    }

    @Test
    fun `multiple requirements preserve their documents and questionnaire`() = runScenario {
        val sourceOfFunds = documentSubmission(listOf(secondFile)).requirements.getValue("source_of_funds")
        val submission = KycSubmission(
            requirements = mapOf(
                "proof_of_address" to KycRequirementSubmission(
                    requestedBy = "swapped",
                    documents = listOf(
                        KycDocumentSubmission("utility_provider", listOf(firstFile), emptyList())
                    ),
                    questionnaire = null,
                ),
                "source_of_funds" to sourceOfFunds,
            )
        )
        val expectedRequirements = mapOf(
            "proof_of_address" to KycRequirementSubmissionRequest(
                requestedBy = "swapped",
                documents = listOf(KycDocumentSubmissionRequest("utility_provider", listOf("file_poa"))),
                collectionRequirements = null,
            ),
            "source_of_funds" to KycRequirementSubmissionRequest(
                requestedBy = "swapped",
                documents = documentRequests(listOf("file_sof")),
                collectionRequirements = KycCollectionSubmissionRequest(questionnaireRequest()),
            ),
        )
        whenever(cryptoApiRepository.uploadKycDocument(firstFile, LINK_SESSION_KEY))
            .thenReturn(Result.success(StripeFile(id = "file_poa")))
        whenever(cryptoApiRepository.uploadKycDocument(secondFile, LINK_SESSION_KEY))
            .thenReturn(Result.success(StripeFile(id = "file_sof")))
        whenever(cryptoApiRepository.fulfillKycRequirements(expectedRequirements, LINK_SESSION_KEY))
            .thenReturn(Result.success(Unit))

        val result = interactor.fulfillKycRequirements(submission)

        assertThat(result.getOrThrow()).isEqualTo(Unit)
        inOrder(cryptoApiRepository) {
            verify(cryptoApiRepository).uploadKycDocument(firstFile, LINK_SESSION_KEY)
            verify(cryptoApiRepository).uploadKycDocument(secondFile, LINK_SESSION_KEY)
            verify(cryptoApiRepository).fulfillKycRequirements(expectedRequirements, LINK_SESSION_KEY)
        }
    }

    @Test
    fun `unverified Link account fails before uploading`() = runScenario(
        linkSessionState = LinkController.SessionState.NeedsVerification,
    ) {
        val result = interactor.fulfillKycRequirements(
            documentSubmission(files = listOf(firstFile))
        )

        assertUnexpectedError<LinkAccountNotVerifiedException>(result.exceptionOrNull())
        verify(cryptoApiRepository, never()).uploadKycDocument(any(), any())
        verifyFulfillmentWasNotRequested()
    }

    @Test
    fun `upload failure stops remaining uploads and submission`() = runScenario {
        val uploadError = IllegalStateException("Upload failed")
        whenever(cryptoApiRepository.uploadKycDocument(firstFile, LINK_SESSION_KEY))
            .thenReturn(Result.failure(uploadError))

        val result = interactor.fulfillKycRequirements(
            documentSubmission(files = listOf(firstFile, secondFile))
        )

        val error = assertUnexpectedError<IllegalStateException>(result.exceptionOrNull())
        assertThat(error.underlyingError).isSameInstanceAs(uploadError)
        verify(cryptoApiRepository, never()).uploadKycDocument(secondFile, LINK_SESSION_KEY)
        verifyFulfillmentWasNotRequested()
    }

    @Test
    fun `uploaded file without an ID fails before submission`() = runScenario {
        whenever(cryptoApiRepository.uploadKycDocument(firstFile, LINK_SESSION_KEY))
            .thenReturn(Result.success(StripeFile(id = null)))

        val result = interactor.fulfillKycRequirements(
            documentSubmission(files = listOf(firstFile))
        )

        val error = assertUnexpectedError<MissingKycFileIdException>(result.exceptionOrNull())
        assertThat(error.code).isEqualTo("unexpected_error")
        assertThat(error.userMessage).isEqualTo("Something went wrong. Please try again later.")
        assertThat(error.developerMessage).contains("Uploaded KYC document is missing a file ID")
        assertThat(error.developerMessage).contains("operation: fulfill_kyc_requirement")
        assertThat(error.docUrl).isNull()
        verifyFulfillmentWasNotRequested()
    }

    @Test
    fun `submission failure is propagated`() = runScenario {
        val submissionError = IllegalStateException("Submission failed")
        val documents = documentRequests(fileIds = listOf("file_1"))
        val questionnaire = questionnaireRequest()
        whenever(cryptoApiRepository.uploadKycDocument(firstFile, LINK_SESSION_KEY))
            .thenReturn(Result.success(StripeFile(id = "file_1")))
        whenever(
            cryptoApiRepository.fulfillKycRequirements(
                requirements = requirementRequests(documents, questionnaire),
                linkSessionKey = LINK_SESSION_KEY,
            )
        ).thenReturn(Result.failure(submissionError))

        val result = interactor.fulfillKycRequirements(
            documentSubmission(files = listOf(firstFile))
        )

        val error = assertUnexpectedError<IllegalStateException>(result.exceptionOrNull())
        assertThat(error.underlyingError).isSameInstanceAs(submissionError)
    }

    @Test
    fun `missing Link session key fails before uploading`() = runScenario(
        linkSessionKey = null,
    ) {
        val result = interactor.fulfillKycRequirements(
            documentSubmission(files = listOf(firstFile))
        )

        assertUnexpectedError<MissingLinkSessionKeyException>(result.exceptionOrNull())
        verify(cryptoApiRepository, never()).uploadKycDocument(any(), any())
        verifyFulfillmentWasNotRequested()
    }

    @Test
    fun `blank Link session key fails before uploading`() = runScenario(
        linkSessionKey = "   ",
    ) {
        val result = interactor.fulfillKycRequirements(
            documentSubmission(files = listOf(firstFile))
        )

        assertUnexpectedError<MissingLinkSessionKeyException>(result.exceptionOrNull())
        verify(cryptoApiRepository, never()).uploadKycDocument(any(), any())
        verifyFulfillmentWasNotRequested()
    }

    @Test
    fun `questionnaire submission requires a Link session key`() = runScenario(
        linkSessionKey = null,
    ) {
        val result = interactor.fulfillKycRequirements(questionnaireSubmission())

        assertUnexpectedError<MissingLinkSessionKeyException>(result.exceptionOrNull())
        verify(cryptoApiRepository, never()).uploadKycDocument(any(), any())
        verifyFulfillmentWasNotRequested()
    }

    @Test
    fun `questionnaire submission uses Link session key without a consumer secret`() = runScenario(
        consumerSessionClientSecret = null,
    ) {
        val requirements = requirementRequests(emptyList(), questionnaireRequest())
        whenever(cryptoApiRepository.fulfillKycRequirements(requirements, LINK_SESSION_KEY))
            .thenReturn(Result.success(Unit))

        val result = interactor.fulfillKycRequirements(questionnaireSubmission())

        assertThat(result.getOrThrow()).isEqualTo(Unit)
        verify(cryptoApiRepository).fulfillKycRequirements(requirements, LINK_SESSION_KEY)
        verify(cryptoApiRepository, never()).uploadKycDocument(any(), any())
    }

    private fun runScenario(
        consumerSessionClientSecret: String? = CONSUMER_SESSION_CLIENT_SECRET,
        linkSessionKey: String? = LINK_SESSION_KEY,
        linkSessionState: LinkController.SessionState = LinkController.SessionState.LoggedIn,
        block: suspend Scenario.() -> Unit,
    ) = runTest {
        val application = createApplication()
        val linkController = mock<LinkController>()
        val cryptoApiRepository = mock<CryptoApiRepository>()
        whenever(linkController.state(any())).thenReturn(
            MutableStateFlow(linkState(consumerSessionClientSecret, linkSessionKey, linkSessionState))
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
            verify(cryptoApiRepository, never()).fulfillKycRequirements(
                requirements = any(),
                linkSessionKey = any(),
            )
        }
    }

    private inline fun <reified T : Throwable> assertUnexpectedError(error: Throwable?): UnexpectedException {
        assertThat(error).isInstanceOf(UnexpectedException::class.java)
        return (error as UnexpectedException).also {
            assertThat(it.underlyingError).isInstanceOf(T::class.java)
        }
    }

    private fun createApplication(): Application {
        val application = mock<Application>()
        val runtimeApplication: Application = RuntimeEnvironment.getApplication()
        whenever(application.packageName).thenReturn(runtimeApplication.packageName)
        whenever(application.getString(R.string.stripe_onramp_default_api_error_user_message))
            .thenReturn("Something went wrong. Please try again later.")
        return application
    }

    private companion object {
        const val LINK_SESSION_KEY = "lsk_test_123"
        const val CONSUMER_SESSION_CLIENT_SECRET = "secret_123"
        fun documentRequests(fileIds: List<String>): List<KycDocumentSubmissionRequest> {
            return listOf(
                KycDocumentSubmissionRequest(
                    documentSubtype = "bank_statement",
                    fileIds = fileIds,
                )
            )
        }

        fun questionnaireRequest(): KycQuestionnaireSubmissionRequest {
            return KycQuestionnaireSubmissionRequest(
                answers = listOf(
                    KycQuestionnaireAnswerRequest(
                        questionId = "purchase_purpose",
                        value = "Long-term savings",
                    )
                )
            )
        }

        fun requirementRequests(
            documents: List<KycDocumentSubmissionRequest>,
            questionnaire: KycQuestionnaireSubmissionRequest,
        ): Map<String, KycRequirementSubmissionRequest> {
            return mapOf(
                "source_of_funds" to KycRequirementSubmissionRequest(
                    requestedBy = "swapped",
                    documents = documents,
                    collectionRequirements = KycCollectionSubmissionRequest(questionnaire),
                )
            )
        }

        fun documentSubmission(files: List<File>): KycSubmission {
            return KycSubmission(
                requirements = mapOf(
                    "source_of_funds" to KycRequirementSubmission(
                        requestedBy = "swapped",
                        documents = listOf(
                            KycDocumentSubmission(
                                documentSubtype = "bank_statement",
                                files = files,
                                uploadedFileIds = emptyList(),
                            )
                        ),
                        questionnaire = KycQuestionnaireSubmission(
                            answers = listOf(
                                KycQuestionnaireAnswer(
                                    questionId = "purchase_purpose",
                                    value = "Long-term savings",
                                )
                            )
                        ),
                    )
                ),
            )
        }

        fun questionnaireSubmission(): KycSubmission {
            return KycSubmission(
                requirements = documentSubmission(emptyList()).requirements.mapValues { (_, requirement) ->
                    requirement.copy(documents = emptyList())
                },
            )
        }

        fun linkState(
            consumerSessionClientSecret: String?,
            linkSessionKey: String?,
            linkSessionState: LinkController.SessionState,
        ): LinkController.State {
            return LinkController.State(
                internalLinkAccount = LinkController.LinkAccount(
                    email = "test@example.com",
                    redactedPhoneNumber = "***-***-1234",
                    sessionState = linkSessionState,
                    consumerSessionClientSecret = consumerSessionClientSecret,
                    linkSessionKey = linkSessionKey,
                ),
                merchantLogoUrl = null,
                selectedPaymentMethodPreview = null,
                createdPaymentMethod = null,
                elementsSessionId = null,
            )
        }
    }
}
