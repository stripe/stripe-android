package com.stripe.android.crypto.onramp.ui

import com.google.common.truth.Truth.assertThat
import com.stripe.android.crypto.onramp.model.AdditionalKycDocumentRequirement
import com.stripe.android.crypto.onramp.model.AdditionalKycDocumentSubtype
import com.stripe.android.crypto.onramp.model.AdditionalKycQuestion
import com.stripe.android.crypto.onramp.model.AdditionalKycQuestionnaire
import com.stripe.android.crypto.onramp.model.AdditionalKycRequirement
import com.stripe.android.crypto.onramp.model.AdditionalKycRequirementError
import com.stripe.android.crypto.onramp.model.AdditionalKycRequirements
import com.stripe.android.link.onramp.ui.AdditionalKycPendingRequirementStatus
import com.stripe.android.link.onramp.ui.AdditionalKycRequirementType
import com.stripe.android.link.onramp.ui.AdditionalKycSubmissionState
import com.stripe.android.link.onramp.ui.AdditionalKycValidationError
import org.junit.Test
import java.io.File

internal class AdditionalKycStateHolderTest {
    @Test
    fun `initial state uses first requirement awaiting user action`() {
        val stateHolder = AdditionalKycStateHolder(
            requirements(
                userActionRequired = listOf(documentRequirement(minDocuments = 1)),
                pendingPartnerAction = listOf(questionnaireRequirement()),
                pendingStripeAction = emptyList(),
            )
        )

        assertThat(stateHolder.state.requirementType)
            .isEqualTo(AdditionalKycRequirementType.ProofOfAddress)
        assertThat(stateHolder.state.document?.slots).hasSize(1)
        assertThat(stateHolder.state.document?.maxFileSizeMegabytes).isEqualTo(50)
        assertThat(stateHolder.state.errorMessages).containsExactly("The previous document was too old")
        assertThat(stateHolder.state.canSubmit).isFalse()
        assertThat(stateHolder.state.pendingRequirements).isEmpty()
    }

    @Test
    fun `submit validates required questionnaire answers`() {
        val stateHolder = AdditionalKycStateHolder(
            requirements(
                userActionRequired = listOf(questionnaireRequirement()),
                pendingPartnerAction = emptyList(),
                pendingStripeAction = emptyList(),
            )
        )

        assertThat(stateHolder.createSubmission()).isNull()
        assertThat(stateHolder.state.validationError)
            .isEqualTo(AdditionalKycValidationError.MissingRequiredAnswers)

        stateHolder.onQuestionAnswerChanged("purchase_purpose", "  Long-term investment  ")
        val submission = stateHolder.createSubmission()

        assertThat(submission?.liquidityProvider).isEqualTo("swapped")
        assertThat(submission?.submissionType).isEqualTo("questionnaire")
        assertThat(submission?.documents).isNull()
        assertThat(submission?.questionnaire?.answers).hasSize(1)
        assertThat(submission?.questionnaire?.answers?.single()?.questionId)
            .isEqualTo("purchase_purpose")
        assertThat(submission?.questionnaire?.answers?.single()?.value)
            .isEqualTo("Long-term investment")
    }

