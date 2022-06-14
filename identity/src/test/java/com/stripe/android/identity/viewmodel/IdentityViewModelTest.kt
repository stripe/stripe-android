package com.stripe.android.identity.viewmodel

import android.graphics.Bitmap
import android.net.Uri
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.google.common.truth.Truth.assertThat
import com.stripe.android.camera.CameraPreviewImage
import com.stripe.android.core.exception.APIException
import com.stripe.android.core.injection.DUMMY_INJECTOR_KEY
import com.stripe.android.core.model.StripeFile
import com.stripe.android.core.model.StripeFilePurpose
import com.stripe.android.identity.IdentityVerificationSheetContract
import com.stripe.android.identity.camera.IdentityAggregator
import com.stripe.android.identity.ml.AnalyzerInput
import com.stripe.android.identity.ml.BoundingBox
import com.stripe.android.identity.ml.FaceDetectorOutput
import com.stripe.android.identity.ml.IDDetectorOutput
import com.stripe.android.identity.networking.DocumentUploadState
import com.stripe.android.identity.networking.IdentityModelFetcher
import com.stripe.android.identity.networking.IdentityRepository
import com.stripe.android.identity.networking.Resource
import com.stripe.android.identity.networking.UploadedResult
import com.stripe.android.identity.networking.models.DocumentUploadParam
import com.stripe.android.identity.networking.models.VerificationPage
import com.stripe.android.identity.networking.models.VerificationPageStaticContentDocumentCaptureModels
import com.stripe.android.identity.networking.models.VerificationPageStaticContentDocumentCapturePage
import com.stripe.android.identity.networking.models.VerificationPageStaticContentSelfieCapturePage
import com.stripe.android.identity.networking.models.VerificationPageStaticContentSelfieModels
import com.stripe.android.identity.states.FaceDetectorTransitioner
import com.stripe.android.identity.states.IdentityScanState
import com.stripe.android.identity.utils.IdentityIO
import com.stripe.android.identity.viewmodel.IdentityViewModel.Companion.BACK
import com.stripe.android.identity.viewmodel.IdentityViewModel.Companion.FRONT
import kotlinx.coroutines.runBlocking
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TestRule
import org.junit.runner.RunWith
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.same
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.robolectric.RobolectricTestRunner
import java.io.File

@RunWith(RobolectricTestRunner::class)
internal class IdentityViewModelTest {
    @get:Rule
    var rule: TestRule = InstantTaskExecutorRule()

    private val mockVerificationPage = mock<VerificationPage> {
        on { documentCapture }.thenReturn(DOCUMENT_CAPTURE)
        on { selfieCapture }.thenReturn(SELFIE_CAPTURE)
    }
    private val mockIdentityRepository = mock<IdentityRepository> {
        onBlocking {
            retrieveVerificationPage(any(), any())
        }.thenReturn(mockVerificationPage)
    }
    private val mockIdentityModelFetcher = mock<IdentityModelFetcher> {
        onBlocking {
            fetchIdentityModel(eq(ID_DETECTOR_URL))
        }.thenReturn(ID_DETECTOR_FILE)
        onBlocking {
            fetchIdentityModel(eq(FACE_DETECTOR_URL))
        }.thenReturn(FACE_DETECTOR_FILE)
    }
    private val mockIdentityIO = mock<IdentityIO> {
        on { resizeUriAndCreateFileToUpload(any(), any(), any(), any(), any(), any()) }.thenReturn(
            File(IMAGE_FILE_NAME)
        )

        on { resizeBitmapAndCreateFileToUpload(any(), any(), any(), any(), any()) }.thenReturn(
            File(IMAGE_FILE_NAME)
        )

        on { cropAndPadBitmap(any(), any(), any()) }.thenReturn(
            CROPPED_BITMAP
        )
    }

    val viewModel = IdentityViewModel(
        IdentityVerificationSheetContract.Args(
            verificationSessionId = VERIFICATION_SESSION_ID,
            ephemeralKeySecret = EPHEMERAL_KEY,
            brandLogo = BRAND_LOGO,
            injectorKey = DUMMY_INJECTOR_KEY
        ),
        mockIdentityRepository,
        mockIdentityModelFetcher,
        mockIdentityIO,
        mock(),
        mock()
    )

