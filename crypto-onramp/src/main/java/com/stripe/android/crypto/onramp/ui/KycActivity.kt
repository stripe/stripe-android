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
import android.webkit.MimeTypeMap
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.PickVisualMediaRequest
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
import com.stripe.android.crypto.onramp.KycSubmissionHandler
import com.stripe.android.crypto.onramp.KycSubmissionHandlerRegistry
import com.stripe.android.crypto.onramp.R
import com.stripe.android.crypto.onramp.model.KycRequirements
import com.stripe.android.link.LinkAppearance
import com.stripe.android.uicore.utils.fadeOut
import kotlinx.coroutines.launch
import kotlinx.parcelize.Parcelize
import java.io.File
import java.util.Locale

internal class KycActivity : ComponentActivity() {
    private lateinit var viewModel: KycViewModel
    private var sourceDialog: AlertDialog? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val args = intent.extras?.let {
            BundleCompat.getParcelable(it, EXTRA_ARGS, KycArgs::class.java)
        } ?: error("Missing KycArgs")

        enableEdgeToEdge()

        val factory = object : ViewModelProvider.Factory {
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                @Suppress("UNCHECKED_CAST")
                return KycViewModel(
                    context = applicationContext,
                    stateHolder = KycStateHolder(args.requirements),
                    uploader = KycSubmissionHandlerRegistry.uploader(args.submissionHandlerKey),
                ) as T
            }
        }
        viewModel = ViewModelProvider(this, factory)[KycViewModel::class.java]
        setContent {
            KycActivityContent(
                stateHolder = viewModel.stateHolder,
                args = args,
                submissionHandler = KycSubmissionHandlerRegistry[args.submissionHandlerKey]
                    ?: missingSubmissionHandler(args.submissionHandlerKey),
            )
        }
    }

    @Composable
    private fun KycActivityContent(
        stateHolder: KycStateHolder,
        args: KycArgs,
        submissionHandler: KycSubmissionHandler,
    ) {
        val scope = rememberCoroutineScope()
        val chooseFile = rememberFilePicker(stateHolder)

        KycScreen(
            appearance = args.appearance,
            state = stateHolder.state,
            onClose = { cancel(stateHolder) },
            onBack = {
                viewModel.cancelFileSelection()
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
                if (stateHolder.state.submissionState == KycSubmissionState.Submitted) {
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
    @Suppress("LongMethod") // Keep both picker launchers with their shared pending selection state.
    private fun rememberFilePicker(
        stateHolder: KycStateHolder,
    ): (Int) -> Unit {
        val takePhoto = rememberCameraPicker(stateHolder)
        var pendingFileSlot by rememberSaveable { mutableStateOf<Int?>(null) }
        val onPicked: (Uri?) -> Unit = { uri ->
            val slotIndex = pendingFileSlot
            pendingFileSlot = null

            if (uri == null || slotIndex == null) {
                stateHolder.onFileSelectionCancelled()
            } else {
                viewModel.selectFile(uri, slotIndex)
            }
        }
        val filePicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument(), onPicked)
        val photoPicker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia(), onPicked)

        return chooseFile@{ slotIndex ->
            if (!stateHolder.canSelectFile(slotIndex)) {
                return@chooseFile
            }
            viewModel.cancelFileSelection()
            stateHolder.onFileSelectionStarted(slotIndex)
            val chooseExisting = {
                pendingFileSlot = slotIndex
                filePicker.launch(acceptedMimeTypes(stateHolder.acceptedFormats))
            }
            val cameraAvailable = kycCameraFormat(stateHolder.acceptedFormats) != null &&
                Intent(MediaStore.ACTION_IMAGE_CAPTURE).resolveActivity(packageManager) != null
            val photoMediaType = kycPhotoPickerMediaType(stateHolder.acceptedFormats)
            val sources = buildList {
                if (cameraAvailable) {
                    add(R.string.stripe_onramp_kyc_take_photo to { takePhoto(slotIndex) })
                }
                if (photoMediaType != null) {
                    add(
                        R.string.stripe_onramp_kyc_choose_photo to {
                            pendingFileSlot = slotIndex
                            photoPicker.launch(PickVisualMediaRequest(photoMediaType))
                        }
                    )
                }
                add(R.string.stripe_onramp_kyc_choose_file to chooseExisting)
            }
            if (sources.size > 1) {
                var selected = false
                sourceDialog = AlertDialog.Builder(this)
                    .setTitle(R.string.stripe_onramp_kyc_document_source)
                    .setItems(
                        sources.map { (label, _) -> getString(label) }.toTypedArray()
                    ) { _, index ->
                        selected = true
                        sources[index].second()
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
        stateHolder: KycStateHolder,
    ): (Int) -> Unit {
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
                viewModel.selectCameraImage(source, slot)
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

    private fun cancel(stateHolder: KycStateHolder) {
        if (stateHolder.state.submissionState == KycSubmissionState.Submitting) return
        viewModel.cancelFileSelection()
        if (stateHolder.state.submissionState == KycSubmissionState.Submitted) {
            finishSubmitted()
            return
        }
        stateHolder.onFileSelectionCancelled()
        stateHolder.currentFiles().forEach { file -> file.delete() }
        setResult(
            RESULT_CANCELED,
            createResultIntent(KycScreenAction.Cancelled),
        )
        finish()
    }

    private suspend fun submit(
        stateHolder: KycStateHolder,
        submissionHandler: KycSubmissionHandler,
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
            createResultIntent(KycScreenAction.Submitted),
        )
        finish()
    }

    private fun missingSubmissionHandler(key: String): KycSubmissionHandler {
        return KycSubmissionHandler {
            Result.failure(
                IllegalStateException("No KYC submission handler registered for key: $key")
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
        fadeOut(
            fadeIn = 0,
            fadeOut = R.anim.stripe_onramp_kyc_exit,
        )
    }

    internal companion object {
        private const val EXTRA_ARGS = "kyc_args"
        private const val ACTION_ARG = "action"

        fun createIntent(
            context: Context,
            args: KycArgs,
        ): Intent {
            return Intent(context, KycActivity::class.java)
                .putExtra(EXTRA_ARGS, args)
        }

        fun createResultIntent(action: KycScreenAction): Intent {
            return Intent().putExtra(ACTION_ARG, action)
        }

        fun argsFrom(intent: Intent): KycArgs? {
            return intent.extras?.let {
                BundleCompat.getParcelable(it, EXTRA_ARGS, KycArgs::class.java)
            }
        }

        fun actionFrom(intent: Intent?): KycScreenAction? {
            return intent?.extras?.let {
                BundleCompat.getParcelable(it, ACTION_ARG, KycScreenAction::class.java)
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

internal data class KycActivityArgs(
    val requirements: KycRequirements,
    val linkAppearance: LinkAppearance?,
    val submissionHandlerKey: String,
)

internal sealed interface KycScreenAction : Parcelable {
    @Parcelize
    data object Cancelled : KycScreenAction

    @Parcelize
    data object Submitted : KycScreenAction
}

internal data class KycActivityResult(
    val action: KycScreenAction,
)

internal class KycActivityContract : ActivityResultContract<
    KycActivityArgs,
    KycActivityResult
    >() {
    override fun createIntent(context: Context, input: KycActivityArgs): Intent {
        return KycActivity.createIntent(
            context = context,
            args = KycArgs(
                requirements = input.requirements,
                appearance = input.linkAppearance?.build(),
                submissionHandlerKey = input.submissionHandlerKey,
            ),
        )
    }

    override fun parseResult(resultCode: Int, intent: Intent?): KycActivityResult {
        val action = KycActivity.actionFrom(intent) ?: KycScreenAction.Cancelled
        return KycActivityResult(action)
    }
}

@Parcelize
internal data class KycArgs(
    val requirements: KycRequirements,
    val appearance: LinkAppearance.State?,
    val submissionHandlerKey: String,
) : Parcelable
