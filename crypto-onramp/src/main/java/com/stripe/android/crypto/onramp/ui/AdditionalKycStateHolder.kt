package com.stripe.android.crypto.onramp.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.stripe.android.crypto.onramp.model.AdditionalKycDocumentSubmission
import com.stripe.android.crypto.onramp.model.AdditionalKycQuestionnaireAnswer
import com.stripe.android.crypto.onramp.model.AdditionalKycQuestionnaireSubmission
import com.stripe.android.crypto.onramp.model.AdditionalKycRequirement
import com.stripe.android.crypto.onramp.model.AdditionalKycRequirements
import com.stripe.android.crypto.onramp.model.AdditionalKycSubmission
import com.stripe.android.link.onramp.ui.AdditionalKycCollectionPage
import com.stripe.android.link.onramp.ui.AdditionalKycDocumentSlotState
import com.stripe.android.link.onramp.ui.AdditionalKycDocumentState
import com.stripe.android.link.onramp.ui.AdditionalKycDocumentSubtypeState
import com.stripe.android.link.onramp.ui.AdditionalKycPendingRequirementState
import com.stripe.android.link.onramp.ui.AdditionalKycPendingRequirementStatus
import com.stripe.android.link.onramp.ui.AdditionalKycQuestionState
import com.stripe.android.link.onramp.ui.AdditionalKycRequirementType
import com.stripe.android.link.onramp.ui.AdditionalKycScreenState
import com.stripe.android.link.onramp.ui.AdditionalKycSubmissionState
import com.stripe.android.link.onramp.ui.AdditionalKycValidationError
import java.io.File
import java.util.Locale