    private fun mockUploadSuccess() = runBlocking {
        whenever(mockIdentityRepository.uploadImage(any(), any(), any(), any())).thenReturn(
            UPLOADED_STRIPE_FILE
        )
    }

    private fun mockUploadFailure() = runBlocking {
        whenever(mockIdentityRepository.uploadImage(any(), any(), any(), any())).thenThrow(
            UPLOADED_FAILURE_EXCEPTION
        )
    }

    @Test
    fun `resetDocumentUploadedState does reset _documentUploadedState`() {
        viewModel.resetDocumentUploadedState()
        assertThat(viewModel.documentUploadState.value).isEqualTo(DocumentUploadState())
    }

    @Test
    fun `uploadManualResult front resizes file and notifies _documentUploadedState`() {
        testUploadManualSuccessResult(true)
    }

    @Test
    fun `uploadManualResult back resizes file and notifies _documentUploadedState`() {
        testUploadManualSuccessResult(false)
    }

    @Test
    fun `uploadManualResult front failure notifies _documentUploadedState`() {
        testUploadManualFailureResult(true)
    }

    @Test
    fun `uploadManualResult back failure notifies _documentUploadedState`() {
        testUploadManualFailureResult(false)
    }

    @Test
    fun `uploadScanResult front success uploads both files and notifies _documentUploadedState`() {
        testUploadDocumentScanSuccessResult(true)
    }

    @Test
    fun `uploadScanResult back success uploads both files and notifies _documentUploadedState`() {
        testUploadDocumentScanSuccessResult(false)
    }

    @Test
    fun `uploadScanResult uploads all files and notifies _selfieUploadedState`() {
        mockUploadSuccess()
        viewModel.uploadScanResult(
            FINAL_FACE_DETECTOR_RESULT,
            mockVerificationPage,
            IdentityScanState.ScanType.SELFIE
        )

        listOf(
            (FaceDetectorTransitioner.Selfie.FIRST),
            (FaceDetectorTransitioner.Selfie.BEST),
            (FaceDetectorTransitioner.Selfie.LAST)
        ).forEach { selfie ->
            listOf(true, false).forEach { isHighRes ->
                testUploadSelfieScanSuccessResult(selfie, isHighRes)
            }
        }
    }

    @Test
    fun `uploadScanResult upload failure notifies _selfieUploadedState`() {
        mockUploadFailure()
        viewModel.uploadScanResult(
            FINAL_FACE_DETECTOR_RESULT,
            mockVerificationPage,
            IdentityScanState.ScanType.SELFIE
        )

        listOf(
            (viewModel.selfieUploadState.value.firstHighResResult),
            (viewModel.selfieUploadState.value.firstLowResResult),
            (viewModel.selfieUploadState.value.bestHighResResult),
            (viewModel.selfieUploadState.value.bestLowResResult),
            (viewModel.selfieUploadState.value.lastHighResResult),
            (viewModel.selfieUploadState.value.lastLowResResult),
        ).forEach { uploadedResult ->
            assertThat(uploadedResult).isEqualTo(
                Resource.error<UploadedResult>(
                    msg = "Failed to upload file : $IMAGE_FILE_NAME",
                    throwable = UPLOADED_FAILURE_EXCEPTION
                )
            )
        }
    }

    @Test
    fun `retrieveAndBufferVerificationPage retrieves model and notifies _verificationPage`() =
        runBlocking {
            viewModel.retrieveAndBufferVerificationPage()

            verify(mockIdentityRepository).retrieveVerificationPage(
                eq(VERIFICATION_SESSION_ID),
                eq(EPHEMERAL_KEY)
            )

            assertThat(viewModel.verificationPage.value).isEqualTo(
                Resource.success(mockVerificationPage)
            )

            verify(mockIdentityModelFetcher).fetchIdentityModel(
                eq(ID_DETECTOR_URL)
            )

            verify(mockIdentityModelFetcher).fetchIdentityModel(
                eq(FACE_DETECTOR_URL)
            )

            assertThat(viewModel.idDetectorModelFile.value).isEqualTo(
                Resource.success(ID_DETECTOR_FILE)
            )

            assertThat(viewModel.faceDetectorModelFile.value).isEqualTo(
                Resource.success(FACE_DETECTOR_FILE)
            )
        }

