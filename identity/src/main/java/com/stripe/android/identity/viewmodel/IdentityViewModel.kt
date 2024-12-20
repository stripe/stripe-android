package com.stripe.android.identity.viewmodel

import android.app.Application
import android.graphics.Bitmap
import android.net.Uri
import android.util.Log
import androidx.activity.result.ActivityResultCaller
import androidx.activity.result.ActivityResultLauncher
import androidx.annotation.VisibleForTesting
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.CreationExtras
import androidx.navigation.NavController
import com.stripe.android.camera.CameraPermissionEnsureable
import com.stripe.android.camera.framework.image.longerEdge
import com.stripe.android.core.injection.IOContext
import com.stripe.android.core.injection.UIContext
import com.stripe.android.core.model.StripeFilePurpose
import com.stripe.android.identity.IdentityVerificationSheet
import com.stripe.android.identity.IdentityVerificationSheetContract
import com.stripe.android.identity.VerificationFlowFinishable
import com.stripe.android.identity.analytics.AnalyticsState
import com.stripe.android.identity.analytics.IdentityAnalyticsRequestFactory
import com.stripe.android.identity.analytics.ScreenTracker
import com.stripe.android.identity.camera.IdentityAggregator
import com.stripe.android.identity.injection.IdentityActivitySubcomponent
import com.stripe.android.identity.ml.BoundingBox
import com.stripe.android.identity.ml.Category
import com.stripe.android.identity.ml.FaceDetectorAnalyzer
import com.stripe.android.identity.ml.FaceDetectorOutput
import com.stripe.android.identity.ml.IDDetectorOutput
import com.stripe.android.identity.navigation.CameraPermissionDeniedDestination
import com.stripe.android.identity.navigation.ConfirmationDestination
import com.stripe.android.identity.navigation.DocumentScanDestination
import com.stripe.android.identity.navigation.DocumentUploadDestination
import com.stripe.android.identity.navigation.ErrorDestination
import com.stripe.android.identity.navigation.IdentityTopLevelDestination
import com.stripe.android.identity.navigation.IndividualDestination
import com.stripe.android.identity.navigation.OTPDestination
import com.stripe.android.identity.navigation.SelfieDestination
import com.stripe.android.identity.navigation.SelfieWarmupDestination
import com.stripe.android.identity.navigation.navigateOnVerificationPageData
import com.stripe.android.identity.navigation.navigateTo
import com.stripe.android.identity.navigation.navigateToErrorScreenWithDefaultValues
import com.stripe.android.identity.navigation.navigateToErrorScreenWithRequirementError
import com.stripe.android.identity.navigation.navigateToFinalErrorScreen
import com.stripe.android.identity.navigation.routeToScreenName
import com.stripe.android.identity.networking.IdentityModelFetcher
import com.stripe.android.identity.networking.IdentityRepository
import com.stripe.android.identity.networking.Resource
import com.stripe.android.identity.networking.Resource.Companion.DUMMY_RESOURCE
import com.stripe.android.identity.networking.SelfieUploadState
import com.stripe.android.identity.networking.SingleSideDocumentUploadState
import com.stripe.android.identity.networking.Status
import com.stripe.android.identity.networking.UploadedResult
import com.stripe.android.identity.networking.models.ClearDataParam
import com.stripe.android.identity.networking.models.CollectedDataParam
import com.stripe.android.identity.networking.models.CollectedDataParam.Companion.clearData
import com.stripe.android.identity.networking.models.CollectedDataParam.Companion.collectedRequirements
import com.stripe.android.identity.networking.models.CollectedDataParam.Companion.mergeWith
import com.stripe.android.identity.networking.models.DocumentUploadParam
import com.stripe.android.identity.networking.models.DocumentUploadParam.UploadMethod
import com.stripe.android.identity.networking.models.Requirement
import com.stripe.android.identity.networking.models.Requirement.Companion.INDIVIDUAL_REQUIREMENT_SET
import com.stripe.android.identity.networking.models.Requirement.Companion.nextDestination
import com.stripe.android.identity.networking.models.Requirement.Companion.supportsForceConfirm
import com.stripe.android.identity.networking.models.VerificationPage
import com.stripe.android.identity.networking.models.VerificationPage.Companion.requireSelfie
import com.stripe.android.identity.networking.models.VerificationPageData
import com.stripe.android.identity.networking.models.VerificationPageData.Companion.hasError
import com.stripe.android.identity.networking.models.VerificationPageData.Companion.needsFallback
import com.stripe.android.identity.networking.models.VerificationPageData.Companion.submittedAndClosed
import com.stripe.android.identity.networking.models.VerificationPageRequirements
import com.stripe.android.identity.networking.models.VerificationPageStaticContentDocumentCapturePage
import com.stripe.android.identity.networking.models.VerificationPageStaticContentSelfieCapturePage
import com.stripe.android.identity.states.FaceDetectorTransitioner
import com.stripe.android.identity.states.IdentityScanState
import com.stripe.android.identity.ui.IndividualCollectedStates
import com.stripe.android.identity.utils.IdentityIO
import com.stripe.android.identity.utils.IdentityImageHandler
import com.stripe.android.mlcore.base.InterpreterInitializer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File
import kotlin.coroutines.CoroutineContext

/**
 * ViewModel hosted by IdentityActivity, shared across fragments.
 */