@Suppress("TooManyFunctions")
internal class AdditionalKycStateHolder(
    requirements: AdditionalKycRequirements,
) {
    private val userActionRequirements = requirements.userActionRequired
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
    private var answers = createAnswers(requirement)
    private var documentSlots = createDocumentSlots(requirement)
    private var validationError: AdditionalKycValidationError? = null
    private var validationFileName: String? = null
    private var selectingFileSlot: Int? = null
    private var selectingFileName: String? = null
    private var submissionState = AdditionalKycSubmissionState.Collecting
    private var page = initialPage(requirement, pendingRequirements)
    private var editingDocumentSlot: Int? = initialEditingSlot(requirement, page)

    var state by mutableStateOf(buildState())
        private set

    val acceptedFormats: List<String>
        get() = requirement?.document?.acceptedFormats.orEmpty()

    val maximumFileSizeBytes: Long?
        get() = when (requirement?.description) {
            PROOF_OF_ADDRESS,
            SOURCE_OF_FUNDS,
            -> MAX_FILE_SIZE_BYTES
            else -> null
        }

    fun onContinue(): Boolean {
        if (!canEdit()) {
            return false
        }

        when (page) {
            AdditionalKycCollectionPage.Context -> page = firstCollectionPage(requirement)
            AdditionalKycCollectionPage.Questionnaire -> {
                if (hasMissingVisibleAnswers()) {
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
                discardEmptyDocumentSlots()
                page = AdditionalKycCollectionPage.DocumentOverview
                editingDocumentSlot = null
            }
            AdditionalKycCollectionPage.DocumentOverview,
            AdditionalKycCollectionPage.Pending,
            AdditionalKycCollectionPage.Submitted,
            AdditionalKycCollectionPage.Unavailable,
            -> return false
        }

        validationError = null
        validationFileName = null
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
                    discardEmptyDocumentSlots()
                    editingDocumentSlot = null
                    AdditionalKycCollectionPage.DocumentOverview
                } else {
                    previousPageBeforeDocuments()
                }
            }
            else -> return false
        }
        validationError = null
        validationFileName = null
        selectingFileSlot = null
        selectingFileName = null
        refreshState()
        return true
    }

    fun onAddDocuments() {
        if (!canEdit() || requirement?.document == null) {
            return
        }
        val slot = documentSlots.firstOrNull { it.file == null } ?: DocumentSlot(
            index = nextDocumentSlotIndex(),
            subtypeId = requirement?.document?.acceptedSubtypes?.firstOrNull()?.id,
            file = null,
        ).also { newSlot -> documentSlots = documentSlots + newSlot }
        editingDocumentSlot = slot.index
        page = AdditionalKycCollectionPage.DocumentEditor
        validationError = null
        validationFileName = null
        refreshState()
    }

    fun onEditDocuments(slotIndex: Int) {
        if (!canEdit() || requirement?.document == null) {
            return
        }
        val selectedSlot = documentSlots.firstOrNull { slot -> slot.index == slotIndex } ?: return
        val editingSlot = documentSlots.firstOrNull { slot ->
            slot.file == null && slot.subtypeId == selectedSlot.subtypeId
        } ?: DocumentSlot(
            index = nextDocumentSlotIndex(),
            subtypeId = selectedSlot.subtypeId,
            file = null,
        ).also { newSlot -> documentSlots = documentSlots + newSlot }
        editingDocumentSlot = editingSlot.index
        page = AdditionalKycCollectionPage.DocumentEditor
        validationError = null
        validationFileName = null
        refreshState()
    }

    fun onQuestionAnswerChanged(questionId: String, answer: String) {
        if (!canEdit() || questionId !in answers) {
            return
        }

        answers[questionId] = answer
        submissionState = AdditionalKycSubmissionState.Collecting
        validationError = null
        refreshState()
    }

    fun onDocumentSubtypeSelected(slotIndex: Int, subtypeId: String) {
        if (!canEdit()) {
            return
        }
        val document = requirement?.document ?: return
        if (document.acceptedSubtypes.none { subtype -> subtype.id == subtypeId }) {
            return
        }

        updateDocumentSlot(slotIndex) { slot -> slot.copy(subtypeId = subtypeId) }
        editingDocumentSlot = slotIndex
        submissionState = AdditionalKycSubmissionState.Collecting
        validationError = null
        validationFileName = null
        refreshState()
    }

    fun onFileSelectionStarted(slotIndex: Int) {
        if (!canEdit() || documentSlots.none { slot -> slot.index == slotIndex }) {
            return
        }

        submissionState = AdditionalKycSubmissionState.Collecting
        selectingFileSlot = slotIndex
        selectingFileName = null
        validationError = null
        validationFileName = null
        refreshState()
    }

    fun onFileUploadStarted(slotIndex: Int, displayName: String) {
        if (selectingFileSlot != slotIndex) {
            return
        }
        selectingFileName = displayName
        refreshState()
    }

    fun onFileSelectionCancelled() {
        selectingFileSlot = null
        selectingFileName = null
        refreshState()
    }

    fun isAcceptedFile(displayName: String, mimeTypeExtension: String?): Boolean {
        val accepted = acceptedFormats
            .map(::normalizeExtension)
            .filter(String::isNotEmpty)
            .toSet()
        if (accepted.isEmpty()) {
            return true
        }

        val candidateExtensions = setOfNotNull(
            displayName.substringAfterLast('.', missingDelimiterValue = ""),
            mimeTypeExtension,
        ).map(::normalizeExtension)

        val isAccepted = candidateExtensions.any { extension -> extension in accepted }
        if (!isAccepted) {
            selectingFileSlot = null
            selectingFileName = null
            validationError = AdditionalKycValidationError.UnsupportedFileType
            validationFileName = displayName
            refreshState()
        }
        return isAccepted
    }

    fun isAcceptedFileSize(fileSizeBytes: Long): Boolean {
        val maximumBytes = maximumFileSizeBytes ?: return true
        val isAccepted = fileSizeBytes <= maximumBytes
        if (!isAccepted) {
            onFileTooLarge()
        }
        return isAccepted
    }

    fun onFileSelected(slotIndex: Int, file: File, displayName: String): File? {
        if (!canEdit()) {
            return file
        }
        var replacedFile: File? = null
        updateDocumentSlot(slotIndex) { slot ->
            replacedFile = slot.file?.file
            slot.copy(file = SelectedKycFile(file = file, displayName = displayName))
        }
        submissionState = AdditionalKycSubmissionState.Collecting
        selectingFileSlot = null
        selectingFileName = null
        validationError = null
        validationFileName = null
        addNextUploadSlotIfNeeded(completedSlotIndex = slotIndex)
        refreshState()
        return replacedFile
    }

    fun onFileSelectionFailed() {
        selectingFileSlot = null
        selectingFileName = null
        validationError = AdditionalKycValidationError.FileUnavailable
        validationFileName = null
        refreshState()
    }

    fun onFileTooLarge(displayName: String? = null) {
        selectingFileSlot = null
        validationError = AdditionalKycValidationError.FileTooLarge
        validationFileName = displayName ?: selectingFileName
        selectingFileName = null
        refreshState()
    }

    fun onFileRemoved(slotIndex: Int): File? {
        if (!canEdit()) {
            return null
        }
        val slot = documentSlots.firstOrNull { it.index == slotIndex } ?: return null
        val removedFile = slot.file?.file
        documentSlots = if (slot.file == null || documentSlots.count { it.file != null } > 1) {
            documentSlots.filterNot { it.index == slotIndex }
        } else {
            documentSlots.map { candidate ->
                if (candidate.index == slotIndex) candidate.copy(file = null) else candidate
            }
        }
        ensureEditingSlot()
        submissionState = AdditionalKycSubmissionState.Collecting
        validationError = null
        validationFileName = null
        refreshState()
        return removedFile
    }

    fun startSubmission(): AdditionalKycSubmission? {
        val submission = createSubmission() ?: return null
        submissionState = AdditionalKycSubmissionState.Submitting
        validationError = null
        validationFileName = null
        selectingFileSlot = null
        selectingFileName = null
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
        answers = createAnswers(requirement)
        documentSlots = createDocumentSlots(requirement)
        validationError = null
        validationFileName = null
        selectingFileSlot = null
        selectingFileName = null
        submissionState = AdditionalKycSubmissionState.Collecting
        page = initialPage(requirement, emptyList())
        editingDocumentSlot = initialEditingSlot(requirement, page)
        refreshState()
        return true
    }

    fun currentFiles(): List<File> {
        return documentSlots.mapNotNull { slot -> slot.file?.file }
    }

    fun createSubmission(): AdditionalKycSubmission? {
        val requirement = requirement ?: return null
        if (!canEdit() || !isCollectionAvailable()) {
            return null
        }
        val error = currentValidationError()
        if (error != null) {
            validationError = error
            refreshState()
            return null
        }

        val completedSlots = documentSlots.filter { slot -> slot.file != null }
        val fundingSources = completedSlots
            .mapNotNull { slot -> subtypeLabel(slot.subtypeId) }
            .distinct()
            .joinToString()

        return AdditionalKycSubmission(
            liquidityProvider = requirement.requestedBy,
            submissionType = requirement.submissionType,
            documents = if (requirement.submissionType == DOCUMENT_SUBMISSION_TYPE) {
                completedSlots.groupBy { slot -> slot.subtypeId }.map { (subtypeId, slots) ->
                    AdditionalKycDocumentSubmission(
                        documentType = requirement.description,
                        documentSubtype = subtypeId,
                        files = slots.map { slot -> requireNotNull(slot.file).file },
                    )
                }
            } else {
                null
            },
            questionnaire = requirement.questionnaire?.let { questionnaire ->
                AdditionalKycQuestionnaireSubmission(
                    answers = questionnaire.questions.mapNotNull { question ->
                        val answer = if (
                            requirement.document != null &&
                            requirement.toRequirementType() == AdditionalKycRequirementType.SourceOfFunds &&
                            question.id == FUNDING_SOURCES_QUESTION_ID &&
                            fundingSources.isNotBlank()
                        ) {
                            fundingSources
                        } else {
                            answers[question.id]
                        }
                        answer
                            ?.takeIf { value -> value.isNotBlank() || question.required }
                            ?.let { value ->
                                AdditionalKycQuestionnaireAnswer(
                                    questionId = question.id,
                                    value = value.trim(),
                                )
                            }
                    },
                )
            },
        )
    }

    private fun refreshState() {
        state = buildState()
    }

    @Suppress("LongMethod")
    private fun buildState(): AdditionalKycScreenState {
        val requirement = requirement
        val document = requirement?.document
        val requirementType = requirement.toRequirementType()
        val completedDocumentCount = documentSlots.count { slot -> slot.file != null }

        return AdditionalKycScreenState(
            page = page,
            requirementType = requirementType,
            errorMessages = requirement?.errors?.map { error -> error.message }.orEmpty(),
            questions = requirement?.questionnaire?.questions.orEmpty().map { question ->
                AdditionalKycQuestionState(
                    id = question.id,
                    prompt = question.prompt,
                    answer = answers[question.id].orEmpty(),
                    required = question.required,
                )
            },
            document = document?.let {
                AdditionalKycDocumentState(
                    acceptedFormats = it.acceptedFormats,
                    instructions = it.instructions,
                    maxFileSizeMegabytes = maximumFileSizeBytes
                        ?.div(BYTES_PER_MEGABYTE)
                        ?.toInt(),
                    minDocuments = it.minDocuments.coerceAtLeast(MINIMUM_DOCUMENT_COUNT),
                    maxDocuments = if (requirementType == AdditionalKycRequirementType.ProofOfAddress) {
                        PROOF_OF_ADDRESS_MAX_DOCUMENT_COUNT
                    } else {
                        SOURCE_OF_FUNDS_MAX_DOCUMENT_COUNT
                    },
                    editingSlotIndex = editingDocumentSlot,
                    slots = documentSlots.map { slot ->
                        AdditionalKycDocumentSlotState(
                            index = slot.index,
                            subtypes = it.acceptedSubtypes.map { subtype ->
                                AdditionalKycDocumentSubtypeState(
                                    id = subtype.id,
                                    label = subtype.label,
                                    isEnabled = true,
                                )
                            },
                            selectedSubtypeId = slot.subtypeId,
                            selectedSubtypeLabel = subtypeLabel(slot.subtypeId),
                            fileName = slot.file?.displayName,
                        )
                    },
                )
            },
            validationError = validationError,
            validationFileName = validationFileName,
            selectingFileSlot = selectingFileSlot,
            selectingFileName = selectingFileName,
            canSubmit = canEdit() &&
                isCollectionAvailable() &&
                selectingFileSlot == null &&
                currentValidationError() == null,
            canContinue = canContinue(),
            isCollectionAvailable = isCollectionAvailable(),
            submissionState = submissionState,
            currentRequirement = if (userActionRequirements.isEmpty()) 0 else requirementIndex + 1,
            totalRequirements = userActionRequirements.size,
            hasMoreRequirements = requirementIndex < userActionRequirements.lastIndex,
            pendingRequirements = pendingRequirements,
            completedDocumentCount = completedDocumentCount,
        )
    }

    private fun canContinue(): Boolean {
        return when (page) {
            AdditionalKycCollectionPage.Context -> isCollectionAvailable()
            AdditionalKycCollectionPage.Questionnaire -> !hasMissingVisibleAnswers()
            AdditionalKycCollectionPage.DocumentEditor ->
                requirement.toRequirementType() == AdditionalKycRequirementType.SourceOfFunds &&
                    documentSlots.any { slot -> slot.file != null }
            else -> false
        }
    }

    private fun canEdit(): Boolean {
        return submissionState == AdditionalKycSubmissionState.Collecting ||
            submissionState == AdditionalKycSubmissionState.Failed
    }

    private fun currentValidationError(): AdditionalKycValidationError? {
        val requirement = requirement ?: return null

        val hasMissingAnswers = requirement.questionnaire?.questions.orEmpty().any { question ->
            if (
                requirement.document != null &&
                requirement.toRequirementType() == AdditionalKycRequirementType.SourceOfFunds &&
                question.id == FUNDING_SOURCES_QUESTION_ID
            ) {
                question.required && documentSlots.none { slot -> slot.file != null }
            } else {
                question.required && answers[question.id].isNullOrBlank()
            }
        }
        if (hasMissingAnswers) {
            return AdditionalKycValidationError.MissingRequiredAnswers
        }

        if (requirement.submissionType == DOCUMENT_SUBMISSION_TYPE) {
            val document = requirement.document ?: return null
            val completedSlots = documentSlots.filter { slot -> slot.file != null }
            if (completedSlots.size < document.minDocuments.coerceAtLeast(MINIMUM_DOCUMENT_COUNT)) {
                return AdditionalKycValidationError.MissingDocuments
            }
            if (document.acceptedSubtypes.isNotEmpty() && completedSlots.any { it.subtypeId == null }) {
                return AdditionalKycValidationError.MissingDocumentType
            }
        }

        return null
    }

    private fun hasMissingVisibleAnswers(): Boolean {
        return visibleQuestions().any { question ->
            question.required && answers[question.id].isNullOrBlank()
        }
    }

    private fun visibleQuestions() = requirement?.questionnaire?.questions.orEmpty().filterNot { question ->
        requirement?.document != null &&
            requirement.toRequirementType() == AdditionalKycRequirementType.SourceOfFunds &&
            question.id == FUNDING_SOURCES_QUESTION_ID
    }

    private fun isCollectionAvailable(): Boolean {
        val requirement = requirement ?: return false
        return when (requirement.submissionType) {
            DOCUMENT_SUBMISSION_TYPE -> requirement.document != null
            QUESTIONNAIRE_SUBMISSION_TYPE -> requirement.questionnaire != null
            else -> false
        }
    }

    private fun addNextUploadSlotIfNeeded(completedSlotIndex: Int) {
        val requirementType = requirement.toRequirementType()
        val completedDocumentCount = documentSlots.count { slot -> slot.file != null }
        val minimumDocumentCount = requirement?.document
            ?.minDocuments
            ?.coerceAtLeast(MINIMUM_DOCUMENT_COUNT)
            ?: MINIMUM_DOCUMENT_COUNT
        if (
            requirementType == AdditionalKycRequirementType.ProofOfAddress &&
            completedDocumentCount >= minimumDocumentCount
        ) {
            editingDocumentSlot = completedSlotIndex
            return
        }
        val maximumDocumentCount = if (requirementType == AdditionalKycRequirementType.ProofOfAddress) {
            PROOF_OF_ADDRESS_MAX_DOCUMENT_COUNT
        } else {
            SOURCE_OF_FUNDS_MAX_DOCUMENT_COUNT
        }
        if (completedDocumentCount >= maximumDocumentCount) {
            editingDocumentSlot = null
            return
        }

        val completedSlot = documentSlots.firstOrNull { slot -> slot.index == completedSlotIndex } ?: return
        val nextSlot = DocumentSlot(
            index = nextDocumentSlotIndex(),
            subtypeId = completedSlot.subtypeId,
            file = null,
        )
        documentSlots = documentSlots + nextSlot
        editingDocumentSlot = nextSlot.index
    }

    private fun ensureEditingSlot() {
        val emptySlot = documentSlots.firstOrNull { slot -> slot.file == null }
        if (page != AdditionalKycCollectionPage.DocumentEditor) {
            editingDocumentSlot = null
        } else if (emptySlot != null) {
            editingDocumentSlot = emptySlot.index
        } else {
            val slot = DocumentSlot(
                index = nextDocumentSlotIndex(),
                subtypeId = documentSlots.lastOrNull()?.subtypeId,
                file = null,
            )
            documentSlots = documentSlots + slot
            editingDocumentSlot = slot.index
        }
    }

    private fun discardEmptyDocumentSlots() {
        documentSlots = documentSlots.filter { slot -> slot.file != null }
    }

    private fun previousPageBeforeDocuments(): AdditionalKycCollectionPage {
        return if (visibleQuestions().isEmpty()) {
            AdditionalKycCollectionPage.Context
        } else {
            AdditionalKycCollectionPage.Questionnaire
        }
    }

    private fun subtypeLabel(subtypeId: String?): String? {
        return requirement?.document?.acceptedSubtypes
            ?.firstOrNull { subtype -> subtype.id == subtypeId }
            ?.label
    }

    private fun nextDocumentSlotIndex(): Int {
        return (documentSlots.maxOfOrNull { slot -> slot.index } ?: -1) + 1
    }

    private fun updateDocumentSlot(
        slotIndex: Int,
        transform: (DocumentSlot) -> DocumentSlot,
    ) {
        documentSlots = documentSlots.map { slot ->
            if (slot.index == slotIndex) transform(slot) else slot
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

    private data class DocumentSlot(
        val index: Int,
        val subtypeId: String?,
        val file: SelectedKycFile?,
    )

    private data class SelectedKycFile(
        val file: File,
        val displayName: String,
    )

    private companion object {
        private const val DOCUMENT_SUBMISSION_TYPE = "document"
        private const val QUESTIONNAIRE_SUBMISSION_TYPE = "questionnaire"
        private const val PROOF_OF_ADDRESS = "proof_of_address"
        private const val SOURCE_OF_FUNDS = "source_of_funds"
        private const val SOURCE_OF_FUNDS_QUESTIONS = "source_of_funds_questions"
        private const val FUNDING_SOURCES_QUESTION_ID = "funding_sources"
        private const val MINIMUM_DOCUMENT_COUNT = 1
        private const val PROOF_OF_ADDRESS_MAX_DOCUMENT_COUNT = 2
        private const val SOURCE_OF_FUNDS_MAX_DOCUMENT_COUNT = 10
        private const val BYTES_PER_MEGABYTE = 1_000_000L
        private const val MAX_FILE_SIZE_BYTES = 5L * BYTES_PER_MEGABYTE

        private fun createAnswers(requirement: AdditionalKycRequirement?): MutableMap<String, String> {
            return requirement
                ?.questionnaire
                ?.questions
                .orEmpty()
                .associate { question -> question.id to "" }
                .toMutableMap()
        }

        private fun createDocumentSlots(requirement: AdditionalKycRequirement?): List<DocumentSlot> {
            if (requirement?.submissionType != DOCUMENT_SUBMISSION_TYPE) {
                return emptyList()
            }
            return listOf(
                DocumentSlot(
                    index = 0,
                    subtypeId = requirement.document?.acceptedSubtypes?.firstOrNull()?.id,
                    file = null,
                )
            )
        }

        private fun initialPage(
            requirement: AdditionalKycRequirement?,
            pendingRequirements: List<AdditionalKycPendingRequirementState>,
        ): AdditionalKycCollectionPage {
            return when {
                pendingRequirements.isNotEmpty() -> AdditionalKycCollectionPage.Pending
                requirement == null -> AdditionalKycCollectionPage.Unavailable
                requirement.submissionType == DOCUMENT_SUBMISSION_TYPE && requirement.document == null ->
                    AdditionalKycCollectionPage.Unavailable
                requirement.submissionType == QUESTIONNAIRE_SUBMISSION_TYPE && requirement.questionnaire == null ->
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
            val hasVisibleQuestions = requirement?.questionnaire?.questions.orEmpty().any { question ->
                requirement?.description !in setOf(SOURCE_OF_FUNDS, SOURCE_OF_FUNDS_QUESTIONS) ||
                    question.id != FUNDING_SOURCES_QUESTION_ID
            }
            return when {
                hasVisibleQuestions -> AdditionalKycCollectionPage.Questionnaire
                requirement?.document != null -> AdditionalKycCollectionPage.DocumentEditor
                else -> AdditionalKycCollectionPage.Questionnaire
            }
        }

        private fun initialEditingSlot(
            requirement: AdditionalKycRequirement?,
            page: AdditionalKycCollectionPage,
        ): Int? {
            return if (
                page == AdditionalKycCollectionPage.DocumentEditor ||
                requirement?.description == PROOF_OF_ADDRESS
            ) {
                0
            } else {
                null
            }
        }

        private fun normalizeExtension(extension: String): String {
            return when (extension.trim().removePrefix(".").lowercase(Locale.ROOT)) {
                "jpg" -> "jpeg"
                "tif" -> "tiff"
                else -> extension.trim().removePrefix(".").lowercase(Locale.ROOT)
            }
        }
    }
}