    private fun testUploadManualSuccessResult(isFront: Boolean) {
        mockUploadSuccess()

        val mockUri = mock<Uri>()
        viewModel.uploadManualResult(
            mockUri,
            isFront,
            DOCUMENT_CAPTURE,
            DocumentUploadParam.UploadMethod.FILEUPLOAD
        )

        verify(mockIdentityIO).resizeUriAndCreateFileToUpload(
            same(mockUri),
            eq(VERIFICATION_SESSION_ID),
            eq(false),
            eq(if (isFront) FRONT else BACK),
            eq(HIGH_RES_IMAGE_MAX_DIMENSION),
            eq(HIGH_RES_COMPRESSION_QUALITY)
        )

        if (isFront) {
            viewModel.documentUploadState.value.frontHighResResult
        } else {
            viewModel.documentUploadState.value.backHighResResult
        }.let { uploadedResult ->
            assertThat(uploadedResult).isEqualTo(
                Resource.success(
                    UploadedResult(
                        UPLOADED_STRIPE_FILE,
                        null,
                        DocumentUploadParam.UploadMethod.FILEUPLOAD
                    )
                )
            )
        }
    }

    private fun testUploadDocumentScanSuccessResult(isFront: Boolean) {
        mockUploadSuccess()

        viewModel.uploadScanResult(
            FINAL_ID_DETECTOR_RESULT,
            mockVerificationPage,
            if (isFront)
                IdentityScanState.ScanType.ID_FRONT
            else
                IdentityScanState.ScanType.ID_BACK
        )

        // high res upload
        verify(mockIdentityIO).cropAndPadBitmap(
            same(INPUT_BITMAP),
            same(BOUNDING_BOX),
            any()
        )

        verify(mockIdentityIO).resizeBitmapAndCreateFileToUpload(
            same(CROPPED_BITMAP),
            eq(VERIFICATION_SESSION_ID),
            eq(
                if (isFront)
                    "${VERIFICATION_SESSION_ID}_$FRONT.jpeg"
                else
                    "${VERIFICATION_SESSION_ID}_$BACK.jpeg"
            ),
            eq(HIGH_RES_IMAGE_MAX_DIMENSION),
            eq(HIGH_RES_COMPRESSION_QUALITY)
        )
        if (isFront) {
            viewModel.documentUploadState.value.frontHighResResult
        } else {
            viewModel.documentUploadState.value.backHighResResult
        }.let { result ->
            assertThat(result).isEqualTo(
                Resource.success(
                    UploadedResult(
                        UPLOADED_STRIPE_FILE,
                        ALL_SCORES,
                        DocumentUploadParam.UploadMethod.AUTOCAPTURE
                    )
                )
            )
        }

        // low res upload
        verify(mockIdentityIO).resizeBitmapAndCreateFileToUpload(
            same(INPUT_BITMAP),
            eq(VERIFICATION_SESSION_ID),
            eq(
                if (isFront)
                    "${VERIFICATION_SESSION_ID}_${FRONT}_full_frame.jpeg"
                else
                    "${VERIFICATION_SESSION_ID}_${BACK}_full_frame.jpeg"
            ),
            eq(LOW_RES_IMAGE_MAX_DIMENSION),
            eq(LOW_RES_COMPRESSION_QUALITY)
        )

        if (isFront) {
            viewModel.documentUploadState.value.frontLowResResult
        } else {
            viewModel.documentUploadState.value.backLowResResult
        }.let { result ->
            assertThat(result).isEqualTo(
                Resource.success(
                    UploadedResult(
                        UPLOADED_STRIPE_FILE,
                        ALL_SCORES,
                        DocumentUploadParam.UploadMethod.AUTOCAPTURE
                    )
                )
            )
        }
    }

