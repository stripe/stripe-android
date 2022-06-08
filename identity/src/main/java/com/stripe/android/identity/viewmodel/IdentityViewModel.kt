package com.stripe.android.identity.viewmodel

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.util.Log
import androidx.annotation.VisibleForTesting
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.stripe.android.camera.AppSettingsOpenable
import com.stripe.android.camera.CameraPermissionEnsureable
import com.stripe.android.camera.framework.image.longerEdge
import com.stripe.android.core.exception.APIConnectionException
import com.stripe.android.core.exception.APIException
import com.stripe.android.core.injection.Injectable
import com.stripe.android.core.injection.injectWithFallback
import com.stripe.android.core.model.StripeFilePurpose
import com.stripe.android.identity.FallbackUrlLauncher
import com.stripe.android.identity.IdentityVerificationSheetContract
import com.stripe.android.identity.VerificationFlowFinishable
import com.stripe.android.identity.camera.IdentityAggregator
import com.stripe.android.identity.injection.DaggerIdentityViewModelFactoryComponent
import com.stripe.android.identity.injection.IdentityViewModelSubcomponent
import com.stripe.android.identity.ml.BoundingBox
import com.stripe.android.identity.ml.FaceDetectorAnalyzer
import com.stripe.android.identity.ml.FaceDetectorOutput
import com.stripe.android.identity.ml.IDDetectorOutput
import com.stripe.android.identity.navigation.IdentityFragmentFactory
import com.stripe.android.identity.networking.DocumentUploadState
import com.stripe.android.identity.networking.IdentityModelFetcher
import com.stripe.android.identity.networking.IdentityRepository
import com.stripe.android.identity.networking.Resource
import com.stripe.android.identity.networking.SelfieUploadState
import com.stripe.android.identity.networking.Status
import com.stripe.android.identity.networking.UploadedResult
import com.stripe.android.identity.networking.models.ClearDataParam
import com.stripe.android.identity.networking.models.CollectedDataParam
import com.stripe.android.identity.networking.models.DocumentUploadParam.UploadMethod
import com.stripe.android.identity.networking.models.VerificationPage
import com.stripe.android.identity.networking.models.VerificationPageData
import com.stripe.android.identity.networking.models.VerificationPageStaticContentDocumentCapturePage
import com.stripe.android.identity.networking.models.VerificationPageStaticContentSelfieCapturePage
import com.stripe.android.identity.states.FaceDetectorTransitioner
import com.stripe.android.identity.states.IdentityScanState
import com.stripe.android.identity.utils.IdentityIO
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject
import javax.inject.Provider

/**
 * ViewModel hosted by IdentityActivity, shared across fragments.
 */

