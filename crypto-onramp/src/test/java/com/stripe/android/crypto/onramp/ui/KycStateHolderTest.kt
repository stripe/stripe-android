package com.stripe.android.crypto.onramp.ui

import com.google.common.truth.Truth.assertThat
import com.stripe.android.crypto.onramp.model.KycDocumentRequirement
import com.stripe.android.crypto.onramp.model.KycDocumentSubtype
import com.stripe.android.crypto.onramp.model.KycQuestion
import com.stripe.android.crypto.onramp.model.KycQuestionnaire
import com.stripe.android.crypto.onramp.model.KycRequirement
import com.stripe.android.crypto.onramp.model.KycRequirementError
import com.stripe.android.crypto.onramp.model.KycRequirements
import com.stripe.android.crypto.onramp.model.RetrieveKycRequirementsResponse
import kotlinx.serialization.json.Json
import org.junit.Test
import java.io.File

internal class KycStateHolderTest {
    @Test
    fun `proof of address fixture preserves server categories through submission`() {
        val stateHolder = stateHolderFromFixture("proof_of_address_required.json")
        stateHolder.onContinue()

        val document = requireNotNull(stateHolder.state.document)
        assertThat(document.maxFileSizeMegabytes).isEqualTo(50)
        assertThat(document.slots.single().subtypes.map { it.id }).containsExactly(
            "id_documents", "government_organization_documents", "utility_provider", "bank", "lease_agreement",
        ).inOrder()
        assertThat(document.fileRequirements).isEqualTo("PDF, JPEG/JPG, or PNG, up to 50 MB per file.")
        assertThat(document.instructions).contains("Documents must include your full name and address.")

        stateHolder.onDocumentSubtypeSelected(0, "utility_provider")
        stateHolder.onFileSelected(0, File("utility.pdf"), "utility.pdf", fileId = "file_uploaded")
        val submission = requireNotNull(stateHolder.startSubmission())

        assertThat(submission.requirements.values.single().requestedBy).isEqualTo("swapped")
        assertThat(submission.requirements.keys).containsExactly("proof_of_address")
        assertThat(submission.requirements.values.single().documents.single().documentSubtype)
            .isEqualTo("utility_provider")
    }

    @Test
    fun `proof of address accepts files up to the server 50 MB limit`() {
        val stateHolder = stateHolderFromFixture("proof_of_address_required.json")

        assertThat(stateHolder.isAcceptedFileSize(50_000_000L)).isTrue()
        assertThat(stateHolder.isAcceptedFileSize(50_000_001L)).isFalse()
        assertThat(stateHolder.state.validationError).isEqualTo(KycValidationError.FileTooLarge)
    }

    @Test
    fun `source of funds fixture supports Word documents and its questionnaire`() {
        val stateHolder = stateHolderFromFixture("source_of_funds_required.json")
        stateHolder.onContinue()

        assertThat(stateHolder.state.page).isEqualTo(KycCollectionPage.Questionnaire)
        stateHolder.onQuestionAnswerChanged("purchase_purpose", "Long-term investment")
        stateHolder.state.questions.filter { it.id != "purchase_purpose" }.forEach {
            stateHolder.onQuestionAnswerChanged(it.id, "Salary")
        }
        stateHolder.onContinue()
        stateHolder.onAddDocuments()
        assertThat(stateHolder.isAcceptedFile("payslip.docx", null)).isTrue()
        stateHolder.onFileSelected(0, File("payslip.docx"), "payslip.docx", fileId = "file_uploaded")

        val submission = requireNotNull(stateHolder.startSubmission())
        assertThat(submission.requirements.values.single().documents.single().documentSubtype).isEqualTo("payslip")
        val answer = submission.requirements.values.single().questionnaire?.answers?.first {
            it.questionId == "purchase_purpose"
        }
        assertThat(answer?.value).isEqualTo("Long-term investment")
    }

    private fun stateHolderFromFixture(fileName: String): KycStateHolder {
        val fixture = requireNotNull(
            javaClass.classLoader?.getResourceAsStream("kyc_requirements/$fileName")
        ).bufferedReader().use { it.readText() }
        val response = Json.decodeFromString<RetrieveKycRequirementsResponse>(fixture)
        return KycStateHolder(response.requirements.toKycRequirements())
    }