internal class IdentityViewModel constructor(
    application: Application,
    internal val verificationArgs: IdentityVerificationSheetContract.Args,
    internal val identityRepository: IdentityRepository,
    private val identityModelFetcher: IdentityModelFetcher,
    private val identityIO: IdentityIO,
    internal val identityAnalyticsRequestFactory: IdentityAnalyticsRequestFactory,
    internal val screenTracker: ScreenTracker,
    internal val imageHandler: IdentityImageHandler,
    private val tfLiteInitializer: InterpreterInitializer,
    private val savedStateHandle: SavedStateHandle,
    @UIContext internal val uiContext: CoroutineContext,
    @IOContext internal val workContext: CoroutineContext,
    private val finishWithResult: (IdentityVerificationSheet.VerificationFlowResult) -> Unit
) : AndroidViewModel(application) {

    /**
     * StateFlow to track the upload status of high/low resolution image for front of document.
     */
    private val _documentFrontUploadedState =
        MutableStateFlow(
            savedStateHandle.get<SingleSideDocumentUploadState>(DOCUMENT_FRONT_UPLOAD_STATE)?.let {
                // If saved as Loading, the uploading coroutine would fail as the app is destroyed.
                // Clear the state when recovered.
                if (it.isLoading()) {
                    SingleSideDocumentUploadState()
                } else {
                    it
                }
            } ?: run {
                SingleSideDocumentUploadState()
            }
        )
    val documentFrontUploadedState: StateFlow<SingleSideDocumentUploadState> =
        _documentFrontUploadedState

    /**
     * StateFlow to track the upload status of high/low resolution image for back of document.
     */
    private val _documentBackUploadedState =
        MutableStateFlow(
            savedStateHandle.get<SingleSideDocumentUploadState>(DOCUMENT_BACK_UPLOAD_STATE)?.let {
                // If saved as Loading, the uploading coroutine would fail as the app is destroyed.
                // Clear the state when recovered.
                if (it.isLoading()) {
                    SingleSideDocumentUploadState()
                } else {
                    it
                }
            } ?: run {
                SingleSideDocumentUploadState()
            }
        )
    val documentBackUploadedState: StateFlow<SingleSideDocumentUploadState> =
        _documentBackUploadedState

    /**
     * StateFlow to track the upload status of high/low resolution images of selfies.
     */
    private val _selfieUploadedState = MutableStateFlow(
        savedStateHandle[SELFIE_UPLOAD_STATE] ?: SelfieUploadState()
    )
    val selfieUploadState: StateFlow<SelfieUploadState> = _selfieUploadedState

    /**
     * StateFlow to track analytics status.
     */
    private val _analyticsState = MutableStateFlow(
        savedStateHandle[ANALYTICS_STATE] ?: AnalyticsState()
    )
    val analyticsState: StateFlow<AnalyticsState> = _analyticsState

    /**
     * StateFlow to track the data collected so far.
     */
    @VisibleForTesting
    internal val _collectedData = MutableStateFlow(
        savedStateHandle[COLLECTED_DATA] ?: CollectedDataParam()
    )
    val collectedData: StateFlow<CollectedDataParam> = _collectedData

    private val _cameraPermissionGranted = MutableStateFlow(
        savedStateHandle[CAMERA_PERMISSION_GRANTED] ?: false
    )
    val cameraPermissionGranted: StateFlow<Boolean> = _cameraPermissionGranted

    private val _visitedIndividualWelcomeScreen = MutableStateFlow(
        savedStateHandle[VISITED_INDIVIDUAL_WELCOME_PAGE] ?: run {
            false
        }
    )
    val visitedIndividualWelcomeScreen: StateFlow<Boolean> = _visitedIndividualWelcomeScreen

    /**
     * StateFlow to track request status of postVerificationPageData
     */
    @VisibleForTesting
    internal val verificationPageData = MutableStateFlow<Resource<Int>>(
        savedStateHandle[VERIFICATION_PAGE_DATA] ?: Resource.idle()
    )

    /**
     * StateFlow to track request status of postVerificationPageSubmit
     */
    @VisibleForTesting
    internal val verificationPageSubmit = MutableStateFlow<Resource<Int>>(
        savedStateHandle[VERIFICATION_PAGE_SUBMIT] ?: Resource.idle()
    )

    /**
     * StateFlow to track missing requirements.
     */
    private val _missingRequirements = MutableStateFlow<Set<Requirement>>(
        savedStateHandle[MISSING_REQUIREMENTS] ?: setOf()
    )
    val missingRequirements: StateFlow<Set<Requirement>> = _missingRequirements

    val frontCollectedInfo =
        _documentFrontUploadedState.combine(_collectedData) { upload, collected ->
            (upload to collected)
        }.stateIn(
            viewModelScope,
            SharingStarted.Eagerly,
            (SingleSideDocumentUploadState() to CollectedDataParam())
        )

    val backCollectedInfo =
        _documentBackUploadedState.combine(_collectedData) { upload, collected ->
            (upload to collected)
        }.stateIn(
            viewModelScope,
            SharingStarted.Eagerly,
            (SingleSideDocumentUploadState() to CollectedDataParam())
        )

    private val _isTfLiteInitialized: MutableLiveData<Boolean> = MutableLiveData(false)
    val isTfLiteInitialized: LiveData<Boolean> = _isTfLiteInitialized

    fun initializeTfLite() {
        viewModelScope.launch(workContext) {
            tfLiteInitializer.initialize(
                getApplication(),
                {
                    _isTfLiteInitialized.postValue(true)
                },
                { throw IllegalStateException("Failed to initialize TFLite runtime: $it") }
            )
        }
    }

    /**
     * Response for initial VerificationPage, used for building UI.
     */
    @VisibleForTesting
    internal val _verificationPage: MutableLiveData<Resource<VerificationPage>> =
        // No need to write to savedStateHandle for livedata
        savedStateHandle.getLiveData(
            key = VERIFICATION_PAGE,
            initialValue = Resource.idle()
        )

    val verificationPage: LiveData<Resource<VerificationPage>> = _verificationPage

    /**
     * Network response for the IDDetector model.
     */
    private val _idDetectorModelFile = MutableLiveData<Resource<File>>()
    val idDetectorModelFile: LiveData<Resource<File>> = _idDetectorModelFile

    /**
     * Network response for the FaceDetector model.
     */
    private val _faceDetectorModelFile = MutableLiveData<Resource<File>>()
    val faceDetectorModelFile: LiveData<Resource<File>> = _faceDetectorModelFile

    data class PageAndModelFiles(
        val page: VerificationPage,
        val idDetectorFile: File,
        val faceDetectorFile: File?
    )

    /**
     * Wrapper for both page and model
     */
    val pageAndModelFiles = object : MediatorLiveData<Resource<PageAndModelFiles>>() {
        private var page: VerificationPage? = null
        private var idDetectorModel: File? = null
        private var faceDetectorModel: File? = null
        private var faceDetectorModelValueSet = false
        private var isTfliteInitialized = false

        init {
            postValue(Resource.loading())
            addSource(verificationPage) {
                when (it.status) {
                    Status.SUCCESS -> {
                        page = it.data
                        maybePostSuccess()
                    }

                    Status.ERROR -> {
                        postValue(Resource.error("$verificationPage posts error"))
                    }

                    Status.LOADING -> {} // no-op
                    Status.IDLE -> {}
                }
            }
            addSource(idDetectorModelFile) {
                when (it.status) {
                    Status.SUCCESS -> {
                        idDetectorModel = it.data
                        maybePostSuccess()
                    }

                    Status.ERROR -> {
                        postValue(Resource.error("$idDetectorModelFile posts error"))
                    }

                    Status.LOADING -> {} // no-op
                    Status.IDLE -> {} // no-op
                }
            }
            addSource(faceDetectorModelFile) {
                when (it.status) {
                    Status.SUCCESS -> {
                        faceDetectorModelValueSet = true
                        faceDetectorModel = it.data
                        maybePostSuccess()
                    }

                    Status.ERROR -> {
                        postValue(Resource.error("$faceDetectorModelFile posts error"))
                    }

                    Status.LOADING -> {} // no-op
                    Status.IDLE -> {} // no-op
                }
            }
            addSource(isTfLiteInitialized) { initialized ->
                isTfliteInitialized = initialized
                if (isTfliteInitialized) {
                    maybePostSuccess()
                }
            }
        }

        private fun maybePostSuccess() {
            page?.let { page ->
                idDetectorModel?.let { idDetectorModel ->
                    if (isTfliteInitialized && faceDetectorModelValueSet) {
                        postValue(
                            Resource.success(
                                PageAndModelFiles(
                                    page,
                                    idDetectorModel,
                                    faceDetectorModel
                                )
                            )
                        )
                    }
                }
            }
        }
    }

    /**
     * LiveData for the cause of ErrorScreen.
     */
    val errorCause = MutableLiveData<Throwable>()
    private val errorCauseObServer = Observer<Throwable> { value -> logError(value) }

    init {
        errorCause.observeForever(errorCauseObServer)
    }

    override fun onCleared() {
        super.onCleared()
        errorCause.removeObserver(errorCauseObServer)
    }

    /**
     * Upload high_res of an image Uri manually picked from local file storage or taken from camera.
     */
    internal fun uploadManualResult(
        uri: Uri,
        isFront: Boolean,
        docCapturePage: VerificationPageStaticContentDocumentCapturePage,
        uploadMethod: UploadMethod,
        scanType: IdentityScanState.ScanType
    ) {
        uploadDocumentImagesAndNotify(
            imageFile =
            identityIO.resizeUriAndCreateFileToUpload(
                uri,
                verificationArgs.verificationSessionId,
                false,
                if (isFront) FRONT else BACK,
                maxDimension = docCapturePage.highResImageMaxDimension,
                compressionQuality = docCapturePage.highResImageCompressionQuality
            ),
            filePurpose = requireNotNull(
                StripeFilePurpose.fromCode(docCapturePage.filePurpose)
            ),
            uploadMethod = uploadMethod,
            isHighRes = true,
            isFront = isFront,
            scanType = scanType,
            compressionQuality = docCapturePage.highResImageCompressionQuality
        )
    }

    /**
     * Upload high_res and low_res of the [IdentityAggregator.FinalResult] from scan.
     */
    internal fun uploadScanResult(
        result: IdentityAggregator.FinalResult,
        verificationPage: VerificationPage
    ) {
        when (result.result) {
            is IDDetectorOutput.Legacy -> {
                uploadLegacyIDDetectorOutput(
                    result.frame.cameraPreviewImage.image,
                    result.result,
                    verificationPage
                )
            }

            is FaceDetectorOutput -> {
                uploadFaceDetectorOutput(
                    result,
                    verificationPage
                )
            }
        }
    }

    private fun uploadLegacyIDDetectorOutput(
        originalBitmap: Bitmap,
        legacyOutput: IDDetectorOutput.Legacy,
        verificationPage: VerificationPage
    ) {
        val scores = legacyOutput.allScores

        val isFront: Boolean
        val targetScanType: IdentityScanState.ScanType

        when (legacyOutput.category) {
            Category.PASSPORT -> {
                isFront = true
                targetScanType = IdentityScanState.ScanType.DOC_FRONT
            }

            Category.ID_FRONT -> {
                isFront = true
                targetScanType = IdentityScanState.ScanType.DOC_FRONT
            }

            Category.ID_BACK -> {
                isFront = false
                targetScanType = IdentityScanState.ScanType.DOC_BACK
            }

            else -> {
                Log.e(TAG, "incorrect category: ${legacyOutput.category}")
                isFront = true
                targetScanType = IdentityScanState.ScanType.DOC_FRONT
                logError(
                    IllegalStateException(
                        "incorrect legacy targetScanType: ${legacyOutput.category}, " +
                            "upload as DOC_FRONT"
                    )
                )
            }
        }

        // upload high res
        processAndUploadBitmap(
            bitmapToUpload = cropBitmapToUpload(
                originalBitmap,
                legacyOutput.boundingBox,
                verificationPage
            ),
            docCapturePage = verificationPage.documentCapture,
            isHighRes = true,
            isFront = isFront,
            scores = scores,
            targetScanType = targetScanType
        )

        // upload low res
        processAndUploadBitmap(
            bitmapToUpload = originalBitmap,
            docCapturePage = verificationPage.documentCapture,
            isHighRes = false,
            isFront = isFront,
            scores = scores,
            targetScanType = targetScanType
        )
    }

    private fun uploadFaceDetectorOutput(
        result: IdentityAggregator.FinalResult,
        verificationPage: VerificationPage
    ) {
        val filteredFrames =
            (result.identityState.transitioner as FaceDetectorTransitioner).filteredFrames
        require(filteredFrames.size == FaceDetectorTransitioner.NUM_FILTERED_FRAMES) {
            "FaceDetectorTransitioner incorrectly collected ${filteredFrames.size} frames " +
                "instead of ${FaceDetectorTransitioner.NUM_FILTERED_FRAMES} frames"
        }

        listOf(
            (FaceDetectorTransitioner.Selfie.FIRST),
            (FaceDetectorTransitioner.Selfie.BEST),
            (FaceDetectorTransitioner.Selfie.LAST)
        ).forEach { selfie ->
            listOf(true, false).forEach { isHighRes ->
                processSelfieScanResultAndUpload(
                    originalBitmap = filteredFrames[selfie.index].first.cameraPreviewImage.image,
                    boundingBox = filteredFrames[selfie.index].second.boundingBox,
                    selfieCapturePage = requireNotNull(verificationPage.selfieCapture),
                    isHighRes = isHighRes,
                    selfie = selfie
                )
            }
        }
    }

    private fun cropBitmapToUpload(
        originalBitmap: Bitmap,
        boundingBox: BoundingBox,
        verificationPage: VerificationPage
    ) =
        identityIO.cropAndPadBitmap(
            originalBitmap,
            boundingBox,
            originalBitmap.longerEdge() * verificationPage.documentCapture.highResImageCropPadding
        )

    /**
     * Processes document scan result by cropping and padding the bitmap if necessary,
     * then upload the processed file.
     */
    @VisibleForTesting
    internal fun processAndUploadBitmap(
        bitmapToUpload: Bitmap,
        docCapturePage: VerificationPageStaticContentDocumentCapturePage,
        isHighRes: Boolean,
        isFront: Boolean,
        scores: List<Float>,
        targetScanType: IdentityScanState.ScanType,
    ) {
        identityIO.resizeBitmapAndCreateFileToUpload(
            bitmap = bitmapToUpload,
            verificationId = verificationArgs.verificationSessionId,
            fileName =
            StringBuilder().also { nameBuilder ->
                nameBuilder.append(verificationArgs.verificationSessionId)
                nameBuilder.append("_${if (isFront) FRONT else BACK}")
                if (!isHighRes) {
                    nameBuilder.append("_full_frame")
                }
                nameBuilder.append(".jpeg")
            }.toString(),
            maxDimension =
            if (isHighRes) {
                docCapturePage.highResImageMaxDimension
            } else {
                docCapturePage.lowResImageMaxDimension
            },
            compressionQuality =
            if (isHighRes) {
                docCapturePage.highResImageCompressionQuality
            } else {
                docCapturePage.lowResImageCompressionQuality
            }
        ).let { imageFile ->
            uploadDocumentImagesAndNotify(
                imageFile = imageFile,
                filePurpose = requireNotNull(
                    StripeFilePurpose.fromCode(docCapturePage.filePurpose)
                ),
                uploadMethod = UploadMethod.AUTOCAPTURE,
                scores = scores,
                isHighRes = isHighRes,
                isFront = isFront,
                scanType = targetScanType,
                compressionQuality =
                if (isHighRes) {
                    docCapturePage.highResImageCompressionQuality
                } else {
                    docCapturePage.lowResImageCompressionQuality
                }
            )
        }
    }

    /**
     * Update the analytics state.
     */
    internal fun updateAnalyticsState(updateBlock: (AnalyticsState) -> AnalyticsState) {
        _analyticsState.updateStateAndSave(updateBlock)
    }

    /**
     * Uploads the imageFile and notifies corresponding result [LiveData].
     */
    private fun uploadDocumentImagesAndNotify(
        imageFile: File,
        filePurpose: StripeFilePurpose,
        uploadMethod: UploadMethod,
        scores: List<Float>? = null,
        isHighRes: Boolean,
        isFront: Boolean,
        scanType: IdentityScanState.ScanType,
        compressionQuality: Float
    ) {
        viewModelScope.launch {
            if (isFront) {
                _documentFrontUploadedState
            } else {
                _documentBackUploadedState
            }.updateStateAndSave { currentState ->
                currentState.updateLoading(isHighRes = isHighRes)
            }

            runCatching {
                var uploadTime = 0L
                identityRepository.uploadImage(
                    verificationId = verificationArgs.verificationSessionId,
                    ephemeralKey = verificationArgs.ephemeralKeySecret,
                    imageFile = imageFile,
                    filePurpose = filePurpose,
                    onSuccessExecutionTimeBlock = { uploadTime = it }
                ) to uploadTime
            }.fold(
                onSuccess = { fileTimePair ->
                    identityAnalyticsRequestFactory.imageUpload(
                        value = fileTimePair.second,
                        compressionQuality = compressionQuality,
                        scanType = scanType,
                        id = fileTimePair.first.id,
                        fileName = fileTimePair.first.filename,
                        fileSize = imageFile.length() / BYTES_IN_KB
                    )

                    updateAnalyticsState { oldState ->
                        if (isFront) {
                            oldState.copy(
                                docFrontUploadType = uploadMethod
                            )
                        } else {
                            oldState.copy(
                                docBackUploadType = uploadMethod
                            )
                        }
                    }
                    if (isFront) {
                        _documentFrontUploadedState
                    } else {
                        _documentBackUploadedState
                    }.updateStateAndSave { currentState ->
                        currentState.update(
                            isHighRes = isHighRes,
                            newResult = UploadedResult(
                                fileTimePair.first,
                                scores,
                                uploadMethod
                            ),
                        )
                    }
                },
                onFailure = {
                    if (isFront) {
                        _documentFrontUploadedState
                    } else {
                        _documentBackUploadedState
                    }.updateStateAndSave { currentState ->
                        currentState.updateError(
                            isHighRes = isHighRes,
                            message = "Failed to upload file : ${imageFile.name}",
                            throwable = it
                        )
                    }
                }
            )
        }
    }

    /**
     * Processes selfie scan result by cropping and padding the bitmap if necessary,
     * then upload the processed file.
     */
    private fun processSelfieScanResultAndUpload(
        originalBitmap: Bitmap,
        boundingBox: BoundingBox,
        selfieCapturePage: VerificationPageStaticContentSelfieCapturePage,
        isHighRes: Boolean,
        selfie: FaceDetectorTransitioner.Selfie
    ) {
        identityIO.resizeBitmapAndCreateFileToUpload(
            bitmap =
            if (isHighRes) {
                identityIO.cropAndPadBitmap(
                    originalBitmap,
                    boundingBox,
                    boundingBox.width * FaceDetectorAnalyzer.INPUT_WIDTH * selfieCapturePage.highResImageCropPadding
                )
            } else {
                originalBitmap
            },
            verificationId = verificationArgs.verificationSessionId,
            fileName =
            StringBuilder().also { nameBuilder ->
                nameBuilder.append(verificationArgs.verificationSessionId)
                nameBuilder.append("_face")
                if (isHighRes) {
                    if (selfie != FaceDetectorTransitioner.Selfie.BEST) {
                        nameBuilder.append("_${selfie.value}_crop_frame")
                    }
                } else {
                    if (selfie == FaceDetectorTransitioner.Selfie.BEST) {
                        nameBuilder.append("_full_frame")
                    } else {
                        nameBuilder.append("_${selfie.value}_full_frame")
                    }
                }
                nameBuilder.append(".jpeg")
            }.toString(),
            maxDimension =
            if (isHighRes) {
                selfieCapturePage.highResImageMaxDimension
            } else {
                selfieCapturePage.lowResImageMaxDimension
            },
            compressionQuality =
            if (isHighRes) {
                selfieCapturePage.highResImageCompressionQuality
            } else {
                selfieCapturePage.lowResImageCompressionQuality
            }
        ).let { imageFile ->
            uploadSelfieImagesAndNotify(
                imageFile = imageFile,
                filePurpose = requireNotNull(
                    StripeFilePurpose.fromCode(selfieCapturePage.filePurpose)
                ),
                isHighRes = isHighRes,
                selfie = selfie,
                compressionQuality = if (isHighRes) {
                    selfieCapturePage.highResImageCompressionQuality
                } else {
                    selfieCapturePage.lowResImageCompressionQuality
                }
            )
        }
    }

    private fun uploadSelfieImagesAndNotify(
        imageFile: File,
        filePurpose: StripeFilePurpose,
        isHighRes: Boolean,
        selfie: FaceDetectorTransitioner.Selfie,
        compressionQuality: Float
    ) {
        _selfieUploadedState.updateStateAndSave { currentState ->
            currentState.updateLoading(isHighRes, selfie)
        }
        viewModelScope.launch {
            runCatching {
                var uploadTime = 0L
                identityRepository.uploadImage(
                    verificationId = verificationArgs.verificationSessionId,
                    ephemeralKey = verificationArgs.ephemeralKeySecret,
                    imageFile = imageFile,
                    filePurpose = filePurpose,
                    onSuccessExecutionTimeBlock = { uploadTime = it }
                ) to uploadTime
            }.fold(
                onSuccess = { fileTimePair ->
                    identityAnalyticsRequestFactory.imageUpload(
                        value = fileTimePair.second,
                        compressionQuality = compressionQuality,
                        scanType = IdentityScanState.ScanType.SELFIE,
                        id = fileTimePair.first.id,
                        fileName = fileTimePair.first.filename,
                        fileSize = imageFile.length() / BYTES_IN_KB
                    )
                    _selfieUploadedState.updateStateAndSave { currentState ->
                        currentState.update(
                            isHighRes = isHighRes,
                            newResult = UploadedResult(
                                fileTimePair.first
                            ),
                            selfie = selfie
                        )
                    }
                },
                onFailure = {
                    _selfieUploadedState.updateStateAndSave { currentState ->
                        currentState.updateError(
                            isHighRes = isHighRes,
                            selfie = selfie,
                            message = "Failed to upload file : ${imageFile.name}",
                            throwable = it
                        )
                    }
                }
            )
        }
    }

    /**
     * Simple wrapper for observing [verificationPage].
     */
    fun observeForVerificationPage(
        owner: LifecycleOwner,
        onSuccess: (VerificationPage) -> Unit,
        onFailure: (Throwable) -> Unit = {
            Log.d(TAG, "Failed to get VerificationPage")
        }
    ) {
        verificationPage.observe(owner) { resource ->
            when (resource.status) {
                Status.SUCCESS -> {
                    onSuccess(requireNotNull(resource.data))
                }

                Status.ERROR -> {
                    Log.e(TAG, "Fail to get VerificationPage")
                    onFailure(requireNotNull(resource.throwable))
                }

                Status.LOADING -> {} // no-op
                Status.IDLE -> {} // no-op
            }
        }
    }

    /**
     * Retrieve the VerificationPage data and post its value to [verificationPage]
     */
    fun retrieveAndBufferVerificationPage(shouldRetrieveModel: Boolean = true) {
        _verificationPage.postValue(Resource.loading())
        viewModelScope.launch {
            runCatching {
                identityRepository.retrieveVerificationPage(
                    verificationArgs.verificationSessionId,
                    verificationArgs.ephemeralKeySecret
                )
            }.fold(
                onSuccess = { verificationPage ->
                    _verificationPage.postValue(Resource.success(verificationPage))
                    identityAnalyticsRequestFactory.verificationPage = verificationPage
                    _missingRequirements.updateStateAndSave {
                        verificationPage.requirements.missing.toSet()
                    }
                    if (shouldRetrieveModel) {
                        downloadModelAndPost(
                            verificationPage.documentCapture.models.idDetectorUrl,
                            _idDetectorModelFile
                        )
                        verificationPage.selfieCapture?.let { selfieCapture ->
                            downloadModelAndPost(
                                selfieCapture.models.faceDetectorUrl,
                                _faceDetectorModelFile
                            )
                        } ?: run {
                            // Selfie not required, post null
                            _faceDetectorModelFile.postValue(Resource.success(null))
                        }
                    }
                },
                onFailure = {
                    "Failed to retrieve verification page with " +
                        (
                            "sessionID: ${verificationArgs.verificationSessionId} and ephemeralKey: " +
                                verificationArgs.ephemeralKeySecret
                            ).let { msg ->
                                _verificationPage.postValue(
                                    Resource.error(
                                        msg,
                                        IllegalStateException(msg, it)
                                    )
                                )
                            }
                }
            )
        }
    }

    /**
     * Invoke the verify endpoint and navigate to the [ConfirmationDestination] if successful.
     */
    suspend fun verifySessionAndTransition(
        fromRoute: String,
        simulateDelay: Boolean,
        navController: NavController
    ) {
        runCatching {
            identityRepository.verifyTestVerificationSession(
                id = verificationArgs.verificationSessionId,
                ephemeralKey = verificationArgs.ephemeralKeySecret,
                simulateDelay = simulateDelay
            )
        }.checkSubmitStatusAndNavigate(fromRoute, navController)
    }

    /**
     * Invoke the unverify endpoint and navigate to the [ConfirmationDestination] if successful.
     */
    suspend fun unverifySessionAndTransition(
        fromRoute: String,
        simulateDelay: Boolean,
        navController: NavController
    ) {
        runCatching {
            identityRepository.unverifyTestVerificationSession(
                id = verificationArgs.verificationSessionId,
                ephemeralKey = verificationArgs.ephemeralKeySecret,
                simulateDelay = simulateDelay
            )
        }.checkSubmitStatusAndNavigate(fromRoute, navController)
    }

    /**
     * Check the submt [Result] of [VerificationPageData] and try to navigate to [ConfirmationDestination].
     *
     * If Result is success, check the value of VerificationPageData
     *   If VerificationPageData has error, navigate to [ErrorDestination] with the error.
     *   If VerificationPageData is submitted closed, navigate to [ConfirmationDestination].
     *   If VerificationPageData is not closed and it still has missings,
     *     document fallback happens during submit, update initial missings and navigate accordingly.
     *   Otherwise navigate to [ErrorDestination].
     *
     * If Result is failed, navigate to [ErrorDestination] with the failure information.
     *
     */
    private fun Result<VerificationPageData>.checkSubmitStatusAndNavigate(
        fromRoute: String,
        navController: NavController
    ) {
        this.onSuccess { submittedVerificationPageData ->
            verificationPageSubmit.updateStateAndSave {
                Resource.success(DUMMY_RESOURCE)
            }
            when {
                submittedVerificationPageData.hasError() -> {
                    submittedVerificationPageData.requirements.errors[0].let { requirementError ->
                        errorCause.postValue(
                            IllegalStateException("VerificationPageDataRequirementError: $requirementError")
                        )
                        navController.navigateToErrorScreenWithRequirementError(
                            fromRoute,
                            requirementError
                        )
                    }
                }
                // After submit, missings got repopulated - fallback from phoneV to doc
                // Need to reset all non-doc related data and start from doc.
                submittedVerificationPageData.needsFallback() -> {
                    // update initialMissings
                    val newMissings =
                        requireNotNull(submittedVerificationPageData.requirements.missings)
                    _verificationPage.postValue(
                        Resource.success(
                            _verificationPage.value?.data?.copy(
                                requirements = VerificationPageRequirements(
                                    missing = newMissings
                                )
                            )
                        )
                    )
                    // clear collectedData
                    _collectedData.updateStateAndSave {
                        CollectedDataParam()
                    }
                    // reset missingRequirement
                    _missingRequirements.updateStateAndSave {
                        newMissings.toSet()
                    }
                    navController.navigateTo(
                        newMissings.nextDestination(getApplication())
                    )
                }
                /**
                 * Only navigates to success when both submitted and closed are true.
                 */
                submittedVerificationPageData.submittedAndClosed() -> {
                    navController.navigateTo(ConfirmationDestination)
                }

                else -> {
                    errorCause.postValue(IllegalStateException("VerificationPage submit failed"))
                    navController.navigateToErrorScreenWithDefaultValues(getApplication())
                }
            }
        }.onFailure {
            errorCause.postValue(it)
            navController.navigateToErrorScreenWithDefaultValues(getApplication())
        }
    }

    /**
     * Download an ML model and post its value to [target].
     */
    private fun downloadModelAndPost(modelUrl: String, target: MutableLiveData<Resource<File>>) {
        viewModelScope.launch {
            runCatching {
                target.postValue(Resource.loading())
                identityModelFetcher.fetchIdentityModel(modelUrl)
            }.fold(
                onSuccess = {
                    target.postValue(Resource.success(it))
                },
                onFailure = {
                    target.postValue(
                        Resource.error(
                            "Failed to download model from $modelUrl",
                            it
                        )
                    )

                    // Exit with failure
                    finishWithResult(IdentityVerificationSheet.VerificationFlowResult.Failed(it))
                }
            )
        }
    }

    private fun calculateClearDataParam(dataToBeCollected: CollectedDataParam) =
        ClearDataParam.createFromRequirements(
            initialMissings.toMutableSet().minus(
                collectedData.value.collectedRequirements()
            ).minus(dataToBeCollected.collectedRequirements())
        )

    private val initialMissings: List<Requirement>
        get() {
            return _verificationPage.value?.data?.requirements?.missing ?: Requirement.entries
                .also {
                    Log.e(
                        TAG,
                        "_verificationPage is null, using Requirement.entries as initialMissings"
                    )
                }
        }

    /**
     * Send a POST request to VerificationPageData,
     * If POST succeeded, invoke [onCorrectResponse].
     * Otherwise transition to Error screen.
     */
    private suspend fun postVerificationPageData(
        navController: NavController,
        collectedDataParam: CollectedDataParam,
        fromRoute: String,
        onCorrectResponse: suspend ((verificationPageDataWithNoError: VerificationPageData) -> Unit) = {}
    ) {
        screenTracker.screenTransitionStart(
            fromRoute.routeToScreenName()
        )
        verificationPageData.updateStateAndSave {
            Resource.loading()
        }
        runCatching {
            identityRepository.postVerificationPageData(
                verificationArgs.verificationSessionId,
                verificationArgs.ephemeralKeySecret,
                collectedDataParam,
                calculateClearDataParam(collectedDataParam)
            )
        }.onSuccess { newVerificationPageData ->
            verificationPageData.updateStateAndSave {
                Resource.success(DUMMY_RESOURCE)
            }
            _collectedData.updateStateAndSave { oldValue ->
                oldValue.mergeWith(collectedDataParam)
            }
            updateStatesWithVerificationPageData(
                fromRoute,
                newVerificationPageData,
                navController,
                onCorrectResponse
            )
        }.onFailure { cause ->
            errorCause.postValue(cause)
            navController.navigateToErrorScreenWithDefaultValues(getApplication())
        }
    }

    /**
     * Check missings and error in VerificationPageData and update states accordingly.
     */
    suspend fun updateStatesWithVerificationPageData(
        fromRoute: String,
        newVerificationPageData: VerificationPageData,
        navController: NavController,
        onCorrectResponse: suspend ((verificationPageDataWithNoError: VerificationPageData) -> Unit) = {}
    ) {
        _missingRequirements.updateStateAndSave { oldMissings ->
            val newMissings =
                requireNotNull(newVerificationPageData.requirements.missings).toSet()

            newMissings.intersect(INDIVIDUAL_REQUIREMENT_SET).takeIf { it.isNotEmpty() }
                ?.let {
                    // If "NAME", "DOB" and "IDNUMBER" are collected and "DOB" is invalid,
                    // newMissings will only contain "DOB". However we still need to show the UI to
                    // collect all three fields, manually updated the oldIndividualMissings.
                    val oldIndividualMissings =
                        oldMissings.intersect(INDIVIDUAL_REQUIREMENT_SET)
                    newMissings.plus(oldIndividualMissings)
                } ?: run {
                newMissings
            }
        }

        if (newVerificationPageData.hasError()) {
            newVerificationPageData.requirements.errors[0].let { requirementError ->
                errorCause.postValue(
                    IllegalStateException("VerificationPageDataRequirementError: $requirementError")
                )
                navController.navigateToErrorScreenWithRequirementError(
                    fromRoute,
                    requirementError,
                )
            }
        } else {
            onCorrectResponse(newVerificationPageData)
        }
    }

    /**
     * Send a POST request to VerificationPageData, navigate or invoke the callbacks based on result.
     */
    suspend fun postVerificationPageDataAndMaybeNavigate(
        navController: NavController,
        collectedDataParam: CollectedDataParam,
        fromRoute: String,
        onMissingBack: () -> Unit = {
            errorCause.postValue(
                IllegalStateException(
                    "unhandled onMissingBack from $fromRoute with $collectedDataParam"
                )
            )
            navController.navigateToErrorScreenWithDefaultValues(getApplication())
        },
        onMissingPhoneOtp: () -> Unit = {
            errorCause.postValue(
                IllegalStateException(
                    "unhandled onMissingOtp from $fromRoute with $collectedDataParam"
                )
            )
            navController.navigateToErrorScreenWithDefaultValues(getApplication())
        },
        onReadyToSubmit: suspend () -> Unit = {
            errorCause.postValue(
                IllegalStateException(
                    "unhandled onReadyToSubmit from $fromRoute with $collectedDataParam"
                )
            )
            navController.navigateToErrorScreenWithDefaultValues(getApplication())
        }
    ) {
        postVerificationPageData(
            navController = navController,
            collectedDataParam = collectedDataParam,
            fromRoute = fromRoute
        ) { verificationPageData ->
            navController.navigateOnVerificationPageData(
                verificationPageData = verificationPageData,
                onMissingOtp = onMissingPhoneOtp,
                onMissingBack = onMissingBack,
                onReadyToSubmit = onReadyToSubmit
            )
        }
    }

    /**
     * Check if Selfie is needed from [verificationPage] and navigate.
     * If selfie is needed, navigate to [SelfieDestination]
     * Otherwise, sends a POST request to VerificationPageDataSubmit and navigate to [ConfirmationDestination].
     */
    suspend fun navigateToSelfieOrSubmit(
        navController: NavController,
        fromRoute: String
    ) {
        if (requireNotNull(verificationPage.value?.data).requireSelfie()) {
            navController.navigateTo(SelfieWarmupDestination)
        } else {
            submitAndNavigate(navController, fromRoute)
        }
    }

    /**
     * Submit the verification and navigate based on result.
     */
    internal suspend fun submitAndNavigate(
        navController: NavController,
        fromRoute: String
    ) {
        verificationPageSubmit.updateStateAndSave {
            Resource.loading()
        }
        runCatching {
            identityRepository.postVerificationPageSubmit(
                verificationArgs.verificationSessionId,
                verificationArgs.ephemeralKeySecret
            )
        }.checkSubmitStatusAndNavigate(fromRoute, navController)
    }

    /**
     * Observe for [verificationPage] and callback onSuccess, navigate to error otherwise.
     */
    fun requireVerificationPage(
        lifecycleOwner: LifecycleOwner,
        navController: NavController,
        onSuccess: (VerificationPage) -> Unit
    ) {
        verificationPage.observe(lifecycleOwner) { resource ->
            when (resource.status) {
                Status.SUCCESS -> {
                    onSuccess(requireNotNull(resource.data))
                }

                Status.ERROR -> {
                    Log.e(TAG, "Fail to get VerificationPage")
                    val cause = requireNotNull(resource.throwable)
                    errorCause.postValue(cause)
                    navController.navigateToErrorScreenWithDefaultValues(
                        getApplication()
                    )
                }

                Status.LOADING -> {} // no-op
                Status.IDLE -> {} // no-op
            }
        }
    }

    fun trackScreenPresented(scanType: IdentityScanState.ScanType?, screenName: String) {
        identityAnalyticsRequestFactory.screenPresented(
            scanType = scanType,
            screenName = screenName
        )
    }

    fun trackScreenTransitionFinish(screenName: String) {
        viewModelScope.launch(workContext) {
            screenTracker.screenTransitionFinish(screenName)
        }
    }

    /**
     * Send VerificationSucceeded analytics event with isFromFallbackUrl = false
     * based on values in [analyticsState].
     */
    fun sendSucceededAnalyticsRequestForNative() {
        viewModelScope.launch {
            analyticsState.collectLatest { latestState ->
                identityAnalyticsRequestFactory.verificationSucceeded(
                    isFromFallbackUrl = false,
                    scanType = latestState.scanType,
                    requireSelfie = latestState.requireSelfie,
                    docFrontRetryTimes = latestState.docFrontRetryTimes,
                    docBackRetryTimes = latestState.docBackRetryTimes,
                    selfieRetryTimes = latestState.selfieRetryTimes,
                    docFrontUploadType = latestState.docFrontUploadType,
                    docBackUploadType = latestState.docBackUploadType,
                    docFrontModelScore = latestState.docFrontModelScore,
                    docBackModelScore = latestState.docBackModelScore,
                    selfieModelScore = latestState.selfieModelScore,
                    docFrontBlurScore = latestState.docFrontBlurScore,
                    docBackBlurScore = latestState.docBackBlurScore
                )
            }
        }
    }

    fun clearCollectedData(field: Requirement) {
        // Remove the requirement from _collectedData.
        _collectedData.updateStateAndSave {
            it.clearData(field)
        }
        // Add the requirement to _missingRequirements.
        // If the requirement doesn't appear in initialMissings, don't add
        if (initialMissings.contains(field)) {
            _missingRequirements.updateStateAndSave {
                it.plus(field)
            }
        }
    }

    fun clearDocumentUploadedState() {
        listOf(_documentFrontUploadedState, _documentBackUploadedState).forEach {
            it.updateStateAndSave {
                SingleSideDocumentUploadState()
            }
        }
    }

    fun clearSelfieUploadedState() {
        _selfieUploadedState.updateStateAndSave {
            SelfieUploadState()
        }
    }

    // Reset document upload, selfie upload(if applicable)
    fun resetAllUploadState() {
        clearDocumentUploadedState()
        clearSelfieUploadedState()
    }

    /**
     * Check if there is a outstanding API request being submitted.
     */
    fun isSubmitting(): Boolean {
        return documentFrontUploadedState.value.isLoading() ||
            documentBackUploadedState.value.isLoading() ||
            selfieUploadState.value.isAnyLoading() ||
            verificationPageData.value.status == Status.LOADING ||
            verificationPageSubmit.value.status == Status.LOADING
    }

    fun updateNewScanType(scanType: IdentityScanState.ScanType) {
        updateAnalyticsState { oldState ->
            when (scanType) {
                IdentityScanState.ScanType.DOC_FRONT -> {
                    oldState.copy(
                        docFrontRetryTimes =
                        oldState.docFrontRetryTimes?.let { it + 1 } ?: 0
                    )
                }

                IdentityScanState.ScanType.DOC_BACK -> {
                    oldState.copy(
                        docBackRetryTimes =
                        oldState.docBackRetryTimes?.let { it + 1 } ?: 0
                    )
                }

                IdentityScanState.ScanType.SELFIE -> {
                    oldState.copy(
                        selfieRetryTimes =
                        oldState.selfieRetryTimes?.let { it + 1 } ?: 0
                    )
                }
            }
        }
    }

    /**
     * Check Camera permission, if has permission navigate to [DocumentScanDestination],
     * otherwise [CameraPermissionDeniedDestination]
     */
    fun checkPermissionAndNavigate(
        navController: NavController,
        cameraPermissionEnsureable: CameraPermissionEnsureable
    ) {
        cameraPermissionEnsureable.ensureCameraPermission(
            onCameraReady = {
                identityAnalyticsRequestFactory.cameraPermissionGranted()
                _cameraPermissionGranted.update { true }
                navController.navigateTo(DocumentScanDestination)
            },
            onUserDeniedCameraPermission = {
                identityAnalyticsRequestFactory.cameraPermissionDenied()
                _cameraPermissionGranted.update { false }
                navController.navigateTo(CameraPermissionDeniedDestination)
            }
        )
    }

    suspend fun postVerificationPageDataForIndividual(
        individualCollectedStates: IndividualCollectedStates,
        navController: NavController
    ) {
        postVerificationPageDataAndMaybeNavigate(
            navController,
            individualCollectedStates.toCollectedDataParam(),
            fromRoute = IndividualDestination.ROUTE.route,
            onMissingPhoneOtp = {
                navController.navigateTo(OTPDestination)
            }
        ) {
            submitAndNavigate(
                navController = navController,
                fromRoute = IndividualDestination.ROUTE.route
            )
        }
    }

    /**
     * Post VerificationPageData for force confirm. Currently it's only possible to force confirm
     * [Requirement.IDDOCUMENTFRONT] and [Requirement.IDDOCUMENTBACK] when user clicks continue
     * anyway on error page after document front/back upload fails.
     *
     * After force confirm, the session would ended up in 3 possible possible states
     *  1. missingBack -
     *    uploaded front -> got failure for front from server -> forced confirm front
     *     -> now need to upload back
     *  2. missingSelfie -
     *    uploaded front and back -> got failure for back from server -> forced confirm back
     *     -> session requires selfie -> now need to upload selfie
     *  3. readyToSubmit -
     *    uploaded front and back -> got failure for back from server -> forced confirm back
     *     -> session doesn't selfie -> now need to submit session
     *
     *  Note: missingFront is impossible here -
     *  because force confirming [Requirement.IDDOCUMENTFRONT] ends up fulfilling front,
     *  and force confirming [Requirement.IDDOCUMENTBACK] assumes front is already fulfilled.
     */
    suspend fun postVerificationPageDataForForceConfirm(
        requirementToForceConfirm: Requirement,
        navController: NavController,
        fromRoute: String = ErrorDestination.ROUTE.route
    ) {
        try {
            val (forceConfirmParam, nextDestination) =
                calculateParamForForceConfirm(requirementToForceConfirm)
            postVerificationPageDataAndMaybeNavigate(
                navController = navController,
                collectedDataParam = forceConfirmParam,
                fromRoute = fromRoute,
                onMissingBack = {
                    navController.navigateTo(nextDestination)
                },
                onReadyToSubmit = {
                    submitAndNavigate(navController, fromRoute)
                }
            )
        } catch (e: IllegalStateException) {
            Log.e(TAG, "Failed to postVerificationPageDataForForceConfirm: ${e.message}")
            navController.navigateToFinalErrorScreen(getApplication())
        }
    }

    /**
     * Calculates the parameters for screen transitioning on force confirm, return null if fails to
     * calculate.
     *
     * @return CollectedDataParam to force confirm, and [IdentityTopLevelDestination] to navigate to
     *  if back is missing.
     */
    private fun calculateParamForForceConfirm(
        requirementToForceConfirm: Requirement
    ): Pair<CollectedDataParam, IdentityTopLevelDestination> {
        check(requirementToForceConfirm.supportsForceConfirm()) {
            "Unsupported requirement to forceConfirm: $requirementToForceConfirm"
        }
        val failedDocumentParam =
            if (requirementToForceConfirm == Requirement.IDDOCUMENTFRONT) {
                collectedData.value.idDocumentFront
            } else {
                collectedData.value.idDocumentBack
            }
        val failedUploadMethod = failedDocumentParam?.uploadMethod

        check(failedDocumentParam != null && failedUploadMethod != null) {
            "Failed to calculate params to forceConfirm"
        }
        val collectedDataParamWithForceConfirm =
            if (requirementToForceConfirm == Requirement.IDDOCUMENTFRONT) {
                CollectedDataParam(
                    idDocumentFront = failedDocumentParam.copy(forceConfirm = true)
                )
            } else {
                CollectedDataParam(
                    idDocumentBack = failedDocumentParam.copy(forceConfirm = true)
                )
            }
        val destinationWhenMissingBack =
            when (failedUploadMethod) {
                UploadMethod.AUTOCAPTURE -> {
                    DocumentScanDestination
                }

                UploadMethod.FILEUPLOAD -> {
                    DocumentUploadDestination
                }

                UploadMethod.MANUALCAPTURE -> {
                    DocumentUploadDestination
                }
            }
        return collectedDataParamWithForceConfirm to destinationWhenMissingBack
    }

    /**
     * Post verification with OTP, and decide next step based on result with 3 possible cases.
     *  1. correct OTP - navigate based on missings(could be none or document related)
     *  2. incorrect OTP - missings still contains otp, show inline error on OTP screen
     *  3. requirement.error - show error
     */
    suspend fun postVerificationPageDataForOTP(
        otp: String,
        navController: NavController,
        onMissingOtp: () -> Unit
    ) {
        postVerificationPageDataAndMaybeNavigate(
            navController,
            CollectedDataParam(phoneOtp = otp),
            fromRoute = OTPDestination.ROUTE.route,
            onMissingPhoneOtp = onMissingOtp
        ) {
            submitAndNavigate(
                navController = navController,
                fromRoute = OTPDestination.ROUTE.route
            )
        }
    }

    /**
     * Check the upload status of the document, post it with VerificationPageData, and decide
     * next step based on result.
     *
     * If result is missing back, then start scanning back of the document,
     * else if result is missing selfie, then start scanning selfie,
     * Otherwise submit
     */
    suspend fun collectDataForDocumentScanScreen(
        navController: NavController,
        isFront: Boolean,
        onMissingBack: () -> Unit
    ) {
        if (isFront) {
            documentFrontUploadedState
        } else {
            documentBackUploadedState
        }.collectLatest { uploadedState ->
            if (uploadedState.hasError()) {
                errorCause.postValue(uploadedState.getError())
                navController.navigateToErrorScreenWithDefaultValues(
                    getApplication()
                )
            } else if (uploadedState.isUploaded()) {
                val route = DocumentScanDestination.ROUTE.route
                postVerificationPageDataAndMaybeNavigate(
                    navController = navController,
                    collectedDataParam = if (isFront) {
                        CollectedDataParam.createFromFrontUploadedResultsForAutoCapture(
                            frontHighResResult = requireNotNull(uploadedState.highResResult.data),
                            frontLowResResult = requireNotNull(uploadedState.lowResResult.data)
                        )
                    } else {
                        CollectedDataParam.createFromBackUploadedResultsForAutoCapture(
                            backHighResResult = requireNotNull(uploadedState.highResResult.data),
                            backLowResResult = requireNotNull(uploadedState.lowResResult.data)
                        )
                    },
                    fromRoute = route,
                    onMissingBack = onMissingBack,
                    onReadyToSubmit = {
                        submitAndNavigate(
                            navController = navController,
                            fromRoute = route
                        )
                    }
                )
            }
        }
    }

    /**
     * Collect the last status of both sids of document upload and post.
     * Only post when that side is not yet collected.
     */
    suspend fun collectDataForDocumentUploadScreen(
        navController: NavController,
        isFront: Boolean
    ) {
        if (isFront) {
            frontCollectedInfo.collectLatest { (frontUploadState, collectedData) ->
                if (collectedData.idDocumentFront == null) {
                    if (frontUploadState.hasError()) {
                        errorCause.postValue(frontUploadState.getError())
                        navController.navigateToErrorScreenWithDefaultValues(getApplication())
                    } else if (frontUploadState.isHighResUploaded()) {
                        val front = requireNotNull(frontUploadState.highResResult.data)
                        postVerificationPageData(
                            navController = navController,
                            collectedDataParam = CollectedDataParam(
                                idDocumentFront = DocumentUploadParam(
                                    highResImage = requireNotNull(front.uploadedStripeFile.id) {
                                        "front uploaded file id is null"
                                    },
                                    uploadMethod = requireNotNull(front.uploadMethod)
                                )
                            ),
                            fromRoute = DocumentUploadDestination.ROUTE.route
                        )
                    }
                }
            }
        } else {
            backCollectedInfo.collectLatest { (backUploadedState, collectedData) ->
                if (collectedData.idDocumentBack == null) {
                    if (backUploadedState.hasError()) {
                        errorCause.postValue(backUploadedState.getError())
                        navController.navigateToErrorScreenWithDefaultValues(getApplication())
                    } else if (backUploadedState.isHighResUploaded()) {
                        val back = requireNotNull(backUploadedState.highResResult.data)
                        postVerificationPageData(
                            navController = navController,
                            collectedDataParam = CollectedDataParam(
                                idDocumentBack = DocumentUploadParam(
                                    highResImage = requireNotNull(back.uploadedStripeFile.id) {
                                        "back uploaded file id is null"
                                    },
                                    uploadMethod = requireNotNull(back.uploadMethod)
                                )
                            ),
                            fromRoute = DocumentUploadDestination.ROUTE.route
                        )
                    }
                }
            }
        }
    }

    /**
     * Check the upload status of the [selfieUploadState], post it with VerificationPageData and
     * navigate accordingly.
     */
    suspend fun collectDataForSelfieScreen(
        navController: NavController,
        faceDetectorTransitioner: FaceDetectorTransitioner,
        allowImageCollection: Boolean
    ) {
        selfieUploadState.collectLatest {
            when {
                it.isIdle() -> {} // no-op
                it.isAnyLoading() -> {} // no-op
                it.hasError() -> {
                    errorCause.postValue(it.getError())
                    navController.navigateToErrorScreenWithDefaultValues(getApplication())
                }

                it.isAllUploaded() -> {
                    runCatching {
                        postVerificationPageDataAndMaybeNavigate(
                            navController = navController,
                            collectedDataParam = CollectedDataParam.createForSelfie(
                                firstHighResResult = requireNotNull(it.firstHighResResult.data),
                                firstLowResResult = requireNotNull(it.firstLowResResult.data),
                                lastHighResResult = requireNotNull(it.lastHighResResult.data),
                                lastLowResResult = requireNotNull(it.lastLowResResult.data),
                                bestHighResResult = requireNotNull(it.bestHighResResult.data),
                                bestLowResResult = requireNotNull(it.bestLowResResult.data),
                                trainingConsent = allowImageCollection,
                                faceScoreVariance = faceDetectorTransitioner.scoreVariance,
                                bestFaceScore = faceDetectorTransitioner.bestFaceScore,
                                numFrames = faceDetectorTransitioner.numFrames
                            ),
                            fromRoute = SelfieDestination.ROUTE.route
                        ) {
                            submitAndNavigate(
                                navController = navController,
                                fromRoute = SelfieDestination.ROUTE.route
                            )
                        }
                    }.onFailure { throwable ->
                        errorCause.postValue(throwable)
                        navController.navigateToErrorScreenWithDefaultValues(getApplication())
                    }
                }

                else -> {
                    errorCause.postValue(
                        IllegalStateException(
                            "collectSelfieUploadedStateAndPost " +
                                "reaches unexpected upload state: $it"
                        )
                    )
                    navController.navigateToErrorScreenWithDefaultValues(getApplication())
                }
            }
        }
    }

    /**
     * Registers for the [ActivityResultLauncher]s to take photo or pick image, should be called
     * during initialization of an Activity or Fragment.
     */
    fun registerActivityResultCaller(
        activityResultCaller: ActivityResultCaller
    ) {
        imageHandler.registerActivityResultCaller(
            activityResultCaller,
            savedStateHandle,
            onFrontPhotoTaken = { uri ->
                uploadManualResult(
                    uri = uri,
                    isFront = true,
                    docCapturePage = requireNotNull(verificationPage.value?.data).documentCapture,
                    uploadMethod = UploadMethod.MANUALCAPTURE,
                    scanType = IdentityScanState.ScanType.DOC_FRONT
                )
            },
            onBackPhotoTaken = { uri ->
                uploadManualResult(
                    uri = uri,
                    isFront = false,
                    docCapturePage = requireNotNull(verificationPage.value?.data).documentCapture,
                    uploadMethod = UploadMethod.MANUALCAPTURE,
                    scanType = IdentityScanState.ScanType.DOC_BACK
                )
            },
            onFrontImageChosen = { uri ->
                uploadManualResult(
                    uri = uri,
                    isFront = true,
                    docCapturePage = requireNotNull(verificationPage.value?.data).documentCapture,
                    uploadMethod = UploadMethod.FILEUPLOAD,
                    scanType = IdentityScanState.ScanType.DOC_FRONT
                )
            },
            onBackImageChosen = { uri ->
                uploadManualResult(
                    uri = uri,
                    isFront = false,
                    docCapturePage = requireNotNull(verificationPage.value?.data).documentCapture,
                    uploadMethod = UploadMethod.FILEUPLOAD,
                    scanType = IdentityScanState.ScanType.DOC_BACK
                )
            }
        )
    }

    fun visitedIndividualWelcome() {
        _visitedIndividualWelcomeScreen.updateStateAndSave { true }
    }

    private fun logError(cause: Throwable) {
        identityAnalyticsRequestFactory.genericError(
            cause.message,
            cause.stackTraceToString()
        )
    }

    private fun <State> MutableStateFlow<State>.updateStateAndSave(function: (State) -> State) {
        this.update(function)
        savedStateHandle[
            when (this) {
                _selfieUploadedState -> SELFIE_UPLOAD_STATE
                _analyticsState -> ANALYTICS_STATE
                _documentFrontUploadedState -> DOCUMENT_FRONT_UPLOAD_STATE
                _documentBackUploadedState -> DOCUMENT_BACK_UPLOAD_STATE
                _collectedData -> COLLECTED_DATA
                _missingRequirements -> MISSING_REQUIREMENTS
                _cameraPermissionGranted -> CAMERA_PERMISSION_GRANTED
                verificationPageData -> VERIFICATION_PAGE_DATA
                verificationPageSubmit -> VERIFICATION_PAGE_SUBMIT
                _visitedIndividualWelcomeScreen -> VISITED_INDIVIDUAL_WELCOME_PAGE
                else -> {
                    throw IllegalStateException("Unexpected state flow: $this")
                }
            }
        ] = this.value
    }

    internal class IdentityViewModelFactory(
        private val applicationSupplier: () -> Application,
        private val uiContextSupplier: () -> CoroutineContext,
        private val workContextSupplier: () -> CoroutineContext,
        private val subcomponentSupplier: () -> IdentityActivitySubcomponent,
        private val finishWithResult: (IdentityVerificationSheet.VerificationFlowResult) -> Unit,
    ) : ViewModelProvider.Factory {

        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T {
            val subcomponent = subcomponentSupplier()
            val savedStateHandle = extras.createSavedStateHandle()

            return IdentityViewModel(
                applicationSupplier(),
                subcomponent.verificationArgs,
                subcomponent.identityRepository,
                subcomponent.identityModelFetcher,
                subcomponent.identityIO,
                subcomponent.identityAnalyticsRequestFactory,
                subcomponent.screenTracker,
                subcomponent.identityImageHandler,
                subcomponent.tfLiteInitializer,
                savedStateHandle,
                uiContextSupplier(),
                workContextSupplier(),
                finishWithResult
            ) as T
        }
    }

    internal companion object {
        val TAG: String = IdentityViewModel::class.java.simpleName
        const val FRONT = "front"
        const val BACK = "back"
        const val BYTES_IN_KB = 1024
        private const val DOCUMENT_FRONT_UPLOAD_STATE = "document_front_upload_state"
        private const val DOCUMENT_BACK_UPLOAD_STATE = "document_back_upload_state"
        private const val SELFIE_UPLOAD_STATE = "selfie_upload_state"
        private const val ANALYTICS_STATE = "analytics_upload_state"
        private const val COLLECTED_DATA = "collected_data"
        private const val MISSING_REQUIREMENTS = "missing_requirements"
        private const val CAMERA_PERMISSION_GRANTED = "cameraPermissionGranted"
        private const val VERIFICATION_PAGE = "verification_page"
        private const val VERIFICATION_PAGE_DATA = "verification_page_data"
        private const val VERIFICATION_PAGE_SUBMIT = "verification_page_submit"
        private const val VISITED_INDIVIDUAL_WELCOME_PAGE = "visited_individual_welcome_page"
    }
}