    private fun testUploadSelfieScanSuccessResult(
        selfie: FaceDetectorTransitioner.Selfie,
        isHighRes: Boolean
    ) {
        if (isHighRes) { // high res
            verify(mockIdentityIO).cropAndPadBitmap(
                same(FILTERED_FRAMES[selfie.index].first.cameraPreviewImage.image),
                same(FILTERED_FRAMES[selfie.index].second.boundingBox),
                any()
            )

            verify(mockIdentityIO).resizeBitmapAndCreateFileToUpload(
                same(CROPPED_BITMAP),
                eq(VERIFICATION_SESSION_ID),
                eq(
                    when (selfie) {
                        FaceDetectorTransitioner.Selfie.FIRST -> "${VERIFICATION_SESSION_ID}_face_first_crop_frame.jpeg"
                        FaceDetectorTransitioner.Selfie.BEST -> "${VERIFICATION_SESSION_ID}_face.jpeg"
                        FaceDetectorTransitioner.Selfie.LAST -> "${VERIFICATION_SESSION_ID}_face_last_crop_frame.jpeg"
                    }
                ),
                eq(HIGH_RES_IMAGE_MAX_DIMENSION),
                eq(HIGH_RES_COMPRESSION_QUALITY)
            )
            assertThat(
                when (selfie) {
                    FaceDetectorTransitioner.Selfie.FIRST -> viewModel.selfieUploadState.value.firstHighResResult
                    FaceDetectorTransitioner.Selfie.BEST -> viewModel.selfieUploadState.value.bestHighResResult
                    FaceDetectorTransitioner.Selfie.LAST -> viewModel.selfieUploadState.value.lastHighResResult
                }
            ).isEqualTo(
                Resource.success(
                    UploadedResult(
                        UPLOADED_STRIPE_FILE,
                    )
                )
            )
        } else { // low res
            verify(mockIdentityIO).resizeBitmapAndCreateFileToUpload(
                same(FILTERED_FRAMES[selfie.index].first.cameraPreviewImage.image),
                eq(VERIFICATION_SESSION_ID),
                eq(
                    when (selfie) {
                        FaceDetectorTransitioner.Selfie.FIRST -> "${VERIFICATION_SESSION_ID}_face_first_full_frame.jpeg"
                        FaceDetectorTransitioner.Selfie.BEST -> "${VERIFICATION_SESSION_ID}_face_full_frame.jpeg"
                        FaceDetectorTransitioner.Selfie.LAST -> "${VERIFICATION_SESSION_ID}_face_last_full_frame.jpeg"
                    }
                ),
                eq(LOW_RES_IMAGE_MAX_DIMENSION),
                eq(LOW_RES_COMPRESSION_QUALITY)
            )
            assertThat(
                when (selfie) {
                    FaceDetectorTransitioner.Selfie.FIRST -> viewModel.selfieUploadState.value.firstLowResResult
                    FaceDetectorTransitioner.Selfie.BEST -> viewModel.selfieUploadState.value.bestLowResResult
                    FaceDetectorTransitioner.Selfie.LAST -> viewModel.selfieUploadState.value.lastLowResResult
                }
            ).isEqualTo(
                Resource.success(
                    UploadedResult(
                        UPLOADED_STRIPE_FILE,
                    )
                )
            )
        }
    }

    private fun testUploadManualFailureResult(isFront: Boolean) {
        mockUploadFailure()

        viewModel.uploadManualResult(
            mock(),
            isFront,
            DOCUMENT_CAPTURE,
            DocumentUploadParam.UploadMethod.FILEUPLOAD
        )

        if (isFront) {
            viewModel.documentUploadState.value.frontHighResResult
        } else {
            viewModel.documentUploadState.value.backHighResResult
        }.let { uploadedResult ->
            assertThat(uploadedResult).isEqualTo(
                Resource.error<UploadedResult>(
                    msg = "Failed to upload file : $IMAGE_FILE_NAME",
                    throwable = UPLOADED_FAILURE_EXCEPTION
                )
            )
        }
    }