    @Test
    fun `document submission collects required unique subtypes files and questionnaire`() {
        val stateHolder = AdditionalKycStateHolder(
            requirements(
                userActionRequired = listOf(
                    documentRequirement(minDocuments = 2).copy(
                        description = "source_of_funds",
                        questionnaire = AdditionalKycQuestionnaire(
                            questions = listOf(
                                AdditionalKycQuestion(
                                    id = "funding_sources",
                                    prompt = "How are you funding your transactions?",
                                    answerType = "free_text",
                                    required = true,
                                )
                            )
                        ),
                    )
                ),
                pendingPartnerAction = emptyList(),
                pendingStripeAction = emptyList(),
            )
        )

        stateHolder.onDocumentSubtypeSelected(slotIndex = 0, subtypeId = "bank_statement")

        val bankStatementInSecondSlot = stateHolder.state.document
            ?.slots
            ?.get(1)
            ?.subtypes
            ?.single { subtype -> subtype.id == "bank_statement" }
        assertThat(bankStatementInSecondSlot?.isEnabled).isFalse()

        stateHolder.onDocumentSubtypeSelected(slotIndex = 1, subtypeId = "bank_statement")
        assertThat(stateHolder.state.validationError)
            .isEqualTo(AdditionalKycValidationError.DuplicateDocumentType)

        stateHolder.onDocumentSubtypeSelected(slotIndex = 1, subtypeId = "payslip")
        stateHolder.onFileSelected(
            slotIndex = 0,
            file = File("/tmp/bank.pdf"),
            displayName = "bank.pdf",
        )
        assertThat(stateHolder.isAcceptedFile("income.jpg", null)).isTrue()
        stateHolder.onFileSelected(
            slotIndex = 1,
            file = File("/tmp/income.jpg"),
            displayName = "income.jpg",
        )
        stateHolder.onQuestionAnswerChanged("funding_sources", "Salary and savings")

        val submission = stateHolder.createSubmission()

        assertThat(submission?.submissionType).isEqualTo("document")
        assertThat(submission?.documents?.map { document -> document.documentType })
            .containsExactly("source_of_funds", "source_of_funds")
        assertThat(submission?.documents?.map { document -> document.documentSubtype })
            .containsExactly("bank_statement", "payslip")
            .inOrder()
        assertThat(submission?.documents?.flatMap { document -> document.files })
            .containsExactly(File("/tmp/bank.pdf"), File("/tmp/income.jpg"))
            .inOrder()
        assertThat(submission?.questionnaire?.answers?.single()?.value)
            .isEqualTo("Salary and savings")
    }

    @Test
    fun `unsupported file type is rejected`() {
        val stateHolder = AdditionalKycStateHolder(
            requirements(
                userActionRequired = listOf(documentRequirement(minDocuments = 1)),
                pendingPartnerAction = emptyList(),
                pendingStripeAction = emptyList(),
            )
        )
        stateHolder.onFileSelectionStarted(slotIndex = 0)

        val accepted = stateHolder.isAcceptedFile(
            displayName = "malware.exe",
            mimeTypeExtension = "exe",
        )

        assertThat(accepted).isFalse()
        assertThat(stateHolder.state.selectingFileSlot).isNull()
        assertThat(stateHolder.state.validationError)
            .isEqualTo(AdditionalKycValidationError.UnsupportedFileType)
    }

    @Test
    fun `proof of address file at size limit is accepted`() {
        val stateHolder = AdditionalKycStateHolder(
            requirements(
                userActionRequired = listOf(documentRequirement(minDocuments = 1)),
                pendingPartnerAction = emptyList(),
                pendingStripeAction = emptyList(),
            )
        )
        stateHolder.onFileSelectionStarted(slotIndex = 0)

        val accepted = stateHolder.isAcceptedFileSize(fileSizeBytes = 50_000_000L)

        assertThat(accepted).isTrue()
        assertThat(stateHolder.state.validationError).isNull()
    }

    @Test
    fun `oversized proof of address file is rejected`() {
        val stateHolder = AdditionalKycStateHolder(
            requirements(
                userActionRequired = listOf(documentRequirement(minDocuments = 1)),
                pendingPartnerAction = emptyList(),
                pendingStripeAction = emptyList(),
            )
        )
        stateHolder.onFileSelectionStarted(slotIndex = 0)

        val accepted = stateHolder.isAcceptedFileSize(fileSizeBytes = 50_000_001L)

        assertThat(accepted).isFalse()
        assertThat(stateHolder.state.selectingFileSlot).isNull()
        assertThat(stateHolder.state.validationError)
            .isEqualTo(AdditionalKycValidationError.FileTooLarge)
    }

    @Test
    fun `oversized source of funds file is rejected`() {
        val stateHolder = AdditionalKycStateHolder(
            requirements(
                userActionRequired = listOf(
                    documentRequirement(minDocuments = 1).copy(description = "source_of_funds")
                ),
                pendingPartnerAction = emptyList(),
                pendingStripeAction = emptyList(),
            )
        )
        stateHolder.onFileSelectionStarted(slotIndex = 0)

        val accepted = stateHolder.isAcceptedFileSize(fileSizeBytes = 5_000_001L)

        assertThat(accepted).isFalse()
        assertThat(stateHolder.state.document?.maxFileSizeMegabytes).isEqualTo(5)
        assertThat(stateHolder.state.validationError)
            .isEqualTo(AdditionalKycValidationError.FileTooLarge)
    }

