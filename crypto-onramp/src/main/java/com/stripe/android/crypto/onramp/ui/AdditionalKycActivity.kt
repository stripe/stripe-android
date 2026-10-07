package com.stripe.android.crypto.onramp.ui

import android.Manifest
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.os.Parcelable
import android.provider.MediaStore
import android.provider.OpenableColumns
import android.webkit.MimeTypeMap
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContract
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.core.os.BundleCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.stripe.android.crypto.onramp.AdditionalKycSubmissionHandler
import com.stripe.android.crypto.onramp.AdditionalKycSubmissionHandlerRegistry
import com.stripe.android.crypto.onramp.R
import com.stripe.android.crypto.onramp.model.AdditionalKycRequirements
import com.stripe.android.link.LinkAppearance
import com.stripe.android.uicore.utils.fadeOut
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.parcelize.Parcelize
import java.io.File
import java.io.IOException
import java.util.Locale

internal class AdditionalKycActivity : ComponentActivity() {
    private var fileSelectionJob: Job? = null
    private var sourceDialog: AlertDialog? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val args = intent.extras?.let {
            BundleCompat.getParcelable(it, EXTRA_ARGS, AdditionalKycArgs::class.java)
        } ?: error("Missing AdditionalKycArgs")

        enableEdgeToEdge()

