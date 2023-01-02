package com.stripe.android.identity.navigation

import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.annotation.StringRes
import androidx.annotation.VisibleForTesting
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.stringResource
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import com.stripe.android.identity.networking.models.CollectedDataParam
import com.stripe.android.identity.networking.models.DocumentUploadParam
import com.stripe.android.identity.states.IdentityScanState
import com.stripe.android.identity.ui.DocumentUploadSideInfo
import com.stripe.android.identity.ui.UploadMethod
import com.stripe.android.identity.ui.UploadScreen
import com.stripe.android.identity.utils.IdentityIO
import com.stripe.android.identity.viewmodel.IdentityUploadViewModel
import com.stripe.android.identity.viewmodel.IdentityViewModel

/**
 * Fragment to upload front and back of a document.
 *
 */
internal abstract class IdentityUploadFragment(
    identityIO: IdentityIO,
    private val identityViewModelFactory: ViewModelProvider.Factory
) : Fragment() {

    @get:StringRes
    abstract val titleRes: Int

    @get:StringRes
    abstract val contextRes: Int

    @get:StringRes
    abstract val frontTextRes: Int

    @get:StringRes
    open var backTextRes: Int? = null

    @get:StringRes
    abstract val frontCheckMarkContentDescription: Int

    @get:StringRes
    open var backCheckMarkContentDescription: Int? = null

    abstract val destinationRoute: IdentityTopLevelDestination.DestinationRoute

    abstract val frontScanType: IdentityScanState.ScanType

    open var backScanType: IdentityScanState.ScanType? = null

    abstract val collectedDataParamType: CollectedDataParam.Type

    @VisibleForTesting
    internal var identityUploadViewModelFactory: ViewModelProvider.Factory =
        IdentityUploadViewModel.FrontBackUploadViewModelFactory(identityIO)

    private val identityUploadViewModel: IdentityUploadViewModel by viewModels {
        identityUploadViewModelFactory
    }

    protected val identityViewModel: IdentityViewModel by activityViewModels { identityViewModelFactory }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Note: this could not be moved to a LaunchedEffect as activityResultCaller needs to be
        // called in Fragment.onCreate.
        // TODO(ccen): move it to Activity.onCreate when the fragment is removed.
        identityUploadViewModel.registerActivityResultCaller(
            activityResultCaller = this,
            onFrontPhotoTaken = {
                uploadResult(
                    uri = it,
                    uploadMethod = DocumentUploadParam.UploadMethod.MANUALCAPTURE,
                    isFront = true,
                    scanType = frontScanType
                )
            },
            onBackPhotoTaken = {
                uploadResult(
                    uri = it,
                    uploadMethod = DocumentUploadParam.UploadMethod.MANUALCAPTURE,
                    isFront = false,
                    scanType = requireNotNull(backScanType) { "null backScanType" }
                )
            },
            onFrontImageChosen = {
                uploadResult(
                    uri = it,
                    uploadMethod = DocumentUploadParam.UploadMethod.FILEUPLOAD,
                    isFront = true,
                    scanType = frontScanType
                )
            },
            onBackImageChosen = {
                uploadResult(
                    uri = it,
                    uploadMethod = DocumentUploadParam.UploadMethod.FILEUPLOAD,
                    isFront = false,
                    scanType = requireNotNull(backScanType) { "null backScanType" }
                )
            }
        )
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ) = ComposeView(requireContext()).apply {
        setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
        setContent {
            UploadScreen(
                navController = findNavController(),
                identityViewModel = identityViewModel,
                collectedDataParamType = collectedDataParamType,
                route = destinationRoute.route,
                title = stringResource(id = titleRes),
                context = stringResource(id = contextRes),
                frontInfo =
                DocumentUploadSideInfo(
                    stringResource(id = frontTextRes),
                    stringResource(id = frontCheckMarkContentDescription),
                    frontScanType
                ) { uploadMethod ->
                    when (uploadMethod) {
                        UploadMethod.TAKE_PHOTO -> {
                            identityUploadViewModel.takePhotoFront(requireContext())
                        }
                        UploadMethod.CHOOSE_PHOTO -> {
                            identityUploadViewModel.chooseImageFront()
                        }
                    }
                },
                backInfo =
                if (backTextRes != null && backCheckMarkContentDescription != null && backScanType != null) {
                    DocumentUploadSideInfo(
                        stringResource(id = backTextRes!!),
                        stringResource(id = backCheckMarkContentDescription!!),
                        backScanType!!
                    ) { uploadMethod ->
                        when (uploadMethod) {
                            UploadMethod.TAKE_PHOTO -> {
                                identityUploadViewModel.takePhotoBack(requireContext())
                            }
                            UploadMethod.CHOOSE_PHOTO -> {
                                identityUploadViewModel.chooseImageBack()
                            }
                        }
                    }
                } else {
                    null
                },
                shouldShowTakePhoto = requireNotNull(arguments).getBoolean(
                    ARG_SHOULD_SHOW_TAKE_PHOTO
                ),
                shouldShowChoosePhoto = requireNotNull(arguments).getBoolean(
                    ARG_SHOULD_SHOW_CHOOSE_PHOTO
                )
            )
        }
    }

    private fun uploadResult(
        uri: Uri,
        uploadMethod: DocumentUploadParam.UploadMethod,
        isFront: Boolean,
        scanType: IdentityScanState.ScanType
    ) {
        identityViewModel.requireVerificationPage(
            lifecycleOwner = viewLifecycleOwner,
            navController = findNavController()
        ) {
            identityViewModel.uploadManualResult(
                uri = uri,
                isFront = isFront,
                docCapturePage = it.documentCapture,
                uploadMethod = uploadMethod,
                scanType = scanType
            )
        }
    }

    companion object {
        val TAG: String = IdentityUploadFragment::class.java.simpleName
    }
}