    private companion object {
        const val VERIFICATION_SESSION_ID = "id_5678"
        const val EPHEMERAL_KEY = "eak_5678"
        val BRAND_LOGO = mock<Uri>()
        const val IMAGE_FILE_NAME = "fileName"
        const val HIGH_RES_IMAGE_MAX_DIMENSION = 512
        const val HIGH_RES_COMPRESSION_QUALITY = 0.9f
        const val LOW_RES_IMAGE_MAX_DIMENSION = 256
        const val LOW_RES_COMPRESSION_QUALITY = 0.4f
        const val NUM_SAMPLES = 8
        const val ID_DETECTOR_URL = "path/to/idDetector"
        const val FACE_DETECTOR_URL = "path/to/faceDetector"
        val DOCUMENT_CAPTURE =
            VerificationPageStaticContentDocumentCapturePage(
                autocaptureTimeout = 0,
                filePurpose = StripeFilePurpose.IdentityPrivate.code,
                highResImageCompressionQuality = HIGH_RES_COMPRESSION_QUALITY,
                highResImageCropPadding = 0f,
                highResImageMaxDimension = HIGH_RES_IMAGE_MAX_DIMENSION,
                lowResImageCompressionQuality = LOW_RES_COMPRESSION_QUALITY,
                lowResImageMaxDimension = LOW_RES_IMAGE_MAX_DIMENSION,
                models = VerificationPageStaticContentDocumentCaptureModels(
                    idDetectorUrl = ID_DETECTOR_URL,
                    idDetectorMinScore = 0.6f
                ),
                requireLiveCapture = false,
                motionBlurMinDuration = 500,
                motionBlurMinIou = 0.95f
            )

        val SELFIE_CAPTURE =
            VerificationPageStaticContentSelfieCapturePage(
                autoCaptureTimeout = 15000,
                filePurpose = StripeFilePurpose.IdentityPrivate.code,
                numSamples = NUM_SAMPLES,
                sampleInterval = 200,
                models = VerificationPageStaticContentSelfieModels(
                    faceDetectorUrl = FACE_DETECTOR_URL,
                    faceDetectorMinScore = 0.8f,
                    faceDetectorIou = 0.5f
                ),
                maxCenteredThresholdX = 0.2f,
                maxCenteredThresholdY = 0.2f,
                minEdgeThreshold = 0.05f,
                minCoverageThreshold = 0.07f,
                maxCoverageThreshold = 0.8f,
                lowResImageMaxDimension = LOW_RES_IMAGE_MAX_DIMENSION,
                lowResImageCompressionQuality = LOW_RES_COMPRESSION_QUALITY,
                highResImageMaxDimension = HIGH_RES_IMAGE_MAX_DIMENSION,
                highResImageCompressionQuality = HIGH_RES_COMPRESSION_QUALITY,
                highResImageCropPadding = 0.5f,
                consentText = "consent"
            )

        val UPLOADED_STRIPE_FILE = StripeFile()
        val UPLOADED_FAILURE_EXCEPTION = APIException()

        val INPUT_BITMAP = mock<Bitmap>()
        val BOUNDING_BOX = mock<BoundingBox>()
        val ALL_SCORES = listOf(1f, 2f, 3f)
        val FINAL_ID_DETECTOR_RESULT = IdentityAggregator.FinalResult(
            frame = AnalyzerInput(
                CameraPreviewImage(
                    INPUT_BITMAP,
                    mock()
                ),
                mock()
            ),
            result = IDDetectorOutput(
                boundingBox = BOUNDING_BOX,
                category = mock(),
                resultScore = 0.8f,
                allScores = ALL_SCORES
            ),
            identityState = mock<IdentityScanState.Finished>()
        )

        val FILTERED_FRAMES = listOf(
            AnalyzerInput(
                cameraPreviewImage = CameraPreviewImage(
                    image = mock(),
                    viewBounds = mock()
                ),
                viewFinderBounds = mock()
            ) to FaceDetectorOutput(
                boundingBox = mock(),
                resultScore = 0.81f
            ), // first
            AnalyzerInput(
                cameraPreviewImage = CameraPreviewImage(
                    image = mock(),
                    viewBounds = mock()
                ),
                viewFinderBounds = mock()
            ) to FaceDetectorOutput(
                boundingBox = mock(),
                resultScore = 0.9f
            ), // best
            AnalyzerInput(
                cameraPreviewImage = CameraPreviewImage(
                    image = mock(),
                    viewBounds = mock()
                ),
                viewFinderBounds = mock()
            ) to FaceDetectorOutput(
                boundingBox = mock(),
                resultScore = 0.82f
            ), // last
        )
        val FINAL_FACE_DETECTOR_RESULT = IdentityAggregator.FinalResult(
            frame = AnalyzerInput(
                CameraPreviewImage(
                    INPUT_BITMAP,
                    mock()
                ),
                mock()
            ),
            result = FaceDetectorOutput(
                boundingBox = BOUNDING_BOX,
                resultScore = 0.8f
            ),
            identityState = IdentityScanState.Finished(
                type = IdentityScanState.ScanType.SELFIE,
                transitioner = mock<FaceDetectorTransitioner> {
                    on { filteredFrames }.thenReturn(FILTERED_FRAMES)
                }
            )
        )

        val ID_DETECTOR_FILE = mock<File>()
        val FACE_DETECTOR_FILE = mock<File>()
        val CROPPED_BITMAP = mock<Bitmap>()
    }
}