    @Test
    fun `unrecognized document requirement has no client file size limit`() {
        val stateHolder = AdditionalKycStateHolder(
            requirements(
                userActionRequired = listOf(
                    documentRequirement(minDocuments = 1).copy(description = "future_requirement")
                ),
                pendingPartnerAction = emptyList(),
                pendingStripeAction = emptyList(),
            )
        )

        val accepted = stateHolder.isAcceptedFileSize(fileSizeBytes = Long.MAX_VALUE)

        assertThat(accepted).isTrue()
        assertThat(stateHolder.maximumFileSizeBytes).isNull()
        assertThat(stateHolder.state.document?.maxFileSizeMegabytes).isNull()
    }

    @Test
    fun `partner requirement produces waiting for review state`() {
        val stateHolder = AdditionalKycStateHolder(
            requirements(
                userActionRequired = emptyList(),
                pendingPartnerAction = listOf(
                    documentRequirement(minDocuments = 1).copy(awaitingActionFrom = "partner")
                ),
                pendingStripeAction = emptyList(),
            )
        )

        assertThat(stateHolder.state.isCollectionAvailable).isFalse()
        assertThat(stateHolder.state.canSubmit).isFalse()
        assertThat(stateHolder.state.pendingRequirements.single().requirementType)
            .isEqualTo(AdditionalKycRequirementType.ProofOfAddress)
        assertThat(stateHolder.state.pendingRequirements.single().status)
            .isEqualTo(AdditionalKycPendingRequirementStatus.WaitingForReview)
        assertThat(stateHolder.createSubmission()).isNull()
    }

    @Test
    fun `Stripe requirement produces processing state`() {
        val stateHolder = AdditionalKycStateHolder(
            requirements(
                userActionRequired = emptyList(),
                pendingPartnerAction = emptyList(),
                pendingStripeAction = listOf(
                    questionnaireRequirement().copy(awaitingActionFrom = "stripe")
                ),
            )
        )

        assertThat(stateHolder.state.isCollectionAvailable).isFalse()
        assertThat(stateHolder.state.pendingRequirements.single().requirementType)
            .isEqualTo(AdditionalKycRequirementType.AdditionalVerification)
        assertThat(stateHolder.state.pendingRequirements.single().status)
            .isEqualTo(AdditionalKycPendingRequirementStatus.Processing)
    }

    @Test
    fun `partner and Stripe requirements are both represented`() {
        val stateHolder = AdditionalKycStateHolder(
            requirements(
                userActionRequired = emptyList(),
                pendingPartnerAction = listOf(
                    documentRequirement(minDocuments = 1).copy(awaitingActionFrom = "partner")
                ),
                pendingStripeAction = listOf(
                    questionnaireRequirement().copy(awaitingActionFrom = "stripe")
                ),
            )
        )

        assertThat(stateHolder.state.pendingRequirements.map { requirement -> requirement.status })
            .containsExactly(
                AdditionalKycPendingRequirementStatus.WaitingForReview,
                AdditionalKycPendingRequirementStatus.Processing,
            )
            .inOrder()
    }

    @Test
    fun `no recognized requirement produces unavailable state`() {
        val stateHolder = AdditionalKycStateHolder(
            requirements(
                userActionRequired = emptyList(),
                pendingPartnerAction = emptyList(),
                pendingStripeAction = emptyList(),
            )
        )

        assertThat(stateHolder.state.isCollectionAvailable).isFalse()
        assertThat(stateHolder.state.pendingRequirements).isEmpty()
        assertThat(stateHolder.createSubmission()).isNull()
    }

    @Test
    fun `submission failure retains answers and allows retry`() {
        val stateHolder = AdditionalKycStateHolder(
            requirements(
                userActionRequired = listOf(questionnaireRequirement()),
                pendingPartnerAction = emptyList(),
                pendingStripeAction = emptyList(),
            )
        )
        stateHolder.onQuestionAnswerChanged("purchase_purpose", "Long-term investment")

        val firstSubmission = stateHolder.startSubmission()
        assertThat(firstSubmission).isNotNull()
        assertThat(stateHolder.state.submissionState)
            .isEqualTo(AdditionalKycSubmissionState.Submitting)
        assertThat(stateHolder.state.canSubmit).isFalse()

        stateHolder.onSubmissionFailed()

        assertThat(stateHolder.state.submissionState)
            .isEqualTo(AdditionalKycSubmissionState.Failed)
        assertThat(stateHolder.state.questions.single().answer).isEqualTo("Long-term investment")
        assertThat(stateHolder.state.canSubmit).isTrue()
        assertThat(stateHolder.startSubmission()).isNotNull()
    }