internal class IdentityViewModel @Inject constructor(
    internal val verificationArgs: IdentityVerificationSheetContract.Args,
    private val identityRepository: IdentityRepository,
    private val identityModelFetcher: IdentityModelFetcher,
    private val identityIO: IdentityIO,
    val identityFragmentFactory: IdentityFragmentFactory,
) : ViewModel() {

    /**
     * StateFlow to track the upload status of high/low resolution image of front and back for documents.
     */
    private val _documentUploadedState = MutableStateFlow(DocumentUploadState())
    val documentUploadState: StateFlow<DocumentUploadState> = _documentUploadedState

    /**
     * StateFlow to track the upload status of high/low resolution images of selfies.
     */
    private val _selfieUploadedState = MutableStateFlow(SelfieUploadState())
    val selfieUploadState: StateFlow<SelfieUploadState> = _selfieUploadedState

    /**
     * Response for initial VerificationPage, used for building UI.
     */
    private val _verificationPage = MutableLiveData<Resource<VerificationPage>>()
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
                }
            }
        }

        private fun maybePostSuccess() {
            page?.let { page ->
                idDetectorModel?.let { idDetectorModel ->
                    if (faceDetectorModelValueSet) {
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
     * Reset document uploaded state to loading state.
     */
    internal fun resetDocumentUploadedState() {
        _documentUploadedState.update {
            DocumentUploadState()
        }
    }

    /**
     * Reset selfie uploaded state to loading state.
     */
    internal fun resetSelfieUploadedState() {
        _selfieUploadedState.update {
            SelfieUploadState()
        }
    }

    /**
     * Upload high_res of an image Uri manually picked from local file storage or taken from camera.
     */
    internal fun uploadManualResult(
        uri: Uri,
        isFront: Boolean,
        docCapturePage: VerificationPageStaticContentDocumentCapturePage,
        uploadMethod: UploadMethod
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
            isFront = isFront
        )
    }

    /**
     * Upload high_res and low_res of the [IdentityAggregator.FinalResult] from scan.
     */
    internal fun uploadScanResult(
        result: IdentityAggregator.FinalResult,
        verificationPage: VerificationPage,
        targetScanType: IdentityScanState.ScanType?
    ) {
        when (result.result) {
            is IDDetectorOutput -> {
                val originalBitmap = result.frame.cameraPreviewImage.image
                val boundingBox = result.result.boundingBox
                val scores = result.result.allScores

                val isFront = when (targetScanType) {
                    IdentityScanState.ScanType.ID_FRONT -> true
                    IdentityScanState.ScanType.ID_BACK -> false
                    IdentityScanState.ScanType.DL_FRONT -> true
                    IdentityScanState.ScanType.DL_BACK -> false
                    // passport is always uploaded as front
                    IdentityScanState.ScanType.PASSPORT -> true
                    else -> {
                        Log.e(TAG, "incorrect targetScanType: $targetScanType")
                        throw IllegalStateException("incorrect targetScanType: $targetScanType")
                    }
                }
                // upload high res
                processDocumentScanResultAndUpload(
                    originalBitmap,
                    boundingBox,
                    verificationPage.documentCapture,
                    isHighRes = true,
                    isFront = isFront,
                    scores
                )

                // upload low res
                processDocumentScanResultAndUpload(
                    originalBitmap,
                    boundingBox,
                    verificationPage.documentCapture,
                    isHighRes = false,
                    isFront = isFront,
                    scores
                )
            }
            is FaceDetectorOutput -> {
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
        }
    }

    /**
     * Processes document scan result by cropping and padding the bitmap if necessary,
     * then upload the processed file.
     */
    @VisibleForTesting
    internal fun processDocumentScanResultAndUpload(
        originalBitmap: Bitmap,
        boundingBox: BoundingBox,
        docCapturePage: VerificationPageStaticContentDocumentCapturePage,
        isHighRes: Boolean,
        isFront: Boolean,
        scores: List<Float>
    ) {
        identityIO.resizeBitmapAndCreateFileToUpload(
            bitmap =
            if (isHighRes)
                identityIO.cropAndPadBitmap(
                    originalBitmap,
                    boundingBox,
                    originalBitmap.longerEdge() * docCapturePage.highResImageCropPadding
                )
            else
                originalBitmap,
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
            if (isHighRes)
                docCapturePage.highResImageMaxDimension
            else
                docCapturePage.lowResImageMaxDimension,
            compressionQuality =
            if (isHighRes)
                docCapturePage.highResImageCompressionQuality
            else
                docCapturePage.lowResImageCompressionQuality
        ).let { imageFile ->
            uploadDocumentImagesAndNotify(
                imageFile = imageFile,
                filePurpose = requireNotNull(
                    StripeFilePurpose.fromCode(docCapturePage.filePurpose)
                ),
                uploadMethod = UploadMethod.AUTOCAPTURE,
                scores = scores,
                isHighRes = isHighRes,
                isFront = isFront
            )
        }
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
        isFront: Boolean
    ) {
        viewModelScope.launch {
            runCatching {
                identityRepository.uploadImage(
                    verificationId = verificationArgs.verificationSessionId,
                    ephemeralKey = verificationArgs.ephemeralKeySecret,
                    imageFile = imageFile,
                    filePurpose = filePurpose
                )
            }.fold(
                onSuccess = { uploadedStripeFile ->
                    _documentUploadedState.update { currentState ->
                        currentState.update(
                            isHighRes = isHighRes,
                            isFront = isFront,
                            newResult = UploadedResult(
                                uploadedStripeFile,
                                scores,
                                uploadMethod
                            )
                        )
                    }
                },
                onFailure = {
                    _documentUploadedState.update { currentState ->
                        currentState.updateError(
                            isHighRes = isHighRes,
                            isFront = isFront,
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
            if (isHighRes)
                identityIO.cropAndPadBitmap(
                    originalBitmap,
                    boundingBox,
                    boundingBox.width * FaceDetectorAnalyzer.INPUT_WIDTH * selfieCapturePage.highResImageCropPadding
                )
            else
                originalBitmap,
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
            if (isHighRes)
                selfieCapturePage.highResImageMaxDimension
            else
                selfieCapturePage.lowResImageMaxDimension,
            compressionQuality =
            if (isHighRes)
                selfieCapturePage.highResImageCompressionQuality
            else
                selfieCapturePage.lowResImageCompressionQuality
        ).let { imageFile ->
            uploadSelfieImagesAndNotify(
                imageFile = imageFile,
                filePurpose = requireNotNull(
                    StripeFilePurpose.fromCode(selfieCapturePage.filePurpose)
                ),
                isHighRes = isHighRes,
                selfie = selfie
            )
        }
    }

    private fun uploadSelfieImagesAndNotify(
        imageFile: File,
        filePurpose: StripeFilePurpose,
        isHighRes: Boolean,
        selfie: FaceDetectorTransitioner.Selfie
    ) {
        viewModelScope.launch {
            runCatching {
                identityRepository.uploadImage(
                    verificationId = verificationArgs.verificationSessionId,
                    ephemeralKey = verificationArgs.ephemeralKeySecret,
                    imageFile = imageFile,
                    filePurpose = filePurpose
                )
            }.fold(
                onSuccess = { uploadedStripeFile ->
                    _selfieUploadedState.update { currentState ->
                        currentState.update(
                            isHighRes = isHighRes,
                            newResult = UploadedResult(
                                uploadedStripeFile
                            ),
                            selfie = selfie
                        )
                    }
                },
                onFailure = {
                    _selfieUploadedState.update { currentState ->
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
        onFailure: (Throwable?) -> Unit,
    ) {
        verificationPage.observe(owner) { resource ->
            when (resource.status) {
                Status.SUCCESS -> {
                    onSuccess(requireNotNull(resource.data))
                }
                Status.ERROR -> {
                    Log.e(TAG, "Fail to get VerificationPage")
                    onFailure(resource.throwable)
                }
                Status.LOADING -> {} // no-op
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
                onSuccess = {
                    _verificationPage.postValue(Resource.success(it))
                    if (shouldRetrieveModel) {
                        downloadModelAndPost(
                            it.documentCapture.models.idDetectorUrl,
                            _idDetectorModelFile
                        )
                        it.selfieCapture?.let { selfieCapture ->
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
                    _verificationPage.postValue(
                        Resource.error(
                            "Failed to retrieve verification page with " +
                                "sessionID: ${verificationArgs.verificationSessionId} and ephemeralKey: ${verificationArgs.ephemeralKeySecret}",
                            it
                        ),
                    )
                }
            )
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
                }
            )
        }
    }

    /**
     * Post collected [CollectedDataParam] to update [VerificationPageData].
     */
    @Throws(
        APIConnectionException::class,
        APIException::class
    )
    suspend fun postVerificationPageData(
        collectedDataParam: CollectedDataParam,
        clearDataParam: ClearDataParam
    ) =
        identityRepository.postVerificationPageData(
            verificationArgs.verificationSessionId,
            verificationArgs.ephemeralKeySecret,
            collectedDataParam,
            clearDataParam
        )

    /**
     * Submit the final [VerificationPageData].
     */
    @Throws(
        APIConnectionException::class,
        APIException::class
    )
    suspend fun postVerificationPageSubmit() =
        identityRepository.postVerificationPageSubmit(
            verificationArgs.verificationSessionId,
            verificationArgs.ephemeralKeySecret
        )

    internal class IdentityViewModelFactory(
        val context: Context,
        private val verificationArgsSupplier: () -> IdentityVerificationSheetContract.Args,
        private val cameraPermissionEnsureable: CameraPermissionEnsureable,
        private val appSettingsOpenable: AppSettingsOpenable,
        private val verificationFlowFinishable: VerificationFlowFinishable,
        private val fallbackUrlLauncher: FallbackUrlLauncher,
    ) : ViewModelProvider.Factory, Injectable<Context> {
        @Inject
        lateinit var subComponentBuilderProvider: Provider<IdentityViewModelSubcomponent.Builder>

        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            val args = verificationArgsSupplier()
            injectWithFallback(
                args.injectorKey,
                context
            )

            return subComponentBuilderProvider.get()
                .args(args)
                .cameraPermissionEnsureable(cameraPermissionEnsureable)
                .appSettingsOpenable(appSettingsOpenable)
                .verificationFlowFinishable(verificationFlowFinishable)
                .identityViewModelFactory(this)
                .fallbackUrlLauncher(fallbackUrlLauncher)
                .build().viewModel as T
        }

        override fun fallbackInitialize(arg: Context) {
            DaggerIdentityViewModelFactoryComponent.builder()
                .context(context)
                .build().inject(this)
        }
    }

    internal companion object {
        val TAG: String = IdentityViewModel::class.java.simpleName
        const val FRONT = "front"
        const val BACK = "back"
    }
}
