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
import com.stripe.android.identity.IdentityVerificationSheetContract
import com.stripe.android.identity.VerificationFlowFinishable
import com.stripe.android.identity.camera.IdentityAggregator
import com.stripe.android.identity.injection.DaggerIdentityViewModelFactoryComponent
import com.stripe.android.identity.injection.IdentityViewModelSubcomponent
import com.stripe.android.identity.ml.BoundingBox
import com.stripe.android.identity.ml.IDDetectorOutput
import com.stripe.android.identity.navigation.IdentityFragmentFactory
import com.stripe.android.identity.networking.DocumentUploadState
import com.stripe.android.identity.networking.IdentityModelFetcher
import com.stripe.android.identity.networking.IdentityRepository
import com.stripe.android.identity.networking.Resource
import com.stripe.android.identity.networking.Status
import com.stripe.android.identity.networking.UploadedResult
import com.stripe.android.identity.networking.models.ClearDataParam
import com.stripe.android.identity.networking.models.CollectedDataParam
import com.stripe.android.identity.networking.models.DocumentUploadParam.UploadMethod
import com.stripe.android.identity.networking.models.VerificationPage
import com.stripe.android.identity.networking.models.VerificationPageData
import com.stripe.android.identity.networking.models.VerificationPageStaticContentDocumentCapturePage
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
    val identityFragmentFactory: IdentityFragmentFactory
) : ViewModel() {

    /**
     * StateFlow to track the upload status of high/low resolution image of front and back for documents.
     */
    private val _documentUploadedState = MutableStateFlow(DocumentUploadState())
    val documentUploadState: StateFlow<DocumentUploadState> = _documentUploadedState

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
     * Wrapper for both page and model
     */
    val pageAndModel = object : MediatorLiveData<Resource<Pair<VerificationPage, File>>>() {
        private var page: VerificationPage? = null
        private var model: File? = null

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
                        model = it.data
                        maybePostSuccess()
                    }
                    Status.ERROR -> {
                        postValue(Resource.error("$idDetectorModelFile posts error"))
                    }
                    Status.LOADING -> {} // no-op
                }
            }
        }

        private fun maybePostSuccess() {
            page?.let { page ->
                model?.let { model ->
                    postValue(Resource.success(Pair(page, model)))
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
        documentCapturePage: VerificationPageStaticContentDocumentCapturePage,
        targetScanType: IdentityScanState.ScanType?
    ) {
        require(result.result is IDDetectorOutput) {
            "Unexpected output type: ${result.result}"
        }
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
            documentCapturePage,
            isHighRes = true,
            isFront = isFront,
            scores
        )

        // upload low res
        processDocumentScanResultAndUpload(
            originalBitmap,
            boundingBox,
            documentCapturePage,
            isHighRes = false,
            isFront = isFront,
            scores
        )
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
        viewModelScope.launch {
            runCatching {
                _verificationPage.postValue(Resource.loading())
                identityRepository.retrieveVerificationPage(
                    verificationArgs.verificationSessionId,
                    verificationArgs.ephemeralKeySecret
                )
            }.fold(
                onSuccess = {
                    _verificationPage.postValue(Resource.success(it))
                    if (shouldRetrieveModel) {
                        downloadIDDetectorModel(it.documentCapture.models.idDetectorUrl)
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
     * Download the IDDetector model and post its value to [idDetectorModelFile].
     */
    private fun downloadIDDetectorModel(modelUrl: String) {
        viewModelScope.launch {
            runCatching {
                _idDetectorModelFile.postValue(Resource.loading())
                identityModelFetcher.fetchIdentityModel(modelUrl)
            }.fold(
                onSuccess = {
                    _idDetectorModelFile.postValue(Resource.success(it))
                },
                onFailure = {
                    _idDetectorModelFile.postValue(
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
