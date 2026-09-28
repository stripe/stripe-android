package com.stripe.android.crypto.onramp

import android.app.Application
import androidx.lifecycle.SavedStateHandle
import com.google.common.truth.Truth.assertThat
import com.stripe.android.core.model.StripeFile
import com.stripe.android.crypto.onramp.analytics.OnrampAnalyticsService
import com.stripe.android.crypto.onramp.exception.LinkAccountNotVerifiedException
import com.stripe.android.crypto.onramp.exception.MissingAdditionalKycFileIdException
import com.stripe.android.crypto.onramp.exception.MissingLinkSessionKeyException
import com.stripe.android.crypto.onramp.exception.OnrampErrorLogger
import com.stripe.android.crypto.onramp.exception.UnexpectedException
import com.stripe.android.crypto.onramp.model.AdditionalKycCollectionSubmissionRequest
import com.stripe.android.crypto.onramp.model.AdditionalKycDocumentSubmission
import com.stripe.android.crypto.onramp.model.AdditionalKycDocumentSubmissionRequest
import com.stripe.android.crypto.onramp.model.AdditionalKycQuestionnaireAnswer
import com.stripe.android.crypto.onramp.model.AdditionalKycQuestionnaireAnswerRequest
import com.stripe.android.crypto.onramp.model.AdditionalKycQuestionnaireSubmission
import com.stripe.android.crypto.onramp.model.AdditionalKycQuestionnaireSubmissionRequest
import com.stripe.android.crypto.onramp.model.AdditionalKycRequirementSubmission
import com.stripe.android.crypto.onramp.model.AdditionalKycRequirementSubmissionRequest
import com.stripe.android.crypto.onramp.model.AdditionalKycSubmission
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
class OnrampInteractorFulfillAdditionalKycRequirementTest {
    @Test
    fun `documents are uploaded in order before submission`() = runScenario {
        val expectedDocuments = documentRequests(fileIds = listOf("file_1", "file_2"))
        val expectedQuestionnaire = questionnaireRequest()
        whenever(cryptoApiRepository.uploadAdditionalKycDocument(firstFile, LINK_SESSION_KEY))
            .thenReturn(Result.success(StripeFile(id = "file_1")))
        whenever(cryptoApiRepository.uploadAdditionalKycDocument(secondFile, LINK_SESSION_KEY))
            .thenReturn(Result.success(StripeFile(id = "file_2")))
        whenever(
            cryptoApiRepository.fulfillAdditionalKycRequirement(
                requirements = requirementRequests(expectedDocuments, expectedQuestionnaire),
                linkSessionKey = LINK_SESSION_KEY,
            )
        ).thenReturn(Result.success(Unit))

        val result = interactor.fulfillAdditionalKycRequirement(
            documentSubmission(files = listOf(firstFile, secondFile))
        )

        assertThat(result.getOrThrow()).isEqualTo(Unit)
        inOrder(cryptoApiRepository) {
            verify(cryptoApiRepository).uploadAdditionalKycDocument(firstFile, LINK_SESSION_KEY)
            verify(cryptoApiRepository).uploadAdditionalKycDocument(secondFile, LINK_SESSION_KEY)
            verify(cryptoApiRepository).fulfillAdditionalKycRequirement(
                requirements = requirementRequests(expectedDocuments, expectedQuestionnaire),
                linkSessionKey = LINK_SESSION_KEY,
            )
        }
    }

