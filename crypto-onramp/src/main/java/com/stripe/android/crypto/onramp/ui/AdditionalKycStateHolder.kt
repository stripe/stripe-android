package com.stripe.android.crypto.onramp.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.stripe.android.crypto.onramp.model.AdditionalKycRequirement
import com.stripe.android.crypto.onramp.model.AdditionalKycRequirementSubmission
import com.stripe.android.crypto.onramp.model.AdditionalKycRequirements
import com.stripe.android.crypto.onramp.model.AdditionalKycSubmission
import java.io.File

@Suppress("TooManyFunctions")
internal class AdditionalKycStateHolder(
    requirements: AdditionalKycRequirements,
) {
    private val userActionRequirements = requirements.userActionRequired.sortedBy {
        if (it.description == PROOF_OF_ADDRESS) 0 else 1
    }
    private val pendingRequirements = if (userActionRequirements.isEmpty()) {
        requirements.pendingPartnerAction.map { requirement ->
            AdditionalKycPendingRequirementState(
                requirementType = requirement.toRequirementType(),
                status = AdditionalKycPendingRequirementStatus.WaitingForReview,
            )
        } + requirements.pendingStripeAction.map { requirement ->
            AdditionalKycPendingRequirementState(
                requirementType = requirement.toRequirementType(),
                status = AdditionalKycPendingRequirementStatus.Processing,
            )
        }
    } else {
        emptyList()
    }
    private var requirementIndex = 0
    private val requirement: AdditionalKycRequirement?
        get() = userActionRequirements.getOrNull(requirementIndex)
    private var questionnaire = AdditionalKycQuestionnaireModel(requirement?.questionnaire)
    private var documents = AdditionalKycDocumentCollectionModel(requirement?.document, requirement.toRequirementType())
    private var validationError: AdditionalKycValidationError? = null
    private var submissionState = AdditionalKycSubmissionState.Collecting
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
            AdditionalKycCollectionPage.Context -> page = firstCollectionPage(requirement)
            AdditionalKycCollectionPage.Questionnaire -> {
                if (questionnaire.hasMissingAnswers) {
                    validationError = AdditionalKycValidationError.MissingRequiredAnswers
                    refreshState()
                    return false
                }
                page = if (requirement?.document == null) {
                    AdditionalKycCollectionPage.Questionnaire
                } else if (requirement.toRequirementType() == AdditionalKycRequirementType.SourceOfFunds) {
                    AdditionalKycCollectionPage.DocumentOverview
                } else {
                    AdditionalKycCollectionPage.DocumentEditor
                }
            }
            AdditionalKycCollectionPage.DocumentEditor -> {
                if (requirement.toRequirementType() != AdditionalKycRequirementType.SourceOfFunds) {
                    return false
                }
                if (!canContinue()) return false
                documents.finishEditing()
                page = AdditionalKycCollectionPage.DocumentOverview
            }
            AdditionalKycCollectionPage.DocumentOverview,
            AdditionalKycCollectionPage.Pending,
            AdditionalKycCollectionPage.Submitted,
            AdditionalKycCollectionPage.Unavailable,
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
            AdditionalKycCollectionPage.Questionnaire -> AdditionalKycCollectionPage.Context
            AdditionalKycCollectionPage.DocumentOverview -> previousPageBeforeDocuments()
            AdditionalKycCollectionPage.DocumentEditor -> {
                if (requirement.toRequirementType() == AdditionalKycRequirementType.SourceOfFunds) {
                    documents.finishEditing()
                    AdditionalKycCollectionPage.DocumentOverview
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
        page = AdditionalKycCollectionPage.DocumentEditor
        validationError = null
        refreshState()
    }

    fun onEditDocuments(slotIndex: Int) {
        if (!canEdit() || !documents.onEditDocuments(slotIndex)) return
        page = AdditionalKycCollectionPage.DocumentEditor
        validationError = null
        refreshState()
    }

    fun onQuestionAnswerChanged(questionId: String, answer: String) {
        if (!canEdit() || !questionnaire.updateAnswer(questionId, answer)) return
        submissionState = AdditionalKycSubmissionState.Collecting
        validationError = null
        refreshState()
    }

    fun onDocumentSubtypeSelected(slotIndex: Int, subtypeId: String) {
        if (!canEdit() || !documents.onDocumentSubtypeSelected(slotIndex, subtypeId)) return
        submissionState = AdditionalKycSubmissionState.Collecting
        validationError = null
        refreshState()
    }

    fun canSelectFile(slotIndex: Int): Boolean {
        return canEdit() && documents.canSelectFile(slotIndex)
    }

    fun onFileSelectionStarted(slotIndex: Int) {
        if (!canEdit() || !documents.onFileSelectionStarted(slotIndex)) return
        submissionState = AdditionalKycSubmissionState.Collecting
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
        submissionState = AdditionalKycSubmissionState.Collecting
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
        val removedFile = documents.onFileRemoved(slotIndex, page == AdditionalKycCollectionPage.DocumentEditor)
        submissionState = AdditionalKycSubmissionState.Collecting
        validationError = null
        refreshState()
        return removedFile
    }

    fun startSubmission(): AdditionalKycSubmission? {
        val submission = createSubmission() ?: return null
        submissionState = AdditionalKycSubmissionState.Submitting
        validationError = null
        documents.clearValidation()
        documents.onFileSelectionCancelled()
        refreshState()
        return submission
    }

    fun onSubmissionFailed() {
        if (submissionState != AdditionalKycSubmissionState.Submitting) {
            return
        }
        submissionState = AdditionalKycSubmissionState.Failed
        refreshState()
    }

    fun onSubmissionSucceeded() {
        if (submissionState != AdditionalKycSubmissionState.Submitting) {
            return
        }
        submissionState = AdditionalKycSubmissionState.Submitted
        page = AdditionalKycCollectionPage.Submitted
        refreshState()
    }

    fun advanceToNextRequirement(): Boolean {
        if (
            submissionState != AdditionalKycSubmissionState.Submitted ||
            requirementIndex >= userActionRequirements.lastIndex
        ) {
            return false
        }

        requirementIndex += 1
        questionnaire = AdditionalKycQuestionnaireModel(requirement?.questionnaire)
        documents = AdditionalKycDocumentCollectionModel(requirement?.document, requirement.toRequirementType())
        validationError = null
        documents.clearValidation()
        documents.onFileSelectionCancelled()
        submissionState = AdditionalKycSubmissionState.Collecting
        page = initialPage(requirement, emptyList())
        refreshState()
        return true
    }

    fun currentFiles(): List<File> = documents.currentFiles()

    fun createSubmission(): AdditionalKycSubmission? {
        val requirement = requirement ?: return null
        if (!canEdit() || !isCollectionAvailable() || documents.selectingFileSlot != null) return null
        val error = currentValidationError()
        if (error != null) {
            validationError = error
            refreshState()
            return null
        }
        return AdditionalKycSubmission(
            requirements = mapOf(
                requirement.description to AdditionalKycRequirementSubmission(
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

    private fun buildState(): AdditionalKycScreenState {
        return AdditionalKycScreenState(
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
            AdditionalKycCollectionPage.Context -> isCollectionAvailable()
            AdditionalKycCollectionPage.Questionnaire -> !questionnaire.hasMissingAnswers
            AdditionalKycCollectionPage.DocumentEditor ->
                requirement.toRequirementType() == AdditionalKycRequirementType.SourceOfFunds && documents.canContinue
            else -> false
        }
    }

    private fun canEdit(): Boolean {
        return submissionState == AdditionalKycSubmissionState.Collecting ||
            submissionState == AdditionalKycSubmissionState.Failed
    }

    private fun currentValidationError(): AdditionalKycValidationError? {
        return if (questionnaire.hasMissingAnswers) {
            AdditionalKycValidationError.MissingRequiredAnswers
        } else {
            documents.currentValidationError()
        }
    }

    private fun isCollectionAvailable(): Boolean = requirement?.isSupportedForCollection() == true

    private fun previousPageBeforeDocuments(): AdditionalKycCollectionPage {
        return if (!questionnaire.hasQuestions) {
            AdditionalKycCollectionPage.Context
        } else {
            AdditionalKycCollectionPage.Questionnaire
        }
    }

    private fun AdditionalKycRequirement?.toRequirementType(): AdditionalKycRequirementType {
        return when (this?.description) {
            PROOF_OF_ADDRESS -> AdditionalKycRequirementType.ProofOfAddress
            SOURCE_OF_FUNDS,
            SOURCE_OF_FUNDS_QUESTIONS,
            -> AdditionalKycRequirementType.SourceOfFunds
            else -> AdditionalKycRequirementType.AdditionalVerification
        }
    }

    private companion object {
        private const val PROOF_OF_ADDRESS = "proof_of_address"
        private const val SOURCE_OF_FUNDS = "source_of_funds"
        private const val SOURCE_OF_FUNDS_QUESTIONS = "source_of_funds_questions"
        private fun initialPage(
            requirement: AdditionalKycRequirement?,
            pendingRequirements: List<AdditionalKycPendingRequirementState>,
        ): AdditionalKycCollectionPage {
            return when {
                pendingRequirements.isNotEmpty() -> AdditionalKycCollectionPage.Pending
                requirement == null -> AdditionalKycCollectionPage.Unavailable
                !requirement.isSupportedForCollection() ->
                    AdditionalKycCollectionPage.Unavailable
                requirement.description !in setOf(
                    PROOF_OF_ADDRESS,
                    SOURCE_OF_FUNDS,
                    SOURCE_OF_FUNDS_QUESTIONS,
                ) -> AdditionalKycCollectionPage.Unavailable
                else -> AdditionalKycCollectionPage.Context
            }
        }

        private fun firstCollectionPage(requirement: AdditionalKycRequirement?): AdditionalKycCollectionPage {
            val hasVisibleQuestions = !requirement?.questionnaire?.questions.isNullOrEmpty()
            return when {
                hasVisibleQuestions -> AdditionalKycCollectionPage.Questionnaire
                requirement?.description == SOURCE_OF_FUNDS && requirement.document != null ->
                    AdditionalKycCollectionPage.DocumentOverview
                requirement?.document != null -> AdditionalKycCollectionPage.DocumentEditor
                else -> AdditionalKycCollectionPage.Questionnaire
            }
        }
    }
}