    @Test
    fun `proof of address advances from context to document editor`() {
        val stateHolder = KycStateHolder(
            requirements(
                userActionRequired = listOf(documentRequirement(minDocumentTypes = 1)),
                pendingPartnerAction = emptyList(),
                pendingStripeAction = emptyList(),
            )
        )

        assertThat(stateHolder.state.page).isEqualTo(KycCollectionPage.Context)

        assertThat(stateHolder.onContinue()).isTrue()

        assertThat(stateHolder.state.page).isEqualTo(KycCollectionPage.DocumentEditor)
        assertThat(stateHolder.state.document?.editingSlotIndex).isEqualTo(0)
    }

    @Test
    fun `source of funds advances through questionnaire overview and editor`() {
        val stateHolder = KycStateHolder(
            requirements(
                userActionRequired = listOf(
                    documentRequirement(minDocumentTypes = 1).copy(
                        description = "source_of_funds",
                        questionnaire = KycQuestionnaire(
                            questions = listOf(
                                question(id = "purchase_purpose"),
                                question(id = "third_party_advised"),
                                question(id = "funding_sources"),
                            )
                        ),
                    )
                ),
                pendingPartnerAction = emptyList(),
                pendingStripeAction = emptyList(),
            )
        )

        stateHolder.onContinue()
        assertThat(stateHolder.state.page).isEqualTo(KycCollectionPage.Questionnaire)

        stateHolder.onQuestionAnswerChanged("purchase_purpose", "For investment")
        stateHolder.onQuestionAnswerChanged("third_party_advised", "No")
        stateHolder.onQuestionAnswerChanged("funding_sources", "Salary")
        assertThat(stateHolder.onContinue()).isTrue()
        assertThat(stateHolder.state.page).isEqualTo(KycCollectionPage.DocumentOverview)

        stateHolder.onAddDocuments()
        assertThat(stateHolder.state.page).isEqualTo(KycCollectionPage.DocumentEditor)
    }

    @Test
    fun `initial state uses first requirement awaiting user action`() {
        val stateHolder = KycStateHolder(
            requirements(
                userActionRequired = listOf(documentRequirement(minDocumentTypes = 1)),
                pendingPartnerAction = listOf(questionnaireRequirement()),
                pendingStripeAction = emptyList(),
            )
        )

        assertThat(stateHolder.state.requirementType)
            .isEqualTo(KycRequirementType.ProofOfAddress)
        assertThat(stateHolder.state.document?.slots).hasSize(1)
        assertThat(stateHolder.state.document?.maxFileSizeMegabytes).isEqualTo(5)
        assertThat(stateHolder.state.page).isEqualTo(KycCollectionPage.Context)
        assertThat(stateHolder.state.errorMessages).containsExactly("The previous document was too old")
        assertThat(stateHolder.state.canSubmit).isFalse()
        assertThat(stateHolder.state.pendingRequirements).isEmpty()
    }

    @Test
    fun `submit validates required questionnaire answers`() {
        val stateHolder = KycStateHolder(
            requirements(
                userActionRequired = listOf(questionnaireRequirement()),
                pendingPartnerAction = emptyList(),
                pendingStripeAction = emptyList(),
            )
        )

        assertThat(stateHolder.createSubmission()).isNull()
        assertThat(stateHolder.state.validationError)
            .isEqualTo(KycValidationError.MissingRequiredAnswers)

        stateHolder.onQuestionAnswerChanged("purchase_purpose", "  Long-term investment  ")
        val submission = stateHolder.createSubmission()

        assertThat(submission?.requirements?.values?.single()?.requestedBy).isEqualTo("swapped")
        assertThat(submission?.requirements?.values?.single()?.questionnaire).isNotNull()
        assertThat(submission?.requirements?.values?.single()?.documents).isEmpty()
        assertThat(submission?.requirements?.values?.single()?.questionnaire?.answers).hasSize(1)
        assertThat(submission?.requirements?.values?.single()?.questionnaire?.answers?.single()?.questionId)
            .isEqualTo("purchase_purpose")
        assertThat(submission?.requirements?.values?.single()?.questionnaire?.answers?.single()?.value)
            .isEqualTo("Long-term investment")
    }