    @Test
    fun `successful submission advances through all user requirements`() {
        val stateHolder = AdditionalKycStateHolder(
            requirements(
                userActionRequired = listOf(
                    questionnaireRequirement(),
                    secondQuestionnaireRequirement(),
                ),
                pendingPartnerAction = emptyList(),
                pendingStripeAction = emptyList(),
            )
        )
        stateHolder.onQuestionAnswerChanged("purchase_purpose", "Long-term investment")
        stateHolder.startSubmission()
        stateHolder.onSubmissionSucceeded()

        assertThat(stateHolder.state.submissionState)
            .isEqualTo(AdditionalKycSubmissionState.Submitted)
        assertThat(stateHolder.state.currentRequirement).isEqualTo(1)
        assertThat(stateHolder.state.totalRequirements).isEqualTo(2)
        assertThat(stateHolder.state.hasMoreRequirements).isTrue()

        assertThat(stateHolder.advanceToNextRequirement()).isTrue()
        assertThat(stateHolder.state.submissionState)
            .isEqualTo(AdditionalKycSubmissionState.Collecting)
        assertThat(stateHolder.state.currentRequirement).isEqualTo(2)
        assertThat(stateHolder.state.questions.single().id).isEqualTo("funding_sources")
        assertThat(stateHolder.state.questions.single().answer).isEmpty()

        stateHolder.onQuestionAnswerChanged("funding_sources", "Salary")
        stateHolder.startSubmission()
        stateHolder.onSubmissionSucceeded()

        assertThat(stateHolder.state.hasMoreRequirements).isFalse()
        assertThat(stateHolder.advanceToNextRequirement()).isFalse()
    }

    private companion object {
        fun requirements(
            userActionRequired: List<AdditionalKycRequirement>,
            pendingPartnerAction: List<AdditionalKycRequirement>,
            pendingStripeAction: List<AdditionalKycRequirement>,
        ): AdditionalKycRequirements {
            return AdditionalKycRequirements(
                userActionRequired = userActionRequired,
                pendingPartnerAction = pendingPartnerAction,
                pendingStripeAction = pendingStripeAction,
                unrecognizedActionOwner = emptyList(),
            )
        }

        fun questionnaireRequirement(): AdditionalKycRequirement {
            return AdditionalKycRequirement(
                description = "screening_questions",
                requestedBy = "swapped",
                awaitingActionFrom = "user",
                requestedReasons = listOf("kyc_step_up"),
                errors = emptyList(),
                submissionType = "questionnaire",
                document = null,
                questionnaire = AdditionalKycQuestionnaire(
                    questions = listOf(
                        AdditionalKycQuestion(
                            id = "purchase_purpose",
                            prompt = "Why are you purchasing cryptocurrency?",
                            answerType = "free_text",
                            required = true,
                        )
                    )
                ),
            )
        }

        fun secondQuestionnaireRequirement(): AdditionalKycRequirement {
            return questionnaireRequirement().copy(
                description = "source_of_funds_questions",
                questionnaire = AdditionalKycQuestionnaire(
                    questions = listOf(
                        AdditionalKycQuestion(
                            id = "funding_sources",
                            prompt = "How are you funding your transactions?",
                            answerType = "free_text",
                            required = true,
                        )
                    )
                ),
            )
        }

        fun documentRequirement(minDocuments: Int): AdditionalKycRequirement {
            return AdditionalKycRequirement(
                description = "proof_of_address",
                requestedBy = "swapped",
                awaitingActionFrom = "user",
                requestedReasons = listOf("kyc_step_up"),
                errors = listOf(
                    AdditionalKycRequirementError(
                        code = "document_too_old",
                        message = "The previous document was too old",
                    )
                ),
                submissionType = "document",
                document = AdditionalKycDocumentRequirement(
                    acceptedSubtypes = listOf(
                        AdditionalKycDocumentSubtype(
                            id = "bank_statement",
                            label = "Bank statement",
                        ),
                        AdditionalKycDocumentSubtype(
                            id = "payslip",
                            label = "Payslip",
                        ),
                    ),
                    acceptedFormats = listOf("pdf", "jpeg", "png"),
                    minDocuments = minDocuments,
                    instructions = listOf("Show your full name and address"),
                ),
                questionnaire = null,
            )
        }
    }
}
