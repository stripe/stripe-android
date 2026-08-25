package com.stripe.android.crypto.onramp.ui

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Parcelable
import android.provider.OpenableColumns
import android.webkit.MimeTypeMap
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContract
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.core.os.BundleCompat
import com.stripe.android.crypto.onramp.AdditionalKycSubmissionHandler
import com.stripe.android.crypto.onramp.AdditionalKycSubmissionHandlerRegistry
import com.stripe.android.crypto.onramp.model.AdditionalKycRequirements
import com.stripe.android.link.LinkAppearance
import com.stripe.android.link.onramp.ui.AdditionalKycScreen
import com.stripe.android.uicore.utils.fadeOut
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.parcelize.Parcelize
import java.io.File
import java.io.IOException
import java.util.Locale

internal class AdditionalKycActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val args = intent.extras?.let {
            BundleCompat.getParcelable(it, EXTRA_ARGS, AdditionalKycArgs::class.java)
        } ?: error("Missing AdditionalKycArgs")

        enableEdgeToEdge()

        setContent {
            AdditionalKycActivityContent(
                args = args,
                submissionHandler = AdditionalKycSubmissionHandlerRegistry[args.submissionHandlerKey]
                    ?: missingSubmissionHandler(args.submissionHandlerKey),
            )
        }
    }

    @Composable
    private fun AdditionalKycActivityContent(
        args: AdditionalKycArgs,
        submissionHandler: AdditionalKycSubmissionHandler,
    ) {
        val stateHolder = remember(args.requirements) {
            AdditionalKycStateHolder(args.requirements)
        }
        val scope = rememberCoroutineScope()
        val chooseFile = rememberFilePicker(stateHolder)

        AdditionalKycScreen(
            appearance = args.appearance,
            state = stateHolder.state,
            onClose = { cancel(stateHolder) },
            onQuestionAnswerChanged = stateHolder::onQuestionAnswerChanged,
            onDocumentSubtypeSelected = stateHolder::onDocumentSubtypeSelected,
            onChooseFile = chooseFile,
            onRemoveFile = { slotIndex ->
                stateHolder.onFileRemoved(slotIndex)?.delete()
            },
            onSubmit = {
                scope.launch {
                    submit(
                        stateHolder = stateHolder,
                        submissionHandler = submissionHandler,
                    )
                }
            },
            onContinue = {
                if (!stateHolder.advanceToNextRequirement()) {
                    finishSubmitted()
                }
            },
        )
    }

    @Composable
    private fun rememberFilePicker(stateHolder: AdditionalKycStateHolder): (Int) -> Unit {
        val scope = rememberCoroutineScope()
        var pendingFileSlot by remember { mutableStateOf<Int?>(null) }
        val filePicker = rememberLauncherForActivityResult(
            contract = ActivityResultContracts.OpenDocument(),
        ) { uri ->
            val slotIndex = pendingFileSlot
            pendingFileSlot = null

            if (uri == null || slotIndex == null) {
                stateHolder.onFileSelectionCancelled()
            } else {
                scope.launch {
                    handleSelectedFile(
                        uri = uri,
                        slotIndex = slotIndex,
                        stateHolder = stateHolder,
                    )
                }
            }
        }

        return { slotIndex ->
            stateHolder.onFileSelectionStarted(slotIndex)
            pendingFileSlot = slotIndex
            filePicker.launch(acceptedMimeTypes(stateHolder.acceptedFormats))
        }
    }

    private suspend fun handleSelectedFile(
        uri: Uri,
        slotIndex: Int,
        stateHolder: AdditionalKycStateHolder,
    ) {
        val selectedFile = withContext(Dispatchers.IO) {
            readSelectedFile(
                uri = uri,
                maximumFileSizeBytes = stateHolder.maximumFileSizeBytes,
            )
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
                    stateHolder.onFileSelected(
                        slotIndex = slotIndex,
                        file = selection.file,
                        displayName = selection.displayName,
                    )?.delete()
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
    }

    private fun cancel(stateHolder: AdditionalKycStateHolder) {
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

    override fun finish() {
        super.finish()
        fadeOut()
    }

    private fun readSelectedFile(
        uri: Uri,
        maximumFileSizeBytes: Long?,
    ): Result<SelectedFile> = runCatching {
        val displayName = selectedFileName(uri)
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