        val factory = object : ViewModelProvider.Factory {
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                @Suppress("UNCHECKED_CAST")
                return KycViewModel(AdditionalKycStateHolder(args.requirements)) as T
            }
        }
        val viewModel = ViewModelProvider(this, factory)[KycViewModel::class.java]
        setContent {
            AdditionalKycActivityContent(
                stateHolder = viewModel.stateHolder,
                args = args,
                submissionHandler = AdditionalKycSubmissionHandlerRegistry[args.submissionHandlerKey]
                    ?: missingSubmissionHandler(args.submissionHandlerKey),
            )
        }
    }

    @Composable
    private fun AdditionalKycActivityContent(
        stateHolder: AdditionalKycStateHolder,
        args: AdditionalKycArgs,
        submissionHandler: AdditionalKycSubmissionHandler,
    ) {
        val scope = rememberCoroutineScope()
        val chooseFile = rememberFilePicker(stateHolder, args.submissionHandlerKey)

        AdditionalKycScreen(
            appearance = args.appearance,
            state = stateHolder.state,
            onClose = { cancel(stateHolder) },
            onBack = {
                fileSelectionJob?.cancel()
                if (!stateHolder.onBack()) {
                    cancel(stateHolder)
                }
            },
            onQuestionAnswerChanged = stateHolder::onQuestionAnswerChanged,
            onDocumentSubtypeSelected = stateHolder::onDocumentSubtypeSelected,
            onChooseFile = chooseFile,
            onRemoveFile = { slotIndex ->
                stateHolder.onFileRemoved(slotIndex)?.delete()
            },
            onAddDocuments = stateHolder::onAddDocuments,
            onEditDocuments = stateHolder::onEditDocuments,
            onSubmit = {
                scope.launch {
                    submit(
                        stateHolder = stateHolder,
                        submissionHandler = submissionHandler,
                    )
                }
            },
            onContinue = {
                if (stateHolder.state.submissionState == AdditionalKycSubmissionState.Submitted) {
                    if (!stateHolder.advanceToNextRequirement()) {
                        finishSubmitted()
                    }
                } else {
                    stateHolder.onContinue()
                }
            },
        )
    }

    @Composable
    private fun rememberFilePicker(
        stateHolder: AdditionalKycStateHolder,
        handlerKey: String,
    ): (Int) -> Unit {
        val scope = rememberCoroutineScope()
        val takePhoto = rememberCameraPicker(stateHolder, handlerKey)
        var pendingFileSlot by rememberSaveable { mutableStateOf<Int?>(null) }
        val filePicker = rememberLauncherForActivityResult(
            contract = ActivityResultContracts.OpenDocument(),
        ) { uri ->
            val slotIndex = pendingFileSlot
            pendingFileSlot = null

            if (uri == null || slotIndex == null) {
                stateHolder.onFileSelectionCancelled()
            } else {
                fileSelectionJob?.cancel()
                fileSelectionJob = scope.launch {
                    handleSelectedFile(
                        uri = uri,
                        slotIndex = slotIndex,
                        stateHolder = stateHolder,
                        handlerKey = handlerKey,
                    )
                }
            }
        }

        return chooseFile@{ slotIndex ->
            if (!stateHolder.canSelectFile(slotIndex)) {
                return@chooseFile
            }
            fileSelectionJob?.cancel()
            stateHolder.onFileSelectionStarted(slotIndex)
            val chooseExisting = {
                pendingFileSlot = slotIndex
                filePicker.launch(acceptedMimeTypes(stateHolder.acceptedFormats))
            }
            val cameraAvailable = additionalKycCameraFormat(stateHolder.acceptedFormats) != null &&
                Intent(MediaStore.ACTION_IMAGE_CAPTURE).resolveActivity(packageManager) != null
            if (cameraAvailable) {
                var selected = false
                sourceDialog = AlertDialog.Builder(this)
                    .setTitle(R.string.stripe_onramp_kyc_document_source)
                    .setItems(
                        arrayOf(
                            getString(R.string.stripe_onramp_kyc_take_photo),
                            getString(R.string.stripe_onramp_kyc_choose_file),
                        )
                    ) { _, index ->
                        selected = true
                        if (index == 0) takePhoto(slotIndex) else chooseExisting()
                    }
                    .setOnDismissListener {
                        if (!selected) stateHolder.onFileSelectionCancelled()
                        sourceDialog = null
                    }
                    .show()
            } else {
                chooseExisting()
            }
        }
    }

    @Composable
    @Suppress("LongMethod") // Keep the two activity-result launchers with their shared pending capture state.
    private fun rememberCameraPicker(
        stateHolder: AdditionalKycStateHolder,
        handlerKey: String,
    ): (Int) -> Unit {
        val scope = rememberCoroutineScope()
        var pendingPath by rememberSaveable { mutableStateOf<String?>(null) }
        var pendingSlot by rememberSaveable { mutableStateOf<Int?>(null) }
        DisposableEffect(Unit) {
            onDispose {
                if (!isChangingConfigurations) pendingPath?.let { File(it).delete() }
            }
        }
        val camera = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { success ->
            val source = pendingPath?.let(::File)
            val slot = pendingSlot
            pendingPath = null
            pendingSlot = null
            if (!success || source == null || slot == null || stateHolder.state.selectingFileSlot != slot) {
                source?.delete()
                stateHolder.onFileSelectionCancelled()
            } else {
                fileSelectionJob?.cancel()
                fileSelectionJob = scope.launch(start = CoroutineStart.UNDISPATCHED) {
                    handleCameraImage(source, slot, stateHolder, handlerKey)
                }
            }
        }
        fun clearPendingCapture() {
            pendingPath?.let { File(it).delete() }
            pendingPath = null
            pendingSlot = null
        }
        fun captureFailed() {
            clearPendingCapture()
            stateHolder.onFileSelectionFailed()
        }
        val launchCamera = {
            try {
                val directory = File(cacheDir, "stripe-onramp-camera").apply { mkdirs() }
                val file = File.createTempFile("capture-", ".jpg", directory)
                pendingPath = file.path
                camera.launch(
                    FileProvider.getUriForFile(this, "$packageName.stripe.onramp.kyc.fileprovider", file)
                )
            } catch (_: Exception) {
                captureFailed()
            }
        }
        val permission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (granted) {
                launchCamera()
            } else {
                clearPendingCapture()
                stateHolder.onFileSelectionCancelled()
                showCameraPermissionDenied()
            }
        }
        return { slot ->
            pendingSlot = slot
            try {
                if (needsCameraPermission()) permission.launch(Manifest.permission.CAMERA) else launchCamera()
            } catch (_: Exception) {
                captureFailed()
            }
        }
    }

    private fun showCameraPermissionDenied() {
        sourceDialog = AlertDialog.Builder(this)
            .setMessage(R.string.stripe_onramp_kyc_camera_permission)
            .setPositiveButton(android.R.string.ok, null)
            .show()
    }

    @Suppress("DEPRECATION")
    private fun needsCameraPermission(): Boolean {
        // Delegating to a camera app requires no permission unless the host declares CAMERA itself.
        val declared = packageManager.getPackageInfo(packageName, PackageManager.GET_PERMISSIONS)
            .requestedPermissions.orEmpty().contains(Manifest.permission.CAMERA)
        return declared && ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) !=
            PackageManager.PERMISSION_GRANTED
    }

    private suspend fun handleCameraImage(
        source: File,
        slot: Int,
        stateHolder: AdditionalKycStateHolder,
        handlerKey: String,
    ) {
        var prepared: File? = null
        try {
            val format = additionalKycCameraFormat(stateHolder.acceptedFormats)
                ?: throw IOException("Camera format is no longer accepted")
            if (stateHolder.state.selectingFileSlot != slot) return
            stateHolder.onFileUploadStarted(slot, "photo.$format")
            val file = withContext(Dispatchers.IO) {
                prepareAdditionalKycCameraImage(
                    source, format, stateHolder.maximumFileSizeBytes ?: Long.MAX_VALUE,
                ).also { prepared = it }
            }
            uploadSelectedFile(SelectedFile(file, "photo.$format", format), slot, stateHolder, handlerKey)
        } catch (error: CancellationException) {
            stateHolder.onFileSelectionCancelled()
            throw error
        } catch (_: AdditionalKycFileTooLargeException) {
            stateHolder.onFileTooLarge()
        } catch (_: Exception) {
            stateHolder.onFileSelectionFailed()
        } finally {
            prepared?.delete()
            source.delete()
        }
    }

    private suspend fun handleSelectedFile(
        uri: Uri,
        slotIndex: Int,
        stateHolder: AdditionalKycStateHolder,
        handlerKey: String,
    ) {
        val displayName = withContext(Dispatchers.IO) {
            runCatching { selectedFileName(uri) }
        }.getOrElse {
            stateHolder.onFileSelectionFailed()
            return
        }
        stateHolder.onFileUploadStarted(slotIndex, displayName)
        var localCopy: File? = null
        try {
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
                        uploadSelectedFile(selection, slotIndex, stateHolder, handlerKey)
                    } else {
                        selection.file.delete()
                    }
                },
                onFailure = { error ->
                    if (error is AdditionalKycFileTooLargeException) {
                        stateHolder.onFileTooLarge()
                    } else {
                        stateHolder.onFileSelectionFailed()
                    }
                },
            )
        } finally {
            localCopy?.delete()
        }
    }

    private suspend fun uploadSelectedFile(
        selection: SelectedFile,
        slotIndex: Int,
        stateHolder: AdditionalKycStateHolder,
        handlerKey: String,
    ) {
        val uploader = AdditionalKycSubmissionHandlerRegistry.uploader(handlerKey)
        val result = uploader?.upload(selection.file)
            ?: Result.failure(IllegalStateException("Missing KYC document uploader"))
        currentCoroutineContext().ensureActive()
        if (stateHolder.state.selectingFileSlot != slotIndex || isFinishing) return
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

    private fun cancel(stateHolder: AdditionalKycStateHolder) {
        if (stateHolder.state.submissionState == AdditionalKycSubmissionState.Submitting) return
        fileSelectionJob?.cancel()
        if (stateHolder.state.submissionState == AdditionalKycSubmissionState.Submitted) {
            finishSubmitted()
            return
        }
        stateHolder.onFileSelectionCancelled()
        stateHolder.currentFiles().forEach { file -> file.delete() }
        setResult(
            RESULT_CANCELED,
            createResultIntent(AdditionalKycScreenAction.Cancelled),
        )
        finish()
    }

    private suspend fun submit(
        stateHolder: AdditionalKycStateHolder,
        submissionHandler: AdditionalKycSubmissionHandler,
    ) {
        val submission = stateHolder.startSubmission() ?: return
        val submittedFiles = stateHolder.currentFiles()

        submissionHandler.submit(submission).fold(
            onSuccess = {
                submittedFiles.forEach { file -> file.delete() }
                stateHolder.onSubmissionSucceeded()
            },
            onFailure = {
                stateHolder.onSubmissionFailed()
            },
        )
    }

    private fun finishSubmitted() {
        setResult(
            RESULT_OK,
            createResultIntent(AdditionalKycScreenAction.Submitted),
        )
        finish()
    }

    private fun missingSubmissionHandler(key: String): AdditionalKycSubmissionHandler {
        return AdditionalKycSubmissionHandler {
            Result.failure(
                IllegalStateException("No additional KYC submission handler registered for key: $key")
            )
        }
    }

    override fun onDestroy() {
        sourceDialog?.dismiss()
        sourceDialog = null
        super.onDestroy()
    }

    override fun finish() {
        super.finish()
        fadeOut()
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
                    copyAdditionalKycFile(
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

    private class KycViewModel(val stateHolder: AdditionalKycStateHolder) : ViewModel()

    private data class SelectedFile(
        val file: File,
        val displayName: String,
        val mimeTypeExtension: String?,
    )

    internal companion object {
        private const val EXTRA_ARGS = "additional_kyc_args"
        private const val ACTION_ARG = "action"
        private const val FILE_NAME_PREFIX = "stripe-onramp-kyc-"
        private const val DEFAULT_FILE_NAME = "document"
        private const val MAX_FILE_EXTENSION_LENGTH = 10

        fun createIntent(
            context: Context,
            args: AdditionalKycArgs,
        ): Intent {
            return Intent(context, AdditionalKycActivity::class.java)
                .putExtra(EXTRA_ARGS, args)
        }

        fun createResultIntent(action: AdditionalKycScreenAction): Intent {
            return Intent().putExtra(ACTION_ARG, action)
        }

        fun argsFrom(intent: Intent): AdditionalKycArgs? {
            return intent.extras?.let {
                BundleCompat.getParcelable(it, EXTRA_ARGS, AdditionalKycArgs::class.java)
            }
        }

        fun actionFrom(intent: Intent?): AdditionalKycScreenAction? {
            return intent?.extras?.let {
                BundleCompat.getParcelable(it, ACTION_ARG, AdditionalKycScreenAction::class.java)
            }
        }

        private fun acceptedMimeTypes(formats: List<String>): Array<String> {
            val mimeTypes = formats.map { format ->
                MimeTypeMap.getSingleton().getMimeTypeFromExtension(
                    format.trim().removePrefix(".").lowercase(Locale.ROOT),
                )
            }

            if (mimeTypes.any { mimeType -> mimeType == null }) {
                return arrayOf(ANY_MIME_TYPE)
            }

            return mimeTypes
                .filterNotNull()
                .distinct()
                .takeIf(List<String>::isNotEmpty)
                ?.toTypedArray()
                ?: arrayOf(ANY_MIME_TYPE)
        }

        private const val ANY_MIME_TYPE = "*/*"
    }
}

internal data class AdditionalKycActivityArgs(
    val requirements: AdditionalKycRequirements,
    val linkAppearance: LinkAppearance?,
    val submissionHandlerKey: String,
)

internal sealed interface AdditionalKycScreenAction : Parcelable {
    @Parcelize
    data object Cancelled : AdditionalKycScreenAction

    @Parcelize
    data object Submitted : AdditionalKycScreenAction
}

internal data class AdditionalKycActivityResult(
    val action: AdditionalKycScreenAction,
)

internal class AdditionalKycActivityContract : ActivityResultContract<
    AdditionalKycActivityArgs,
    AdditionalKycActivityResult
    >() {
    override fun createIntent(context: Context, input: AdditionalKycActivityArgs): Intent {
        return AdditionalKycActivity.createIntent(
            context = context,
            args = AdditionalKycArgs(
                requirements = input.requirements,
                appearance = input.linkAppearance?.build(),
                submissionHandlerKey = input.submissionHandlerKey,
            ),
        )
    }

    override fun parseResult(resultCode: Int, intent: Intent?): AdditionalKycActivityResult {
        val action = AdditionalKycActivity.actionFrom(intent) ?: AdditionalKycScreenAction.Cancelled
        return AdditionalKycActivityResult(action)
    }
}

@Parcelize
internal data class AdditionalKycArgs(
    val requirements: AdditionalKycRequirements,
    val appearance: LinkAppearance.State?,
    val submissionHandlerKey: String,
) : Parcelable
