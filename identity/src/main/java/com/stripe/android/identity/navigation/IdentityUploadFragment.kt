package com.stripe.android.identity.navigation

import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.IdRes
import androidx.annotation.StringRes
import androidx.annotation.VisibleForTesting
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.stringResource
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.stripe.android.identity.networking.models.CollectedDataParam
import com.stripe.android.identity.networking.models.DocumentUploadParam
import com.stripe.android.identity.networking.models.VerificationPageStaticContentDocumentCapturePage
import com.stripe.android.identity.states.IdentityScanState
import com.stripe.android.identity.ui.DocumentUploadSideInfo
import com.stripe.android.identity.ui.UploadMethod
import com.stripe.android.identity.ui.UploadScreen
import com.stripe.android.identity.utils.IdentityIO
import com.stripe.android.identity.utils.fragmentIdToScreenName
import com.stripe.android.identity.utils.navigateToDefaultErrorFragment
import com.stripe.android.identity.utils.navigateToSelfieOrSubmit
import com.stripe.android.identity.utils.postVerificationPageData
import com.stripe.android.identity.viewmodel.IdentityUploadViewModel
import com.stripe.android.identity.viewmodel.IdentityViewModel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

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

    @get:IdRes
    abstract val fragmentId: Int

    abstract val route: String

    abstract val frontScanType: IdentityScanState.ScanType

    open var backScanType: IdentityScanState.ScanType? = null

    abstract val collectedDataParamType: CollectedDataParam.Type

    private var shouldShowTakePhoto: Boolean = false

    private var shouldShowChoosePhoto: Boolean = false

    @VisibleForTesting
    internal var identityUploadViewModelFactory: ViewModelProvider.Factory =
        IdentityUploadViewModel.FrontBackUploadViewModelFactory(identityIO)

    private val identityUploadViewModel: IdentityUploadViewModel by viewModels {
        identityUploadViewModelFactory
    }

    protected val identityViewModel: IdentityViewModel by activityViewModels { identityViewModelFactory }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val args = requireNotNull(arguments) {
            "Argument to FrontBackUploadFragment is null"
        }
        shouldShowTakePhoto = args.getBoolean(ARG_SHOULD_SHOW_TAKE_PHOTO)
        shouldShowChoosePhoto = args.getBoolean(ARG_SHOULD_SHOW_CHOOSE_PHOTO)

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
                identityViewModel = identityViewModel,
                title = stringResource(id = titleRes),
                context = stringResource(id = contextRes),
                frontInfo =
                DocumentUploadSideInfo(
                    stringResource(id = frontTextRes),
                    stringResource(id = frontCheckMarkContentDescription),
                    frontScanType,
                    shouldShowTakePhoto,
                    shouldShowChoosePhoto
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
                        backScanType!!,
                        shouldShowTakePhoto,
                        shouldShowChoosePhoto
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
                onComposeFinish = {
                    identityViewModel.observeForVerificationPage(
                        this@IdentityUploadFragment,
                        onSuccess = {
                            lifecycleScope.launch(identityViewModel.workContext) {
                                identityViewModel.screenTracker.screenTransitionFinish(
                                    fragmentId.fragmentIdToScreenName()
                                )
                            }
                            identityViewModel.sendAnalyticsRequest(
                                identityViewModel.identityAnalyticsRequestFactory.screenPresented(
                                    scanType = frontScanType,
                                    screenName = fragmentId.fragmentIdToScreenName()
                                )
                            )
                        }
                    )
                }
            ) {
                identityViewModel.observeForVerificationPage(
                    viewLifecycleOwner,
                    onSuccess = { verificationPage ->
                        lifecycleScope.launch {
                            navigateToSelfieOrSubmit(
                                verificationPage,
                                identityViewModel,
                                route
                            )
                        }
                    },
                    onFailure = { throwable ->
                        Log.e(TAG, "Fail to observeForVerificationPage: $throwable")
                        navigateToDefaultErrorFragment(throwable, identityViewModel)
                    }
                )
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        collectedUploadState()
    }

    /**
     * Collect the last status of both sids of document upload and post.
     * Only post when that side is not yet collected.
     */
    private fun collectedUploadState() {
        lifecycleScope.launch {
            identityViewModel.frontCollectedInfo.collectLatest { (frontUploadState, collectedData) ->
                if (collectedData.idDocumentFront == null) {
                    if (frontUploadState.hasError()) {
                        navigateToDefaultErrorFragment(
                            frontUploadState.getError(),
                            identityViewModel
                        )
                    } else if (frontUploadState.isHighResUploaded()) {
                        val front = requireNotNull(frontUploadState.highResResult.data)
                        postCollectedDataParamAndNavigate(
                            CollectedDataParam(
                                idDocumentFront = DocumentUploadParam(
                                    highResImage = requireNotNull(front.uploadedStripeFile.id) {
                                        "front uploaded file id is null"
                                    },
                                    uploadMethod = requireNotNull(front.uploadMethod)
                                ),
                                idDocumentType = collectedDataParamType
                            )
                        )
                    }
                }
            }
        }
        lifecycleScope.launch {
            identityViewModel.backCollectedInfo.collectLatest { (backUploadedState, collectedData) ->
                if (collectedData.idDocumentBack == null) {
                    if (backUploadedState.hasError()) {
                        navigateToDefaultErrorFragment(
                            backUploadedState.getError(),
                            identityViewModel
                        )
                    } else if (backUploadedState.isHighResUploaded()) {
                        val back = requireNotNull(backUploadedState.highResResult.data)
                        postCollectedDataParamAndNavigate(
                            CollectedDataParam(
                                idDocumentBack = DocumentUploadParam(
                                    highResImage = requireNotNull(back.uploadedStripeFile.id) {
                                        "back uploaded file id is null"
                                    },
                                    uploadMethod = requireNotNull(back.uploadMethod)
                                ),
                                idDocumentType = collectedDataParamType
                            )
                        )
                    }
                }
            }
        }
    }

    private fun postCollectedDataParamAndNavigate(
        collectedDataParam: CollectedDataParam
    ) = identityViewModel.observeForVerificationPage(
        viewLifecycleOwner,
        onSuccess = {
            lifecycleScope.launch {
                runCatching {
                    postVerificationPageData(
                        identityViewModel = identityViewModel,
                        collectedDataParam =
                        collectedDataParam,
                        fromRoute = route
                    )
                }.onFailure {
                    Log.e(TAG, "Fail to observeForVerificationPage: $it")
                    navigateToDefaultErrorFragment(it, identityViewModel)
                }
            }
        },
        onFailure = { throwable ->
            Log.e(TAG, "Fail to observeForVerificationPage: $throwable")
            navigateToDefaultErrorFragment(throwable, identityViewModel)
        }
    )

    private fun observeForDocCapturePage(
        onSuccess: (VerificationPageStaticContentDocumentCapturePage) -> Unit
    ) {
        identityViewModel.observeForVerificationPage(
            viewLifecycleOwner,
            onSuccess = {
                onSuccess(it.documentCapture)
            },
            onFailure = {
                navigateToDefaultErrorFragment(it, identityViewModel)
            }
        )
    }

    private fun uploadResult(
        uri: Uri,
        uploadMethod: DocumentUploadParam.UploadMethod,
        isFront: Boolean,
        scanType: IdentityScanState.ScanType
    ) {
        observeForDocCapturePage { docCapturePage ->
            identityViewModel.uploadManualResult(
                uri = uri,
                isFront = isFront,
                docCapturePage = docCapturePage,
                uploadMethod = uploadMethod,
                scanType = scanType
            )
        }
    }

    companion object {
        val TAG: String = IdentityUploadFragment::class.java.simpleName
    }
}