    @Test
    fun `multiple requirements preserve their documents and questionnaire`() = runScenario {
        val sourceOfFunds = documentSubmission(listOf(secondFile)).requirements.getValue("source_of_funds")
        val submission = AdditionalKycSubmission(
            requirements = mapOf(
                "proof_of_address" to AdditionalKycRequirementSubmission(
                    requestedBy = "swapped",
                    documents = listOf(AdditionalKycDocumentSubmission("utility_provider", listOf(firstFile))),
                    questionnaire = null,
                ),
                "source_of_funds" to sourceOfFunds,
            )
        )
        val expectedRequirements = mapOf(
            "proof_of_address" to AdditionalKycRequirementSubmissionRequest(
                requestedBy = "swapped",
                documents = listOf(AdditionalKycDocumentSubmissionRequest("utility_provider", listOf("file_poa"))),
                additionalRequirements = null,
            ),
            "source_of_funds" to AdditionalKycRequirementSubmissionRequest(
                requestedBy = "swapped",
                documents = documentRequests(listOf("file_sof")),
                additionalRequirements = AdditionalKycCollectionSubmissionRequest(questionnaireRequest()),
            ),
        )
        whenever(cryptoApiRepository.uploadAdditionalKycDocument(firstFile, LINK_SESSION_KEY))
            .thenReturn(Result.success(StripeFile(id = "file_poa")))
        whenever(cryptoApiRepository.uploadAdditionalKycDocument(secondFile, LINK_SESSION_KEY))
            .thenReturn(Result.success(StripeFile(id = "file_sof")))
        whenever(cryptoApiRepository.fulfillAdditionalKycRequirement(expectedRequirements, LINK_SESSION_KEY))
            .thenReturn(Result.success(Unit))

        val result = interactor.fulfillAdditionalKycRequirement(submission)

        assertThat(result.getOrThrow()).isEqualTo(Unit)
        inOrder(cryptoApiRepository) {
            verify(cryptoApiRepository).uploadAdditionalKycDocument(firstFile, LINK_SESSION_KEY)
            verify(cryptoApiRepository).uploadAdditionalKycDocument(secondFile, LINK_SESSION_KEY)
            verify(cryptoApiRepository).fulfillAdditionalKycRequirement(expectedRequirements, LINK_SESSION_KEY)
        }
    }

    @Test
    fun `unverified Link account fails before uploading`() = runScenario(
        linkSessionState = LinkController.SessionState.NeedsVerification,
    ) {
        val result = interactor.fulfillAdditionalKycRequirement(
            documentSubmission(files = listOf(firstFile))
        )

        assertUnexpectedError<LinkAccountNotVerifiedException>(result.exceptionOrNull())
        verify(cryptoApiRepository, never()).uploadAdditionalKycDocument(any(), any())
        verifyFulfillmentWasNotRequested()
    }

    @Test
    fun `upload failure stops remaining uploads and submission`() = runScenario {
        val uploadError = IllegalStateException("Upload failed")
        whenever(cryptoApiRepository.uploadAdditionalKycDocument(firstFile, LINK_SESSION_KEY))
            .thenReturn(Result.failure(uploadError))

        val result = interactor.fulfillAdditionalKycRequirement(
            documentSubmission(files = listOf(firstFile, secondFile))
        )

        val error = assertUnexpectedError<IllegalStateException>(result.exceptionOrNull())
        assertThat(error.underlyingError).isSameInstanceAs(uploadError)
        verify(cryptoApiRepository, never()).uploadAdditionalKycDocument(secondFile, LINK_SESSION_KEY)
        verifyFulfillmentWasNotRequested()
    }

    @Test
    fun `uploaded file without an ID fails before submission`() = runScenario {
        whenever(cryptoApiRepository.uploadAdditionalKycDocument(firstFile, LINK_SESSION_KEY))
            .thenReturn(Result.success(StripeFile(id = null)))

        val result = interactor.fulfillAdditionalKycRequirement(
            documentSubmission(files = listOf(firstFile))
        )

        val error = assertUnexpectedError<MissingAdditionalKycFileIdException>(result.exceptionOrNull())
        assertThat(error.code).isEqualTo("unexpected_error")
        assertThat(error.userMessage).isEqualTo("Something went wrong. Please try again later.")
        assertThat(error.developerMessage).contains("Uploaded additional KYC document is missing a file ID")
        assertThat(error.developerMessage).contains("operation: fulfill_additional_kyc_requirement")
        assertThat(error.docUrl).isNull()
        verifyFulfillmentWasNotRequested()
    }

