package com.stripe.android.crypto.onramp.ui

import com.stripe.android.crypto.onramp.model.KycDocumentRequirement
import com.stripe.android.crypto.onramp.model.KycDocumentSubmission
import java.io.File
import java.util.Locale

@Suppress("TooManyFunctions")
internal class KycDocumentCollectionModel(
    private val document: KycDocumentRequirement?,
    private val requirementType: KycRequirementType,
) {
    private var documentSlots = if (document == null) {
        emptyList()
    } else {
        listOf(DocumentSlot(0, document.acceptedSubtypes.firstOrNull()?.id, null))
    }
    var editingDocumentSlot: Int? = if (requirementType == KycRequirementType.ProofOfAddress) 0 else null
        private set
    var validationError: KycValidationError? = null
        private set
    var validationFileName: String? = null
        private set
    var selectingFileSlot: Int? = null
        private set
    var selectingFileName: String? = null
        private set

    val acceptedFormats: List<String> get() = document?.acceptedFormats.orEmpty()
    val maximumFileSizeBytes: Long? get() = document?.maxFileSizeBytes
    val completedDocumentCount: Int get() = documentSlots.count { it.file != null }
    val canContinue: Boolean
        get() = selectingFileSlot == null && documentSlots.any { slot ->
            slot.file != null && slot.subtypeId == documentSlots.firstOrNull {
                it.index == editingDocumentSlot
            }?.subtypeId
        }

    fun clearValidation() {
        validationError = null
        validationFileName = null
    }

    fun finishEditing() {
        discardEmptyDocumentSlots()
        editingDocumentSlot = null
    }

    fun onAddDocuments(): Boolean {
        val selectedTypes = documentSlots.filter { it.file != null }.mapNotNull { it.subtypeId }.toSet()
        val document = document ?: return false
        if (selectedTypes.size >= document.maxDocumentTypes) return false
        val subtype = document.acceptedSubtypes.firstOrNull { it.id !in selectedTypes } ?: return false
        val slot = DocumentSlot(
            index = documentSlots.firstOrNull { it.file == null }?.index ?: nextDocumentSlotIndex(),
            subtypeId = subtype.id,
            file = null,
        )
        discardEmptyDocumentSlots()
        documentSlots = documentSlots + slot
        editingDocumentSlot = slot.index
        validationError = null
        validationFileName = null
        return true
    }

    fun onEditDocuments(slotIndex: Int): Boolean {
        if (document == null) {
            return false
        }
        val selectedSlot = documentSlots.firstOrNull { slot -> slot.index == slotIndex } ?: return false
        val editingSlot = documentSlots.firstOrNull { slot ->
            slot.file == null && slot.subtypeId == selectedSlot.subtypeId
        } ?: DocumentSlot(
            index = nextDocumentSlotIndex(),
            subtypeId = selectedSlot.subtypeId,
            file = null,
        ).also { newSlot -> documentSlots = documentSlots + newSlot }
        editingDocumentSlot = editingSlot.index
        validationError = null
        validationFileName = null
        return true
    }

    fun onDocumentSubtypeSelected(slotIndex: Int, subtypeId: String): Boolean {
        if (selectingFileSlot != null) {
            return false
        }
        val document = document ?: return false
        if (
            document.acceptedSubtypes.none { subtype -> subtype.id == subtypeId } ||
            !canSelectSubtype(slotIndex, subtypeId)
        ) {
            return false
        }

        updateDocumentSlot(slotIndex) { slot -> slot.copy(subtypeId = subtypeId) }
        editingDocumentSlot = slotIndex
        validationError = null
        validationFileName = null
        return true
    }

    fun canSelectFile(slotIndex: Int): Boolean {
        val document = document ?: return false
        val slot = documentSlots.firstOrNull { it.index == slotIndex } ?: return false
        return slot.subtypeId != null && documentSlots.count {
            it.index != slotIndex && it.subtypeId == slot.subtypeId && it.file != null
        } < document.maxFilesPerDocumentType
    }

    fun onFileSelectionStarted(slotIndex: Int): Boolean {
        if (!canSelectFile(slotIndex)) {
            return false
        }

        selectingFileSlot = slotIndex
        selectingFileName = null
        validationError = null
        validationFileName = null
        return true
    }

    fun onFileUploadStarted(slotIndex: Int, displayName: String) {
        if (selectingFileSlot != slotIndex) {
            return
        }
        selectingFileName = displayName
    }

    fun onFileSelectionCancelled() {
        selectingFileSlot = null
        selectingFileName = null
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
            validationError = KycValidationError.UnsupportedFileType
            validationFileName = displayName
        }
        return isAccepted
    }

    fun isAcceptedFileSize(fileSizeBytes: Long): Boolean {
        val maximumBytes = maximumFileSizeBytes ?: return true
        val isAccepted = fileSizeBytes <= maximumBytes
        if (!isAccepted) {
            onFileTooLarge(null)
        }
        return isAccepted
    }

    fun onFileSelected(slotIndex: Int, file: File, displayName: String, fileId: String): File? {
        if (!canSelectFile(slotIndex)) {
            onFileSelectionCancelled()
            return file
        }
        var replacedFile: File? = null
        updateDocumentSlot(slotIndex) { slot ->
            replacedFile = slot.file?.file
            slot.copy(file = SelectedKycFile(file = file, displayName = displayName, fileId = fileId))
        }
        selectingFileSlot = null
        selectingFileName = null
        validationError = null
        validationFileName = null
        addNextUploadSlotIfNeeded(completedSlotIndex = slotIndex)
        return replacedFile
    }

    fun onFileSelectionFailed() {
        selectingFileSlot = null
        selectingFileName = null
        validationError = KycValidationError.FileUnavailable
        validationFileName = null
    }

    fun onFileTooLarge(displayName: String?) {
        selectingFileSlot = null
        validationError = KycValidationError.FileTooLarge
        validationFileName = displayName ?: selectingFileName
        selectingFileName = null
    }

    fun onFileRemoved(slotIndex: Int, isEditing: Boolean): File? {
        val slot = documentSlots.firstOrNull { it.index == slotIndex } ?: return null
        val removedFile = slot.file?.file
        documentSlots = if (slot.file == null || documentSlots.count { it.file != null } > 1) {
            documentSlots.filterNot { it.index == slotIndex }
        } else {
            documentSlots.map { candidate ->
                if (candidate.index == slotIndex) candidate.copy(file = null) else candidate
            }
        }
        ensureEditingSlot(isEditing)
        validationError = null
        validationFileName = null
        return removedFile
    }

    fun containsSlot(slotIndex: Int): Boolean = documentSlots.any { it.index == slotIndex }

    fun currentFiles(): List<File> {
        return documentSlots.mapNotNull { slot -> slot.file?.file }
    }

    private fun canSelectSubtype(slotIndex: Int, subtypeId: String): Boolean {
        val document = document ?: return false
        val selectedTypes = documentSlots.filter { it.index != slotIndex && it.file != null }
            .mapNotNull { it.subtypeId }.toSet()
        val fileCount = documentSlots.count {
            it.index != slotIndex && it.subtypeId == subtypeId && it.file != null
        }
        return fileCount < document.maxFilesPerDocumentType &&
            (subtypeId in selectedTypes || selectedTypes.size < document.maxDocumentTypes)
    }

    private fun addNextUploadSlotIfNeeded(completedSlotIndex: Int) {
        val completedDocumentCount = documentSlots.filter { it.file != null }
            .mapNotNull { it.subtypeId }.distinct().size
        val minimumDocumentCount = document
            ?.minDocumentTypes
            ?.coerceAtLeast(MINIMUM_DOCUMENT_COUNT)
            ?: MINIMUM_DOCUMENT_COUNT
        if (
            requirementType == KycRequirementType.ProofOfAddress &&
            completedDocumentCount >= minimumDocumentCount
        ) {
            editingDocumentSlot = completedSlotIndex
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

    private fun ensureEditingSlot(isEditing: Boolean) {
        val emptySlot = documentSlots.firstOrNull { slot -> slot.file == null }
        if (!isEditing) {
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

    private fun subtypeLabel(subtypeId: String?): String? {
        return document?.acceptedSubtypes
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

    private data class DocumentSlot(
        val index: Int,
        val subtypeId: String?,
        val file: SelectedKycFile?,
    )

    private data class SelectedKycFile(
        val file: File,
        val displayName: String,
        val fileId: String,
    )

    fun createSubmission(): List<KycDocumentSubmission> {
        val completedSlots = documentSlots.filter { it.file != null }
        return if (document != null) {
            completedSlots.groupBy { it.subtypeId }.map { (subtypeId, slots) ->
                KycDocumentSubmission(
                    documentSubtype = requireNotNull(subtypeId),
                    files = emptyList(),
                    uploadedFileIds = slots.map { requireNotNull(it.file).fileId },
                )
            }
        } else {
            emptyList()
        }
    }

    fun buildState(): KycDocumentState? = document?.let {
        KycDocumentState(
            acceptedFormats = it.acceptedFormats,
            instructions = it.instructions,
            fileRequirements = it.fileRequirements,
            maxFileSizeMegabytes = maximumFileSizeBytes
                ?.div(BYTES_PER_MEGABYTE)
                ?.toInt(),
            minDocumentTypes = it.minDocumentTypes.coerceAtLeast(MINIMUM_DOCUMENT_COUNT),
            maxDocumentTypes = it.maxDocumentTypes,
            maxFilesPerDocumentType = it.maxFilesPerDocumentType,
            editingSlotIndex = editingDocumentSlot,
            slots = documentSlots.map { slot ->
                KycDocumentSlotState(
                    index = slot.index,
                    subtypes = it.acceptedSubtypes.map { subtype ->
                        KycDocumentSubtypeState(
                            id = subtype.id,
                            label = subtype.label,
                            description = subtype.description,
                            isEnabled = canSelectSubtype(slot.index, subtype.id),
                        )
                    },
                    selectedSubtypeId = slot.subtypeId,
                    selectedSubtypeLabel = subtypeLabel(slot.subtypeId),
                    fileName = slot.file?.displayName,
                )
            },
        )
    }

    fun currentValidationError(): KycValidationError? {
        if (document != null) {
            val completedSlots = documentSlots.filter { slot -> slot.file != null }
            val completedTypes = completedSlots.mapNotNull { it.subtypeId }.toSet()
            val exceedsFileLimit = completedSlots.groupingBy { it.subtypeId }.eachCount().values.any {
                it > document.maxFilesPerDocumentType
            }
            val missingProofOfAddress = requirementType == KycRequirementType.ProofOfAddress &&
                completedSlots.isEmpty()
            if (completedTypes.size < document.minDocumentTypes ||
                completedTypes.size > document.maxDocumentTypes ||
                exceedsFileLimit ||
                missingProofOfAddress
            ) {
                return KycValidationError.MissingDocuments
            }
            if (document.acceptedSubtypes.isNotEmpty() && completedSlots.any { it.subtypeId == null }) {
                return KycValidationError.MissingDocumentType
            }
        }

        return null
    }

    private fun normalizeExtension(extension: String): String {
        return when (extension.trim().removePrefix(".").lowercase(Locale.ROOT)) {
            "jpg" -> "jpeg"
            "tif" -> "tiff"
            else -> extension.trim().removePrefix(".").lowercase(Locale.ROOT)
        }
    }

    private companion object {
        const val MINIMUM_DOCUMENT_COUNT = 1
        const val BYTES_PER_MEGABYTE = 1_000_000L
    }
}
