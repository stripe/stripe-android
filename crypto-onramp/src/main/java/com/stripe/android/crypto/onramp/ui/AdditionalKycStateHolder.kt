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
    private var selectingFileSlot: Int? = null
    private var submissionState = AdditionalKycSubmissionState.Collecting

    var state by mutableStateOf(buildState())
        private set

    val acceptedFormats: List<String>
        get() = requirement?.document?.acceptedFormats.orEmpty()

    val maximumFileSizeBytes: Long?
        get() = when (requirement?.description) {
            PROOF_OF_ADDRESS -> PROOF_OF_ADDRESS_MAX_FILE_SIZE_BYTES
            SOURCE_OF_FUNDS -> SOURCE_OF_FUNDS_MAX_FILE_SIZE_BYTES
            else -> null
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

        val isDuplicate = documentSlots.any { slot ->
            slot.index != slotIndex && slot.subtypeId == subtypeId
        }
        if (isDuplicate) {
            validationError = AdditionalKycValidationError.DuplicateDocumentType
            refreshState()
            return
        }

        updateDocumentSlot(slotIndex) { slot -> slot.copy(subtypeId = subtypeId) }
        submissionState = AdditionalKycSubmissionState.Collecting
        validationError = null
        refreshState()
    }

    fun onFileSelectionStarted(slotIndex: Int) {
        if (!canEdit() || documentSlots.none { slot -> slot.index == slotIndex }) {
            return
        }

        submissionState = AdditionalKycSubmissionState.Collecting
        selectingFileSlot = slotIndex
        validationError = null
        refreshState()
    }

    fun onFileSelectionCancelled() {
        selectingFileSlot = null
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
            validationError = AdditionalKycValidationError.UnsupportedFileType
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
        validationError = null
        refreshState()
        return replacedFile
    }

    fun onFileSelectionFailed() {
        selectingFileSlot = null
        validationError = AdditionalKycValidationError.FileUnavailable
        refreshState()
    }

    fun onFileTooLarge() {
        selectingFileSlot = null
        validationError = AdditionalKycValidationError.FileTooLarge
        refreshState()
    }

    fun onFileRemoved(slotIndex: Int): File? {
        if (!canEdit()) {
            return null
        }
        var removedFile: File? = null
        updateDocumentSlot(slotIndex) { slot ->
            removedFile = slot.file?.file
            slot.copy(file = null)
        }
        submissionState = AdditionalKycSubmissionState.Collecting
        validationError = null
        refreshState()
        return removedFile
    }

    fun startSubmission(): AdditionalKycSubmission? {
        val submission = createSubmission() ?: return null
        submissionState = AdditionalKycSubmissionState.Submitting
        validationError = null
        selectingFileSlot = null
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
        selectingFileSlot = null
        submissionState = AdditionalKycSubmissionState.Collecting
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

        return AdditionalKycSubmission(
            liquidityProvider = requirement.requestedBy,
            submissionType = requirement.submissionType,
            documents = if (requirement.submissionType == DOCUMENT_SUBMISSION_TYPE) {
                documentSlots.map { slot ->
                    AdditionalKycDocumentSubmission(
                        documentType = requirement.description,
                        documentSubtype = slot.subtypeId,
                        files = listOf(requireNotNull(slot.file).file),
                    )
                }
            } else {
                null
            },
            questionnaire = requirement.questionnaire?.let { questionnaire ->
                AdditionalKycQuestionnaireSubmission(
                    answers = questionnaire.questions.mapNotNull { question ->
                        answers[question.id]
                            ?.takeIf { answer -> answer.isNotBlank() || question.required }
                            ?.let { answer ->
                                AdditionalKycQuestionnaireAnswer(
                                    questionId = question.id,
                                    value = answer.trim(),
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

    private fun buildState(): AdditionalKycScreenState {
        val requirement = requirement
        val document = requirement?.document
        val selectedSubtypeIds = documentSlots.mapNotNull { slot -> slot.subtypeId }.toSet()

        return AdditionalKycScreenState(
            requirementType = requirement.toRequirementType(),
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
                    slots = documentSlots.map { slot ->
                        AdditionalKycDocumentSlotState(
                            index = slot.index,
                            subtypes = it.acceptedSubtypes.map { subtype ->
                                AdditionalKycDocumentSubtypeState(
                                    id = subtype.id,
                                    label = subtype.label,
                                    isEnabled = subtype.id == slot.subtypeId ||
                                        subtype.id !in selectedSubtypeIds,
                                )
                            },
                            selectedSubtypeLabel = it.acceptedSubtypes
                                .firstOrNull { subtype -> subtype.id == slot.subtypeId }
                                ?.label,
                            fileName = slot.file?.displayName,
                        )
                    },
                )
            },
            validationError = validationError,
            selectingFileSlot = selectingFileSlot,
            canSubmit = canEdit() &&
                isCollectionAvailable() &&
                selectingFileSlot == null &&
                currentValidationError() == null,
            isCollectionAvailable = isCollectionAvailable(),
            submissionState = submissionState,
            currentRequirement = if (userActionRequirements.isEmpty()) 0 else requirementIndex + 1,
            totalRequirements = userActionRequirements.size,
            hasMoreRequirements = requirementIndex < userActionRequirements.lastIndex,
            pendingRequirements = pendingRequirements,
        )
    }

    private fun canEdit(): Boolean {
        return submissionState == AdditionalKycSubmissionState.Collecting ||
            submissionState == AdditionalKycSubmissionState.Failed
    }

    private fun currentValidationError(): AdditionalKycValidationError? {
        val requirement = requirement ?: return null

        val hasMissingAnswers = requirement.questionnaire?.questions.orEmpty().any { question ->
            question.required && answers[question.id].isNullOrBlank()
        }
        if (hasMissingAnswers) {
            return AdditionalKycValidationError.MissingRequiredAnswers
        }

        if (requirement.submissionType == DOCUMENT_SUBMISSION_TYPE) {
            val document = requirement.document ?: return null
            if (document.acceptedSubtypes.isNotEmpty() && documentSlots.any { it.subtypeId == null }) {
                return AdditionalKycValidationError.MissingDocumentType
            }
            if (documentSlots.any { it.file == null }) {
                return AdditionalKycValidationError.MissingDocuments
            }
            val subtypeIds = documentSlots.mapNotNull { slot -> slot.subtypeId }
            if (subtypeIds.size != subtypeIds.distinct().size) {
                return AdditionalKycValidationError.DuplicateDocumentType
            }
        }

        return null
    }

    private fun isCollectionAvailable(): Boolean {
        val requirement = requirement ?: return false
        return when (requirement.submissionType) {
            DOCUMENT_SUBMISSION_TYPE -> {
                val document = requirement.document ?: return false
                val minimumDocuments = document.minDocuments.coerceAtLeast(MINIMUM_DOCUMENT_COUNT)
                document.acceptedSubtypes.isEmpty() ||
                    document.acceptedSubtypes.size >= minimumDocuments
            }
            QUESTIONNAIRE_SUBMISSION_TYPE -> requirement.questionnaire != null
            else -> false
        }
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
            SOURCE_OF_FUNDS -> AdditionalKycRequirementType.SourceOfFunds
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
        private const val MINIMUM_DOCUMENT_COUNT = 1
        private const val BYTES_PER_MEGABYTE = 1_000_000L
        private const val PROOF_OF_ADDRESS_MAX_FILE_SIZE_BYTES = 50L * BYTES_PER_MEGABYTE
        private const val SOURCE_OF_FUNDS_MAX_FILE_SIZE_BYTES = 5L * BYTES_PER_MEGABYTE

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

            val count = requirement.document
                ?.minDocuments
                ?.coerceAtLeast(MINIMUM_DOCUMENT_COUNT)
                ?: return emptyList()
            return List(count) { index ->
                DocumentSlot(index = index, subtypeId = null, file = null)
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