    @Test
    fun `document submission groups files by source and populates funding source answer`() {
        val stateHolder = KycStateHolder(
            requirements(
                userActionRequired = listOf(
                    documentRequirement(minDocumentTypes = 1).copy(
                        description = "source_of_funds",
                        questionnaire = KycQuestionnaire(
                            questions = listOf(
                                KycQuestion(
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

        stateHolder.onQuestionAnswerChanged("funding_sources", "Savings and salary")
        stateHolder.onFileSelected(
            slotIndex = 0,
            file = File("/tmp/bank.pdf"),
            displayName = "bank.pdf",
            fileId = "file_uploaded",
        )
        assertThat(stateHolder.isAcceptedFile("income.jpg", null)).isTrue()
        stateHolder.onFileSelected(
            slotIndex = 1,
            file = File("/tmp/income.jpg"),
            displayName = "income.jpg",
            fileId = "file_uploaded",
        )

        val submission = stateHolder.createSubmission()

        assertThat(submission?.requirements?.values?.single()?.documents).isNotEmpty()
        assertThat(submission?.requirements?.keys)
            .containsExactly("source_of_funds")
        assertThat(submission?.requirements?.values?.single()?.documents?.map { document -> document.documentSubtype })
            .containsExactly("bank_statement")
        val fileIds = submission?.requirements?.values?.single()?.documents?.flatMap { it.uploadedFileIds }
        assertThat(fileIds)
            .containsExactly("file_uploaded", "file_uploaded")
            .inOrder()
        assertThat(submission?.requirements?.values?.single()?.questionnaire?.answers?.single()?.value)
            .isEqualTo("Savings and salary")
    }

    @Test
    fun `unsupported file type is rejected`() {
        val stateHolder = KycStateHolder(
            requirements(
                userActionRequired = listOf(documentRequirement(minDocumentTypes = 1)),
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
            .isEqualTo(KycValidationError.UnsupportedFileType)
        assertThat(stateHolder.state.validationFileName).isEqualTo("malware.exe")
    }

    @Test
    fun `uploading state retains selected file name`() {
        val stateHolder = KycStateHolder(
            requirements(
                userActionRequired = listOf(documentRequirement(minDocumentTypes = 1)),
                pendingPartnerAction = emptyList(),
                pendingStripeAction = emptyList(),
            )
        )
        stateHolder.onFileSelectionStarted(slotIndex = 0)

        stateHolder.onFileUploadStarted(slotIndex = 0, displayName = "electricity-bill.pdf")

        assertThat(stateHolder.state.selectingFileSlot).isEqualTo(0)
        assertThat(stateHolder.state.selectingFileName).isEqualTo("electricity-bill.pdf")
    }

    @Test
    fun `proof of address file at size limit is accepted`() {
        val stateHolder = KycStateHolder(
            requirements(
                userActionRequired = listOf(documentRequirement(minDocumentTypes = 1)),
                pendingPartnerAction = emptyList(),
                pendingStripeAction = emptyList(),
            )
        )
        stateHolder.onFileSelectionStarted(slotIndex = 0)

        val accepted = stateHolder.isAcceptedFileSize(fileSizeBytes = 5_000_000L)

        assertThat(accepted).isTrue()
        assertThat(stateHolder.state.validationError).isNull()
    }

    @Test
    fun `oversized proof of address file is rejected`() {
        val stateHolder = KycStateHolder(
            requirements(
                userActionRequired = listOf(documentRequirement(minDocumentTypes = 1)),
                pendingPartnerAction = emptyList(),
                pendingStripeAction = emptyList(),
            )
        )
        stateHolder.onFileSelectionStarted(slotIndex = 0)

        val accepted = stateHolder.isAcceptedFileSize(fileSizeBytes = 5_000_001L)

        assertThat(accepted).isFalse()
        assertThat(stateHolder.state.selectingFileSlot).isNull()
        assertThat(stateHolder.state.validationError)
            .isEqualTo(KycValidationError.FileTooLarge)
    }

    @Test
    fun `oversized source of funds file is rejected`() {
        val stateHolder = KycStateHolder(
            requirements(
                userActionRequired = listOf(
                    documentRequirement(minDocumentTypes = 1).copy(description = "source_of_funds")
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
            .isEqualTo(KycValidationError.FileTooLarge)
    }

    @Test
    fun `unrecognized document requirement uses server file size limit`() {
        val stateHolder = KycStateHolder(
            requirements(
                userActionRequired = listOf(
                    documentRequirement(minDocumentTypes = 1).copy(description = "future_requirement")
                ),
                pendingPartnerAction = emptyList(),
                pendingStripeAction = emptyList(),
            )
        )

        val accepted = stateHolder.isAcceptedFileSize(fileSizeBytes = Long.MAX_VALUE)

        assertThat(accepted).isFalse()
        assertThat(stateHolder.maximumFileSizeBytes).isEqualTo(5_000_000L)
        assertThat(stateHolder.state.document?.maxFileSizeMegabytes).isEqualTo(5)
    }

    @Test
    fun `partner requirement produces waiting for review state`() {
        val stateHolder = KycStateHolder(
            requirements(
                userActionRequired = emptyList(),
                pendingPartnerAction = listOf(
                    documentRequirement(minDocumentTypes = 1).copy(awaitingActionFrom = "partner")
                ),
                pendingStripeAction = emptyList(),
            )
        )

        assertThat(stateHolder.state.isCollectionAvailable).isFalse()
        assertThat(stateHolder.state.canSubmit).isFalse()
        assertThat(stateHolder.state.pendingRequirements.single().requirementType)
            .isEqualTo(KycRequirementType.ProofOfAddress)
        assertThat(stateHolder.state.pendingRequirements.single().status)
            .isEqualTo(KycPendingRequirementStatus.WaitingForReview)
        assertThat(stateHolder.createSubmission()).isNull()
    }

    @Test
    fun `Stripe requirement produces processing state`() {
        val stateHolder = KycStateHolder(
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
            .isEqualTo(KycRequirementType.SourceOfFunds)
        assertThat(stateHolder.state.pendingRequirements.single().status)
            .isEqualTo(KycPendingRequirementStatus.Processing)
    }

    @Test
    fun `partner and Stripe requirements are both represented`() {
        val stateHolder = KycStateHolder(
            requirements(
                userActionRequired = emptyList(),
                pendingPartnerAction = listOf(
                    documentRequirement(minDocumentTypes = 1).copy(awaitingActionFrom = "partner")
                ),
                pendingStripeAction = listOf(
                    questionnaireRequirement().copy(awaitingActionFrom = "stripe")
                ),
            )
        )

        assertThat(stateHolder.state.pendingRequirements.map { requirement -> requirement.status })
            .containsExactly(
                KycPendingRequirementStatus.WaitingForReview,
                KycPendingRequirementStatus.Processing,
            )
            .inOrder()
    }

    @Test
    fun `no recognized requirement produces unavailable state`() {
        val stateHolder = KycStateHolder(
            requirements(
                userActionRequired = emptyList(),
                pendingPartnerAction = emptyList(),
                pendingStripeAction = emptyList(),
            )
        )

        assertThat(stateHolder.state.isCollectionAvailable).isFalse()
        assertThat(stateHolder.state.pendingRequirements).isEmpty()
        assertThat(stateHolder.state.page).isEqualTo(KycCollectionPage.Unavailable)
        assertThat(stateHolder.createSubmission()).isNull()
    }

    @Test
    fun `submission failure retains answers and allows retry`() {
        val stateHolder = KycStateHolder(
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
            .isEqualTo(KycSubmissionState.Submitting)
        assertThat(stateHolder.state.canSubmit).isFalse()

        stateHolder.onSubmissionFailed()

        assertThat(stateHolder.state.submissionState)
            .isEqualTo(KycSubmissionState.Failed)
        assertThat(stateHolder.state.questions.single().answer).isEqualTo("Long-term investment")
        assertThat(stateHolder.state.canSubmit).isTrue()
        assertThat(stateHolder.startSubmission()).isNotNull()
    }

    @Test
    fun `successful submission advances through all user requirements`() {
        val stateHolder = KycStateHolder(
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
            .isEqualTo(KycSubmissionState.Submitted)
        assertThat(stateHolder.state.currentRequirement).isEqualTo(1)
        assertThat(stateHolder.state.totalRequirements).isEqualTo(2)
        assertThat(stateHolder.state.hasMoreRequirements).isTrue()

        assertThat(stateHolder.advanceToNextRequirement()).isTrue()
        assertThat(stateHolder.state.submissionState)
            .isEqualTo(KycSubmissionState.Collecting)
        assertThat(stateHolder.state.currentRequirement).isEqualTo(2)
        assertThat(stateHolder.state.questions.single().id).isEqualTo("funding_sources")
        assertThat(stateHolder.state.questions.single().answer).isEmpty()

        stateHolder.onQuestionAnswerChanged("funding_sources", "Salary")
        stateHolder.startSubmission()
        stateHolder.onSubmissionSucceeded()

        assertThat(stateHolder.state.hasMoreRequirements).isFalse()
        assertThat(stateHolder.advanceToNextRequirement()).isFalse()
    }

    @Test
    fun `multiple files of one type do not satisfy minimum distinct types`() = runDocumentScenario(
        minDocumentTypes = 2,
    ) {
        onFileSelected(0, File("/tmp/bank.pdf"), "bank.pdf", fileId = "file_uploaded")
        onFileSelected(1, File("/tmp/bank-2.pdf"), "bank-2.pdf", fileId = "file_uploaded")

        assertThat(createSubmission()).isNull()
        assertThat(state.validationError).isEqualTo(KycValidationError.MissingDocuments)

        onDocumentSubtypeSelected(1, "payslip")

        assertThat(createSubmission()?.requirements?.values?.single()?.documents?.map { it.documentSubtype })
            .containsExactly("bank_statement", "payslip")
    }

    @Test
    fun `maximum distinct types disables new types but permits more files of existing type`() = runDocumentScenario(
        maxDocumentTypes = 1,
    ) {
        onFileSelected(0, File("/tmp/bank.pdf"), "bank.pdf", fileId = "file_uploaded")

        val slot = requireNotNull(state.document).slots.first { it.index == 1 }
        assertThat(slot.subtypes.first { it.id == "payslip" }.isEnabled).isFalse()
        assertThat(slot.subtypes.first { it.id == "bank_statement" }.isEnabled).isTrue()
        onDocumentSubtypeSelected(1, "payslip")
        onFileSelected(1, File("/tmp/bank-2.pdf"), "bank-2.pdf", fileId = "file_uploaded")

        val documents = requireNotNull(createSubmission()).requirements.values.single().documents
        assertThat(documents.single().documentSubtype).isEqualTo("bank_statement")
        assertThat(documents.single().uploadedFileIds).hasSize(2)
    }

    @Test
    fun `server file size limit is used at byte boundary`() = runDocumentScenario(
        maxFileSizeBytes = 2_000_000L,
    ) {
        assertThat(maximumFileSizeBytes).isEqualTo(2_000_000L)
        assertThat(isAcceptedFileSize(2_000_000L)).isTrue()
        assertThat(isAcceptedFileSize(2_000_001L)).isFalse()
        assertThat(state.document?.maxFileSizeMegabytes).isEqualTo(2)
    }

    @Test
    fun `source of funds without questions skips questionnaire and omits answers`() = runDocumentScenario {
        assertThat(onContinue()).isTrue()
        assertThat(state.page).isEqualTo(KycCollectionPage.DocumentOverview)

        onAddDocuments()
        val slot = requireNotNull(state.document?.editingSlotIndex)
        onFileSelected(slot, File("salary.pdf"), "salary.pdf", fileId = "file_salary")

        val requirement = requireNotNull(createSubmission()).requirements.getValue("source_of_funds")
        assertThat(requirement.questionnaire).isNull()
        assertThat(requirement.documents.single().uploadedFileIds).containsExactly("file_salary")
    }

    @Test
    fun `per source limit blocks additional uploads and removal restores capacity`() = runDocumentScenario(
        maxFilesPerDocumentType = 1,
    ) {
        onFileSelected(0, File("first.pdf"), "first.pdf", "file_first")
        assertThat(canSelectFile(1)).isFalse()
        onFileSelectionStarted(1)
        assertThat(state.selectingFileSlot).isNull()
        val rejected = File("second.pdf")
        assertThat(onFileSelected(1, rejected, "second.pdf", "file_second")).isEqualTo(rejected)
        assertThat(createSubmission()?.requirements?.values?.single()?.documents?.single()?.uploadedFileIds)
            .containsExactly("file_first")
        onFileRemoved(0)
        assertThat(canSelectFile(1)).isTrue()
    }

    @Test
    fun `file allowance is independent for each source`() = runDocumentScenario(
        maxFilesPerDocumentType = 1,
    ) {
        onFileSelected(0, File("bank.pdf"), "bank.pdf", "file_bank")
        onDocumentSubtypeSelected(1, "payslip")
        assertThat(canSelectFile(1)).isTrue()
        onFileSelected(1, File("salary.pdf"), "salary.pdf", "file_salary")
        assertThat(createSubmission()?.requirements?.values?.single()?.documents).hasSize(2)
    }

    private fun runDocumentScenario(
        minDocumentTypes: Int = 1,
        maxDocumentTypes: Int = 2,
        maxFileSizeBytes: Long = 5_000_000L,
        maxFilesPerDocumentType: Int = 10,
        block: KycStateHolder.() -> Unit,
    ) {
        val requirement = documentRequirement(minDocumentTypes)
        KycStateHolder(
            requirements(
                userActionRequired = listOf(
                    requirement.copy(
                        description = "source_of_funds",
                        document = requireNotNull(requirement.document).copy(
                            maxDocumentTypes = maxDocumentTypes,
                            maxFilesPerDocumentType = maxFilesPerDocumentType,
                            maxFileSizeBytes = maxFileSizeBytes,
                        ),
                    )
                ),
                pendingPartnerAction = emptyList(),
                pendingStripeAction = emptyList(),
            )
        ).block()
    }

    private companion object {
        fun question(id: String): KycQuestion {
            return KycQuestion(
                id = id,
                prompt = "Question $id",
                answerType = "free_text",
                required = true,
            )
        }

        fun requirements(
            userActionRequired: List<KycRequirement>,
            pendingPartnerAction: List<KycRequirement>,
            pendingStripeAction: List<KycRequirement>,
        ): KycRequirements {
            return KycRequirements(
                userActionRequired = userActionRequired,
                pendingPartnerAction = pendingPartnerAction,
                pendingStripeAction = pendingStripeAction,
                unrecognizedActionOwner = emptyList(),
            )
        }

        fun questionnaireRequirement(): KycRequirement {
            return KycRequirement(
                description = "source_of_funds",
                requestedBy = "swapped",
                awaitingActionFrom = "user",
                errors = emptyList(),
                document = null,
                questionnaire = KycQuestionnaire(
                    questions = listOf(
                        KycQuestion(
                            id = "purchase_purpose",
                            prompt = "Why are you purchasing cryptocurrency?",
                            answerType = "free_text",
                            required = true,
                        )
                    )
                ),
            )
        }

        fun secondQuestionnaireRequirement(): KycRequirement {
            return questionnaireRequirement().copy(
                description = "source_of_funds_questions",
                questionnaire = KycQuestionnaire(
                    questions = listOf(
                        KycQuestion(
                            id = "funding_sources",
                            prompt = "How are you funding your transactions?",
                            answerType = "free_text",
                            required = true,
                        )
                    )
                ),
            )
        }

        fun documentRequirement(minDocumentTypes: Int): KycRequirement {
            return KycRequirement(
                description = "proof_of_address",
                requestedBy = "swapped",
                awaitingActionFrom = "user",
                errors = listOf(
                    KycRequirementError(
                        code = "document_too_old",
                        developerMessage = "The previous document was too old",
                    )
                ),
                document = KycDocumentRequirement(
                    acceptedSubtypes = listOf(
                        KycDocumentSubtype(
                            id = "bank_statement",
                            label = "Bank statement",
                            description = "Statements from your bank",
                        ),
                        KycDocumentSubtype(
                            id = "payslip",
                            label = "Payslip",
                            description = "Recent payslips",
                        ),
                    ),
                    acceptedFormats = listOf("pdf", "jpeg", "png"),
                    minDocumentTypes = minDocumentTypes,
                    maxDocumentTypes = 2,
                    maxFilesPerDocumentType = 10,
                    maxFileSizeBytes = 5_000_000L,
                    fileRequirements = "PDF, JPEG, or PNG, up to 5 MB per file.",
                    instructions = listOf("Show your full name and address"),
                ),
                questionnaire = null,
            )
        }
    }
}
