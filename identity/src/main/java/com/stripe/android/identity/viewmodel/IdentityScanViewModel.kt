package com.stripe.android.identity.viewmodel

import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import androidx.annotation.VisibleForTesting
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.stripe.android.core.model.InternalStripeFile
import com.stripe.android.core.model.InternalStripeFilePurpose
import com.stripe.android.identity.IdentityVerificationSheetContract
import com.stripe.android.identity.camera.IDDetectorAggregator
import com.stripe.android.identity.ml.BoundingBox
import com.stripe.android.identity.networking.IdentityRepository
import com.stripe.android.identity.networking.Resource
import com.stripe.android.identity.networking.models.VerificationPageStaticContentDocumentCapturePage
import com.stripe.android.identity.states.IdentityScanState
import com.stripe.android.identity.states.IdentityScanState.ScanType.DL_BACK
import com.stripe.android.identity.states.IdentityScanState.ScanType.DL_FRONT
import com.stripe.android.identity.states.IdentityScanState.ScanType.ID_BACK
import com.stripe.android.identity.states.IdentityScanState.ScanType.ID_FRONT
import com.stripe.android.identity.states.IdentityScanState.ScanType.PASSPORT
import com.stripe.android.identity.utils.PairMediatorLiveData
import com.stripe.android.identity.utils.cropAndPadBitmap
import com.stripe.android.identity.utils.resizeBitmapAndCreateFileToUpload
import kotlinx.coroutines.launch

internal class IdentityScanViewModel(
    private val identityRepository: IdentityRepository,
    private val verificationArgs: IdentityVerificationSheetContract.Args
) : CameraViewModel() {

    /**
     * Wrapper class for the uploaded param.
     */
    internal data class UploadedResult(
        val uploadedStripeFile: InternalStripeFile,
        val scores: List<Float>
    )

    /**
     * The target ScanType of current scan.
     */
    internal var targetScanType: IdentityScanState.ScanType? = null

    private val _frontHighResUploaded = MutableLiveData<Resource<UploadedResult>>()
    private val frontHighResUploaded: LiveData<Resource<UploadedResult>> = _frontHighResUploaded

    private val _backHighResUploaded = MutableLiveData<Resource<UploadedResult>>()
    private val backHighResUploaded: LiveData<Resource<UploadedResult>> = _backHighResUploaded

    private val _frontLowResUploaded = MutableLiveData<Resource<UploadedResult>>()
    private val frontLowResUploaded: LiveData<Resource<UploadedResult>> = _frontLowResUploaded

    private val _backLowResUploaded = MutableLiveData<Resource<UploadedResult>>()
    private val backLowResUploaded: LiveData<Resource<UploadedResult>> = _backLowResUploaded

    internal val frontUploaded =
        PairMediatorLiveData(frontHighResUploaded, frontLowResUploaded)

    private val backUploaded =
        PairMediatorLiveData(backHighResUploaded, backLowResUploaded)

    internal val bothUploaded = PairMediatorLiveData(frontUploaded, backUploaded)

    /**
     * Upload high_res and low_res of the captured image for targetScanType.
     */
    internal fun uploadResult(
        result: IDDetectorAggregator.FinalResult,
        context: Context,
        docCapturePage: VerificationPageStaticContentDocumentCapturePage,
    ) {
        val originalBitmap = result.frame.cameraPreviewImage.image
        val boundingBox = result.result.boundingBox
        val scores = result.result.allScores

        val isFront = when (targetScanType) {
            ID_FRONT -> true
            ID_BACK -> false
            DL_FRONT -> true
            DL_BACK -> false
            // passport is always uploaded as front
            PASSPORT -> true
            else -> {
                Log.e(TAG, "incorrect targetScanType: $targetScanType")
                throw IllegalStateException("incorrect targetScanType: $targetScanType")
            }
        }
        // upload high res
        uploadImageAndNotify(
            context,
            originalBitmap,
            boundingBox,
            docCapturePage,
            isHighRes = true,
            isFront = isFront,
            scores
        )

        // upload low res
        uploadImageAndNotify(
            context,
            originalBitmap,
            boundingBox,
            docCapturePage,
            isHighRes = false,
            isFront = isFront,
            scores
        )
    }

    /**
     * Saves a bitmap as a file and upload it. Notifies its corresponding result livedata.
     *
     * @param isHighRes - if true then first crop and pad the bitmap before saving it.
     */
    @VisibleForTesting
    internal fun uploadImageAndNotify(
        context: Context,
        originalBitmap: Bitmap,
        boundingBox: BoundingBox,
        docCapturePage: VerificationPageStaticContentDocumentCapturePage,
        isHighRes: Boolean,
        isFront: Boolean,
        scores: List<Float>
    ) {
        resizeBitmapAndCreateFileToUpload(
            context = context,
            bitmap =
            if (isHighRes)
                cropAndPadBitmap(originalBitmap, boundingBox, docCapturePage)
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
            val resultLiveData =
                when {
                    isHighRes && isFront -> _frontHighResUploaded
                    isHighRes && !isFront -> _backHighResUploaded
                    !isHighRes && isFront -> _frontLowResUploaded
                    !isHighRes && !isFront -> _backLowResUploaded
                    else -> throw IllegalStateException(
                        "Illegal state: isHighRes=$isHighRes, isFront=$isFront"
                    )
                }
            resultLiveData.postValue(Resource.loading())
            viewModelScope.launch {
                runCatching {
                    identityRepository.uploadImage(
                        verificationId = verificationArgs.verificationSessionId,
                        ephemeralKey = verificationArgs.ephemeralKeySecret,
                        imageFile = imageFile,
                        filePurpose = requireNotNull(
                            InternalStripeFilePurpose.fromCode(docCapturePage.filePurpose)
                        )
                    )
                }.fold(
                    onSuccess = { uploadedStripeFile ->
                        resultLiveData.postValue(
                            Resource.success(
                                UploadedResult(
                                    uploadedStripeFile,
                                    scores
                                )
                            )
                        )
                    },
                    onFailure = {
                        resultLiveData.postValue(
                            Resource.error(
                                "Failed to upload file : ${imageFile.name}",
                                throwable = it
                            )
                        )
                    }
                )
            }
        }
    }

    internal class IdentityScanViewModelFactory(
        private val identityRepository: IdentityRepository,
        private val verificationArgs: IdentityVerificationSheetContract.Args
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return IdentityScanViewModel(identityRepository, verificationArgs) as T
        }
    }

    private companion object {
        val TAG: String = IdentityScanViewModel::class.java.simpleName
        const val FRONT = "front"
        const val BACK = "back"
    }
}