    @Test
    fun `submission failure is propagated`() = runScenario {
        val submissionError = IllegalStateException("Submission failed")
        val documents = documentRequests(fileIds = listOf("file_1"))
        val questionnaire = questionnaireRequest()
        whenever(cryptoApiRepository.uploadAdditionalKycDocument(firstFile, LINK_SESSION_KEY))
            .thenReturn(Result.success(StripeFile(id = "file_1")))
        whenever(
            cryptoApiRepository.fulfillAdditionalKycRequirement(
                requirements = requirementRequests(documents, questionnaire),
                linkSessionKey = LINK_SESSION_KEY,
            )
        ).thenReturn(Result.failure(submissionError))

        val result = interactor.fulfillAdditionalKycRequirement(
            documentSubmission(files = listOf(firstFile))
        )

        val error = assertUnexpectedError<IllegalStateException>(result.exceptionOrNull())
        assertThat(error.underlyingError).isSameInstanceAs(submissionError)
    }

    @Test
    fun `missing Link session key fails before uploading`() = runScenario(
        linkSessionKey = null,
    ) {
        val result = interactor.fulfillAdditionalKycRequirement(
            documentSubmission(files = listOf(firstFile))
        )

        assertUnexpectedError<MissingLinkSessionKeyException>(result.exceptionOrNull())
        verify(cryptoApiRepository, never()).uploadAdditionalKycDocument(any(), any())
        verifyFulfillmentWasNotRequested()
    }

    @Test
    fun `blank Link session key fails before uploading`() = runScenario(
        linkSessionKey = "   ",
    ) {
        val result = interactor.fulfillAdditionalKycRequirement(
            documentSubmission(files = listOf(firstFile))
        )

        assertUnexpectedError<MissingLinkSessionKeyException>(result.exceptionOrNull())
        verify(cryptoApiRepository, never()).uploadAdditionalKycDocument(any(), any())
        verifyFulfillmentWasNotRequested()
    }

    @Test
    fun `questionnaire submission requires a Link session key`() = runScenario(
        linkSessionKey = null,
    ) {
        val result = interactor.fulfillAdditionalKycRequirement(questionnaireSubmission())

        assertUnexpectedError<MissingLinkSessionKeyException>(result.exceptionOrNull())
        verify(cryptoApiRepository, never()).uploadAdditionalKycDocument(any(), any())
        verifyFulfillmentWasNotRequested()
    }

    @Test
    fun `questionnaire submission uses Link session key without a consumer secret`() = runScenario(
        consumerSessionClientSecret = null,
    ) {
        val requirements = requirementRequests(emptyList(), questionnaireRequest())
        whenever(cryptoApiRepository.fulfillAdditionalKycRequirement(requirements, LINK_SESSION_KEY))
            .thenReturn(Result.success(Unit))

        val result = interactor.fulfillAdditionalKycRequirement(questionnaireSubmission())

        assertThat(result.getOrThrow()).isEqualTo(Unit)
        verify(cryptoApiRepository).fulfillAdditionalKycRequirement(requirements, LINK_SESSION_KEY)
        verify(cryptoApiRepository, never()).uploadAdditionalKycDocument(any(), any())
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
            verify(cryptoApiRepository, never()).fulfillAdditionalKycRequirement(
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
        fun documentRequests(fileIds: List<String>): List<AdditionalKycDocumentSubmissionRequest> {
            return listOf(
                AdditionalKycDocumentSubmissionRequest(
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

        fun requirementRequests(
            documents: List<AdditionalKycDocumentSubmissionRequest>,
            questionnaire: AdditionalKycQuestionnaireSubmissionRequest,
        ): Map<String, AdditionalKycRequirementSubmissionRequest> {
            return mapOf(
                "source_of_funds" to AdditionalKycRequirementSubmissionRequest(
                    requestedBy = "swapped",
                    documents = documents,
                    additionalRequirements = AdditionalKycCollectionSubmissionRequest(questionnaire),
                )
            )
        }

        fun documentSubmission(files: List<File>): AdditionalKycSubmission {
            return AdditionalKycSubmission(
                requirements = mapOf(
                    "source_of_funds" to AdditionalKycRequirementSubmission(
                        requestedBy = "swapped",
                        documents = listOf(
                            AdditionalKycDocumentSubmission(
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
                ),
            )
        }

        fun questionnaireSubmission(): AdditionalKycSubmission {
            return AdditionalKycSubmission(
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
