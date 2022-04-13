package com.stripe.android.identity.viewmodel

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
import com.stripe.android.core.exception.APIConnectionException
import com.stripe.android.core.exception.APIException
import com.stripe.android.core.model.StripeFile
import com.stripe.android.core.model.StripeFilePurpose
import com.stripe.android.identity.IdentityVerificationSheetContract
import com.stripe.android.identity.camera.IDDetectorAggregator
import com.stripe.android.identity.ml.BoundingBox
import com.stripe.android.identity.networking.IDDetectorFetcher
import com.stripe.android.identity.networking.IdentityRepository
import com.stripe.android.identity.networking.Resource
import com.stripe.android.identity.networking.Status
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

/**
 * ViewModel hosted by IdentityActivity, shared across fragments.
 */
internal class IdentityViewModel(
    internal val args: IdentityVerificationSheetContract.Args,
    private val identityRepository: IdentityRepository,
    private val idDetectorFetcher: IDDetectorFetcher,
    private val verificationArgs: IdentityVerificationSheetContract.Args,
    private val identityIO: IdentityIO
) : ViewModel() {

    /**
     * Indicates the update states of 4 images.
     */
    internal data class UploadState(
        val frontHighResResult: Resource<UploadedResult> = Resource.loading(),
        val frontLowResResult: Resource<UploadedResult> = Resource.loading(),
        val backHighResResult: Resource<UploadedResult> = Resource.loading(),
        val backLowResResult: Resource<UploadedResult> = Resource.loading()
    ) {

        fun update(
            isHighRes: Boolean,
            isFront: Boolean,
            newResult: UploadedResult
        ) = if (isHighRes) {
            if (isFront) {
                this.copy(
                    frontHighResResult = Resource.success(newResult)
                )
            } else {
                this.copy(
                    backHighResResult = Resource.success(newResult)
                )
            }
        } else {
            if (isFront) {
                this.copy(
                    frontLowResResult = Resource.success(newResult)
                )
            } else {
                this.copy(
                    backLowResResult = Resource.success(newResult)
                )
            }
        }

        fun updateError(
            isHighRes: Boolean,
            isFront: Boolean,
            message: String,
            throwable: Throwable
        ) = if (isHighRes) {
            if (isFront) {
                this.copy(
                    frontHighResResult = Resource.error(msg = message, throwable = throwable)
                )
            } else {
                this.copy(
                    backHighResResult = Resource.error(msg = message, throwable = throwable)
                )
            }
        } else {
            if (isFront) {
                this.copy(
                    frontLowResResult = Resource.error(msg = message, throwable = throwable)
                )
            } else {
                this.copy(
                    backLowResResult = Resource.error(msg = message, throwable = throwable)
                )
            }
        }

        fun isAnyLoading() =
            frontHighResResult.status == Status.LOADING ||
                backHighResResult.status == Status.LOADING ||
                frontLowResResult.status == Status.LOADING ||
                backLowResResult.status == Status.LOADING

        fun isFrontLoading() =
            frontHighResResult.status == Status.LOADING ||
                frontLowResResult.status == Status.LOADING

        fun hasError() =
            frontHighResResult.status == Status.ERROR ||
                backHighResResult.status == Status.ERROR ||
                frontLowResResult.status == Status.ERROR ||
                backHighResResult.status == Status.ERROR

        fun getError(): Throwable {
            StringBuilder().let { errorMessageBuilder ->
                if (frontHighResResult.status == Status.ERROR) {
                    errorMessageBuilder.appendLine(frontHighResResult.message)
                }
                if (frontLowResResult.status == Status.ERROR) {
                    errorMessageBuilder.appendLine(frontLowResResult.message)
                }
                if (backHighResResult.status == Status.ERROR) {
                    errorMessageBuilder.appendLine(backHighResResult.message)
                }
                if (backLowResResult.status == Status.ERROR) {
                    errorMessageBuilder.appendLine(backLowResResult.message)
                }
                return IllegalStateException(errorMessageBuilder.toString())
            }
        }

        fun isFrontUploaded() =
            frontHighResResult.status == Status.SUCCESS &&
                frontLowResResult.status == Status.SUCCESS

        fun isBothUploaded() =
            frontHighResResult.status == Status.SUCCESS &&
                backHighResResult.status == Status.SUCCESS &&
                frontLowResResult.status == Status.SUCCESS &&
                backLowResResult.status == Status.SUCCESS

        fun isFrontHighResUploaded() =
            frontHighResResult.status == Status.SUCCESS

        fun isBackHighResUploaded() =
            backHighResResult.status == Status.SUCCESS

        fun isHighResUploaded() = isFrontHighResUploaded() && isBackHighResUploaded()
    }

    /**
     * Wrapper class for the uploaded param.
     */
    internal data class UploadedResult(
        val uploadedStripeFile: StripeFile,
        val scores: List<Float>?,
        val uploadMethod: UploadMethod
    )

    /**
     * StateFlow to track the upload status of high/low resolution image of front and back.
     */
    private val _uploadedState = MutableStateFlow(UploadState())
    val uploadState: StateFlow<UploadState> = _uploadedState

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
     * Reset uploaded state to loading state.
     */
    internal fun resetUploadedState() {
        _uploadedState.update {
            UploadState()
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
        uploadImageAndNotify(
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
     * Upload high_res and low_res of the [IDDetectorAggregator.FinalResult] from scan.
     */
    internal fun uploadScanResult(
        result: IDDetectorAggregator.FinalResult,
        docCapturePage: VerificationPageStaticContentDocumentCapturePage,
        targetScanType: IdentityScanState.ScanType?
    ) {
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
        processScanResultAndUpload(
            originalBitmap,
            boundingBox,
            docCapturePage,
            isHighRes = true,
            isFront = isFront,
            scores
        )

        // upload low res
        processScanResultAndUpload(
            originalBitmap,
            boundingBox,
            docCapturePage,
            isHighRes = false,
            isFront = isFront,
            scores
        )
    }

    /**
     * Processes scan result by cropping and padding the bitmap if necessary,
     * then upload the processed file.
     *
     * @param isHighRes - if true then first crop and pad the bitmap before saving it.
     */
    @VisibleForTesting
    internal fun processScanResultAndUpload(
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
                identityIO.cropAndPadBitmap(originalBitmap, boundingBox, docCapturePage)
            else
                originalBitmap,
            verificationId = verificationArgs.verificationSessionId,
            isFullFrame = !isHighRes,
            side = if (isFront) FRONT else BACK,
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
            uploadImageAndNotify(
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
    private fun uploadImageAndNotify(
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
                    _uploadedState.update { currentState ->
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
                    _uploadedState.update { currentState ->
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
                    args.verificationSessionId,
                    args.ephemeralKeySecret
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
                                "sessionID: ${args.verificationSessionId} and ephemeralKey: ${args.ephemeralKeySecret}",
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
                idDetectorFetcher.fetchIDDetector(modelUrl)
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
            args.verificationSessionId,
            args.ephemeralKeySecret,
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
            args.verificationSessionId,
            args.ephemeralKeySecret
        )

    internal class IdentityViewModelFactory(
        private val args: IdentityVerificationSheetContract.Args,
        private val identityRepository: IdentityRepository,
        private val idDetectorFetcher: IDDetectorFetcher,
        private val verificationArgs: IdentityVerificationSheetContract.Args,
        private val identityIO: IdentityIO
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return IdentityViewModel(
                args,
                identityRepository,
                idDetectorFetcher,
                verificationArgs,
                identityIO
            ) as T
        }
    }

    private companion object {
        val TAG: String = IdentityViewModel::class.java.simpleName
        const val FRONT = "front"
        const val BACK = "back"
    }
}
