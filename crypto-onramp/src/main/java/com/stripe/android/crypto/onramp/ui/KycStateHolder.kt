package com.stripe.android.crypto.onramp.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.stripe.android.crypto.onramp.model.KycRequirement
import com.stripe.android.crypto.onramp.model.KycRequirementSubmission
import com.stripe.android.crypto.onramp.model.KycRequirements
import com.stripe.android.crypto.onramp.model.KycSubmission
import java.io.File

@Suppress("TooManyFunctions")
internal class KycStateHolder(
    requirements: KycRequirements,
) {
    private val userActionRequirements = requirements.userActionRequired.sortedBy {
        if (it.description == PROOF_OF_ADDRESS) 0 else 1
    }
    private val pendingRequirements = if (userActionRequirements.isEmpty()) {
        requirements.pendingPartnerAction.map { requirement ->
            KycPendingRequirementState(
                requirementType = requirement.toRequirementType(),
                status = KycPendingRequirementStatus.WaitingForReview,
            )
        } + requirements.pendingStripeAction.map { requirement ->
            KycPendingRequirementState(
                requirementType = requirement.toRequirementType(),
                status = KycPendingRequirementStatus.Processing,
            )
        }
    } else {
        emptyList()
    }
    private var requirementIndex = 0
    private val requirement: KycRequirement?
        get() = userActionRequirements.getOrNull(requirementIndex)
    private var questionnaire = KycQuestionnaireModel(requirement?.questionnaire)
    private var documents = KycDocumentCollectionModel(requirement?.document, requirement.toRequirementType())
    private var validationError: KycValidationError? = null
    private var submissionState = KycSubmissionState.Collecting
    private var page = initialPage(requirement, pendingRequirements)

    var state by mutableStateOf(buildState())
        private set

    val acceptedFormats: List<String>
        get() = documents.acceptedFormats

    val maximumFileSizeBytes: Long?
        get() = documents.maximumFileSizeBytes

    fun onContinue(): Boolean {
        if (!canEdit() || !isCollectionAvailable() || documents.selectingFileSlot != null) {
            return false
        }

        when (page) {
            KycCollectionPage.Context -> page = firstCollectionPage(requirement)
            KycCollectionPage.Questionnaire -> {
                if (questionnaire.hasMissingAnswers) {
                    validationError = KycValidationError.MissingRequiredAnswers
                    refreshState()
                    return false
                }
                page = if (requirement?.document == null) {
                    KycCollectionPage.Questionnaire
                } else if (requirement.toRequirementType() == KycRequirementType.SourceOfFunds) {
                    KycCollectionPage.DocumentOverview
                } else {
                    KycCollectionPage.DocumentEditor
                }
            }
            KycCollectionPage.DocumentEditor -> {
                if (requirement.toRequirementType() != KycRequirementType.SourceOfFunds) {
                    return false
                }
                if (!canContinue()) return false
                documents.finishEditing()
                page = KycCollectionPage.DocumentOverview
            }
            KycCollectionPage.DocumentOverview,
            KycCollectionPage.Pending,
            KycCollectionPage.Submitted,
            KycCollectionPage.Unavailable,
            -> return false
        }

        validationError = null
        documents.clearValidation()
        refreshState()
        return true
    }

    fun onBack(): Boolean {
        if (!canEdit()) {
            return false
        }

        page = when (page) {
            KycCollectionPage.Questionnaire -> KycCollectionPage.Context
            KycCollectionPage.DocumentOverview -> previousPageBeforeDocuments()
            KycCollectionPage.DocumentEditor -> {
                if (requirement.toRequirementType() == KycRequirementType.SourceOfFunds) {
                    documents.finishEditing()
                    KycCollectionPage.DocumentOverview
                } else {
                    previousPageBeforeDocuments()
                }
            }
            else -> return false
        }
        validationError = null
        documents.clearValidation()
        documents.onFileSelectionCancelled()
        refreshState()
        return true
    }

    fun onAddDocuments() {
        if (!canEdit() || !documents.onAddDocuments()) return
        page = KycCollectionPage.DocumentEditor
        validationError = null
        refreshState()
    }

    fun onEditDocuments(slotIndex: Int) {
        if (!canEdit() || !documents.onEditDocuments(slotIndex)) return
        page = KycCollectionPage.DocumentEditor
        validationError = null
        refreshState()
    }

    fun onQuestionAnswerChanged(questionId: String, answer: String) {
        if (!canEdit() || !questionnaire.updateAnswer(questionId, answer)) return
        submissionState = KycSubmissionState.Collecting
        validationError = null
        refreshState()
    }

    fun onDocumentSubtypeSelected(slotIndex: Int, subtypeId: String) {
        if (!canEdit() || !documents.onDocumentSubtypeSelected(slotIndex, subtypeId)) return
        submissionState = KycSubmissionState.Collecting
        validationError = null
        refreshState()
    }

    fun canSelectFile(slotIndex: Int): Boolean {
        return canEdit() && documents.canSelectFile(slotIndex)
    }

    fun onFileSelectionStarted(slotIndex: Int) {
        if (!canEdit() || !documents.onFileSelectionStarted(slotIndex)) return
        submissionState = KycSubmissionState.Collecting
        validationError = null
        refreshState()
    }

    fun onFileUploadStarted(slotIndex: Int, displayName: String) {
        documents.onFileUploadStarted(slotIndex, displayName)
        refreshState()
    }

    fun onFileSelectionCancelled() {
        documents.onFileSelectionCancelled()
        refreshState()
    }

    fun isAcceptedFile(displayName: String, mimeTypeExtension: String?): Boolean {
        val accepted = documents.isAcceptedFile(displayName, mimeTypeExtension)
        if (!accepted) {
            validationError = documents.validationError
            refreshState()
        }
        return accepted
    }

    fun isAcceptedFileSize(fileSizeBytes: Long): Boolean {
        val accepted = documents.isAcceptedFileSize(fileSizeBytes)
        if (!accepted) {
            validationError = documents.validationError
            refreshState()
        }
        return accepted
    }

    fun onFileSelected(slotIndex: Int, file: File, displayName: String, fileId: String): File? {
        if (!canSelectFile(slotIndex)) {
            onFileSelectionCancelled()
            return file
        }
        val replacedFile = documents.onFileSelected(slotIndex, file, displayName, fileId)
        submissionState = KycSubmissionState.Collecting
        validationError = null
        refreshState()
        return replacedFile
    }

    fun onFileSelectionFailed() {
        documents.onFileSelectionFailed()
        validationError = documents.validationError
        refreshState()
    }

    fun onFileTooLarge(displayName: String? = null) {
        documents.onFileTooLarge(displayName)
        validationError = documents.validationError
        refreshState()
    }

    fun onFileRemoved(slotIndex: Int): File? {
        if (!canEdit() || !documents.containsSlot(slotIndex)) return null
        val removedFile = documents.onFileRemoved(slotIndex, page == KycCollectionPage.DocumentEditor)
        submissionState = KycSubmissionState.Collecting
        validationError = null
        refreshState()
        return removedFile
    }

    fun startSubmission(): KycSubmission? {
        val submission = createSubmission() ?: return null
        submissionState = KycSubmissionState.Submitting
        validationError = null
        documents.clearValidation()
        documents.onFileSelectionCancelled()
        refreshState()
        return submission
    }

    fun onSubmissionFailed() {
        if (submissionState != KycSubmissionState.Submitting) {
            return
        }
        submissionState = KycSubmissionState.Failed
        refreshState()
    }

    fun onSubmissionSucceeded() {
        if (submissionState != KycSubmissionState.Submitting) {
            return
        }
        submissionState = KycSubmissionState.Submitted
        page = KycCollectionPage.Submitted
        refreshState()
    }

    fun advanceToNextRequirement(): Boolean {
        if (
            submissionState != KycSubmissionState.Submitted ||
            requirementIndex >= userActionRequirements.lastIndex
        ) {
            return false
        }

        requirementIndex += 1
        questionnaire = KycQuestionnaireModel(requirement?.questionnaire)
        documents = KycDocumentCollectionModel(requirement?.document, requirement.toRequirementType())
        validationError = null
        documents.clearValidation()
        documents.onFileSelectionCancelled()
        submissionState = KycSubmissionState.Collecting
        page = initialPage(requirement, emptyList())
        refreshState()
        return true
    }

    fun currentFiles(): List<File> = documents.currentFiles()

    fun createSubmission(): KycSubmission? {
        val requirement = requirement ?: return null
        if (!canEdit() || !isCollectionAvailable() || documents.selectingFileSlot != null) return null
        val error = currentValidationError()
        if (error != null) {
            validationError = error
            refreshState()
            return null
        }
        return KycSubmission(
            requirements = mapOf(
                requirement.description to KycRequirementSubmission(
                    requestedBy = requirement.requestedBy,
                    documents = documents.createSubmission(),
                    questionnaire = questionnaire.createSubmission(),
                )
            ),
        )
    }

    private fun refreshState() {
        state = buildState()
    }

    private fun buildState(): KycScreenState {
        return KycScreenState(
            page = page,
            requirementType = requirement.toRequirementType(),
            errorMessages = requirement?.errors?.map { it.developerMessage }.orEmpty(),
            questions = questionnaire.state,
            document = documents.buildState(),
            validationError = validationError,
            validationFileName = documents.validationFileName,
            selectingFileSlot = documents.selectingFileSlot,
            selectingFileName = documents.selectingFileName,
            canSubmit = canEdit() && isCollectionAvailable() &&
                documents.selectingFileSlot == null && currentValidationError() == null,
            canContinue = canContinue(),
            isCollectionAvailable = isCollectionAvailable(),
            submissionState = submissionState,
            currentRequirement = if (userActionRequirements.isEmpty()) 0 else requirementIndex + 1,
            totalRequirements = userActionRequirements.size,
            hasMoreRequirements = requirementIndex < userActionRequirements.lastIndex,
            pendingRequirements = pendingRequirements,
            completedDocumentCount = documents.completedDocumentCount,
        )
    }

    private fun canContinue(): Boolean {
        return when (page) {
            KycCollectionPage.Context -> isCollectionAvailable()
            KycCollectionPage.Questionnaire -> !questionnaire.hasMissingAnswers
            KycCollectionPage.DocumentEditor ->
                requirement.toRequirementType() == KycRequirementType.SourceOfFunds && documents.canContinue
            else -> false
        }
    }

    private fun canEdit(): Boolean {
        return submissionState == KycSubmissionState.Collecting ||
            submissionState == KycSubmissionState.Failed
    }

    private fun currentValidationError(): KycValidationError? {
        return if (questionnaire.hasMissingAnswers) {
            KycValidationError.MissingRequiredAnswers
        } else {
            documents.currentValidationError()
        }
    }

    private fun isCollectionAvailable(): Boolean = requirement?.isSupportedForCollection() == true

    private fun previousPageBeforeDocuments(): KycCollectionPage {
        return if (!questionnaire.hasQuestions) {
            KycCollectionPage.Context
        } else {
            KycCollectionPage.Questionnaire
        }
    }

    private fun KycRequirement?.toRequirementType(): KycRequirementType {
        return when (this?.description) {
            PROOF_OF_ADDRESS -> KycRequirementType.ProofOfAddress
            SOURCE_OF_FUNDS,
            SOURCE_OF_FUNDS_QUESTIONS,
            -> KycRequirementType.SourceOfFunds
            else -> KycRequirementType.Verification
        }
    }

    private companion object {
        private const val PROOF_OF_ADDRESS = "proof_of_address"
        private const val SOURCE_OF_FUNDS = "source_of_funds"
        private const val SOURCE_OF_FUNDS_QUESTIONS = "source_of_funds_questions"
        private fun initialPage(
            requirement: KycRequirement?,
            pendingRequirements: List<KycPendingRequirementState>,
        ): KycCollectionPage {
            return when {
                pendingRequirements.isNotEmpty() -> KycCollectionPage.Pending
                requirement == null -> KycCollectionPage.Unavailable
                !requirement.isSupportedForCollection() ->
                    KycCollectionPage.Unavailable
                requirement.description !in setOf(
                    PROOF_OF_ADDRESS,
                    SOURCE_OF_FUNDS,
                    SOURCE_OF_FUNDS_QUESTIONS,
                ) -> KycCollectionPage.Unavailable
                else -> KycCollectionPage.Context
            }
        }

        private fun firstCollectionPage(requirement: KycRequirement?): KycCollectionPage {
            val hasVisibleQuestions = !requirement?.questionnaire?.questions.isNullOrEmpty()
            return when {
                hasVisibleQuestions -> KycCollectionPage.Questionnaire
                requirement?.description == SOURCE_OF_FUNDS && requirement.document != null ->
                    KycCollectionPage.DocumentOverview
                requirement?.document != null -> KycCollectionPage.DocumentEditor
                else -> KycCollectionPage.Questionnaire
            }
        }
    }
}
