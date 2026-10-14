package com.stripe.android.crypto.onramp.ui

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import android.webkit.MimeTypeMap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.stripe.android.crypto.onramp.KycDocumentUploader
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException

internal class KycViewModel(
    context: Context,
    val stateHolder: KycStateHolder,
    private val uploader: KycDocumentUploader?,
) : ViewModel() {
    private val contentResolver = context.applicationContext.contentResolver
    private val cacheDir = context.applicationContext.cacheDir
    private var fileSelectionJob: Job? = null
    private var selectionToken: Any? = null

    fun selectFile(uri: Uri, slotIndex: Int) {
        startFileSelection { handleSelectedFile(uri, slotIndex) }
    }

    fun selectCameraImage(source: File, slotIndex: Int) {
        startFileSelection { handleCameraImage(source, slotIndex) }
    }

    fun cancelFileSelection() {
        fileSelectionJob?.cancel()
        fileSelectionJob = null
        selectionToken = null
        stateHolder.onFileSelectionCancelled()
    }

    private fun startFileSelection(block: suspend () -> Unit) {
        // An older cancelled job must not clear a newer selection.
        val token = Any()
        selectionToken = token
        fileSelectionJob?.cancel()
        fileSelectionJob = viewModelScope.launch(start = CoroutineStart.UNDISPATCHED) {
            try {
                block()
            } finally {
                if (selectionToken === token) {
                    selectionToken = null
                    stateHolder.onFileSelectionCancelled()
                }
            }
        }
    }

    private suspend fun handleCameraImage(
        source: File,
        slot: Int,
    ) {
        var prepared: File? = null
        try {
            val format = kycCameraFormat(stateHolder.acceptedFormats)
                ?: throw IOException("Camera format is no longer accepted")
            if (stateHolder.state.selectingFileSlot != slot) return
            stateHolder.onFilePreparationStarted(slot, "photo.$format")
            val file = withContext(Dispatchers.IO) {
                prepareKycCameraImage(
                    source, format, stateHolder.maximumFileSizeBytes ?: Long.MAX_VALUE,
                ).also { prepared = it }
            }
            uploadSelectedFile(SelectedFile(file, "photo.$format", format), slot)
        } catch (error: CancellationException) {
            throw error
        } catch (_: KycFileTooLargeException) {
            currentCoroutineContext().ensureActive()
            stateHolder.onFileTooLarge()
        } catch (_: Exception) {
            currentCoroutineContext().ensureActive()
            stateHolder.onFileSelectionFailed()
        } finally {
            prepared?.delete()
            source.delete()
        }
    }

    private suspend fun handleSelectedFile(
        uri: Uri,
        slotIndex: Int,
    ) {
        var localCopy: File? = null
        try {
            val displayName = withContext(Dispatchers.IO) { selectedFileName(uri) }
            stateHolder.onFilePreparationStarted(slotIndex, displayName)
            val selectedFile = withContext(Dispatchers.IO) {
                readSelectedFile(
                    uri = uri,
                    displayName = displayName,
                    maximumFileSizeBytes = stateHolder.maximumFileSizeBytes,
                ).also { localCopy = it.getOrNull()?.file }
            }
            selectedFile.fold(
                onSuccess = { selection ->
                    if (
                        stateHolder.isAcceptedFileSize(selection.file.length()) &&
                        stateHolder.isAcceptedFile(
                            displayName = selection.displayName,
                            mimeTypeExtension = selection.mimeTypeExtension,
                        )
                    ) {
                        uploadSelectedFile(selection, slotIndex)
                    } else {
                        selection.file.delete()
                    }
                },
                onFailure = { error ->
                    if (error is KycFileTooLargeException) {
                        stateHolder.onFileTooLarge()
                    } else {
                        stateHolder.onFileSelectionFailed()
                    }
                },
            )
        } catch (error: CancellationException) {
            throw error
        } catch (_: Exception) {
            currentCoroutineContext().ensureActive()
            stateHolder.onFileSelectionFailed()
        } finally {
            localCopy?.delete()
        }
    }

    private suspend fun uploadSelectedFile(
        selection: SelectedFile,
        slotIndex: Int,
    ) {
        stateHolder.onFileUploadStarted(slotIndex, selection.displayName)
        val result = uploader?.upload(selection.file)
            ?: Result.failure(IllegalStateException("Missing KYC document uploader"))
        currentCoroutineContext().ensureActive()
        if (stateHolder.state.selectingFileSlot != slotIndex) return
        result.fold(
            onSuccess = { fileId ->
                stateHolder.onFileSelected(
                    slotIndex = slotIndex,
                    file = selection.file,
                    displayName = selection.displayName,
                    fileId = fileId,
                )?.delete()
            },
            onFailure = { error ->
                if (error is CancellationException) throw error
                stateHolder.onFileSelectionFailed()
            },
        )
    }

    private fun readSelectedFile(
        uri: Uri,
        displayName: String,
        maximumFileSizeBytes: Long?,
    ): Result<SelectedFile> = runCatching {
        val mimeTypeExtension = contentResolver.getType(uri)
            ?.let(MimeTypeMap.getSingleton()::getExtensionFromMimeType)
        val suffix = displayName
            .substringAfterLast('.', missingDelimiterValue = "")
            .filter(Char::isLetterOrDigit)
            .take(MAX_FILE_EXTENSION_LENGTH)
            .takeIf(String::isNotEmpty)
            ?.let { extension -> ".$extension" }
        val destination = File.createTempFile(FILE_NAME_PREFIX, suffix, cacheDir)

        copySelectedFile(
            uri = uri,
            destination = destination,
            maximumFileSizeBytes = maximumFileSizeBytes,
        )

        SelectedFile(
            file = destination,
            displayName = displayName,
            mimeTypeExtension = mimeTypeExtension,
        )
    }

    private fun copySelectedFile(
        uri: Uri,
        destination: File,
        maximumFileSizeBytes: Long?,
    ) {
        var copyCompleted = false
        try {
            val inputStream = contentResolver.openInputStream(uri)
                ?: throw IOException("Unable to open the selected file")
            inputStream.use { input ->
                destination.outputStream().use { output ->
                    copyKycFile(
                        input = input,
                        output = output,
                        maximumFileSizeBytes = maximumFileSizeBytes,
                    )
                }
            }
            copyCompleted = true
        } finally {
            if (!copyCompleted) {
                destination.delete()
            }
        }
    }

    private fun selectedFileName(uri: Uri): String {
        val queriedName = contentResolver.query(
            uri,
            arrayOf(OpenableColumns.DISPLAY_NAME),
            null,
            null,
            null,
        )?.use { cursor ->
            val displayNameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            if (displayNameIndex >= 0 && cursor.moveToFirst()) {
                cursor.getString(displayNameIndex)
            } else {
                null
            }
        }

        return queriedName ?: uri.lastPathSegment ?: DEFAULT_FILE_NAME
    }

    private data class SelectedFile(
        val file: File,
        val displayName: String,
        val mimeTypeExtension: String?,
    )

    private companion object {
        const val FILE_NAME_PREFIX = "stripe-onramp-kyc-"
        const val DEFAULT_FILE_NAME = "document"
        const val MAX_FILE_EXTENSION_LENGTH = 10
    }
}
