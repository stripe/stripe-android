package com.stripe.android.identity.viewmodel

import android.graphics.Bitmap
import android.net.Uri
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavController
import androidx.navigation.NavOptionsBuilder
import androidx.test.core.app.ApplicationProvider
import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import com.stripe.android.camera.CameraPermissionEnsureable
import com.stripe.android.camera.CameraPreviewImage
import com.stripe.android.core.exception.APIException
import com.stripe.android.core.injection.DUMMY_INJECTOR_KEY
import com.stripe.android.core.model.StripeFile
import com.stripe.android.core.model.StripeFilePurpose
import com.stripe.android.identity.CORRECT_WITH_SUBMITTED_SUCCESS_VERIFICATION_PAGE_DATA
import com.stripe.android.identity.IdentityVerificationSheetContract
import com.stripe.android.identity.SUBMITTED_AND_CLOSED_VERIFICATION_PAGE_DATA
import com.stripe.android.identity.SUBMITTED_AND_NOT_CLOSED_NO_MISSING_VERIFICATION_PAGE_DATA
import com.stripe.android.identity.SUBMITTED_AND_NOT_CLOSED_VERIFICATION_PAGE_DATA
import com.stripe.android.identity.SUCCESS_VERIFICATION_PAGE_REQUIRE_LIVE_CAPTURE
import com.stripe.android.identity.SUCCESS_VERIFICATION_PAGE_REQUIRE_SELFIE_LIVE_CAPTURE
import com.stripe.android.identity.VERIFICATION_PAGE_DATA_HAS_ERROR
import com.stripe.android.identity.VERIFICATION_PAGE_DATA_MISSING_BACK
import com.stripe.android.identity.VERIFICATION_PAGE_DATA_MISSING_CONSENT
import com.stripe.android.identity.VERIFICATION_PAGE_DATA_MISSING_PHONE_OTP
import com.stripe.android.identity.VERIFICATION_PAGE_DATA_MISSING_SELFIE
import com.stripe.android.identity.analytics.IdentityAnalyticsRequestFactory
import com.stripe.android.identity.analytics.IdentityAnalyticsRequestFactory.Companion.SCREEN_NAME_CONSENT
import com.stripe.android.identity.analytics.IdentityAnalyticsRequestFactory.Companion.SCREEN_NAME_DOC_WARMUP
import com.stripe.android.identity.analytics.ScreenTracker
import com.stripe.android.identity.camera.IdentityAggregator
import com.stripe.android.identity.ml.AnalyzerInput
import com.stripe.android.identity.ml.BoundingBox
import com.stripe.android.identity.ml.Category
import com.stripe.android.identity.ml.FaceDetectorOutput
import com.stripe.android.identity.ml.IDDetectorOutput
import com.stripe.android.identity.navigation.ConfirmationDestination
import com.stripe.android.identity.navigation.ConsentDestination
import com.stripe.android.identity.navigation.DocumentScanDestination
import com.stripe.android.identity.navigation.ErrorDestination
import com.stripe.android.identity.navigation.IdentityTopLevelDestination
import com.stripe.android.identity.navigation.SelfieWarmupDestination
import com.stripe.android.identity.navigation.SelfieWarmupDestination.SELFIE_WARMUP
import com.stripe.android.identity.networking.IdentityModelFetcher
import com.stripe.android.identity.networking.IdentityRepository
import com.stripe.android.identity.networking.Resource
import com.stripe.android.identity.networking.SingleSideDocumentUploadState
import com.stripe.android.identity.networking.Status
import com.stripe.android.identity.networking.UploadedResult
import com.stripe.android.identity.networking.models.CollectedDataParam
import com.stripe.android.identity.networking.models.DocumentUploadParam
import com.stripe.android.identity.networking.models.Requirement
import com.stripe.android.identity.networking.models.VerificationPage
import com.stripe.android.identity.networking.models.VerificationPage.Companion.IDPROD_3D_FACE_CAPTURE_MOBILE_EXPERIMENT
import com.stripe.android.identity.networking.models.VerificationPageData
import com.stripe.android.identity.networking.models.VerificationPageRequirements
import com.stripe.android.identity.networking.models.VerificationPageStaticContentDocumentCaptureModels
import com.stripe.android.identity.networking.models.VerificationPageStaticContentDocumentCapturePage
import com.stripe.android.identity.networking.models.VerificationPageStaticContentExperiment
import com.stripe.android.identity.networking.models.VerificationPageStaticContentSelfieCapturePage
import com.stripe.android.identity.networking.models.VerificationPageStaticContentSelfieModels
import com.stripe.android.identity.states.FaceDetectorTransitioner
import com.stripe.android.identity.states.IdentityScanState
import com.stripe.android.identity.utils.FakeIdentityIO
import com.stripe.android.identity.utils.FakeIdentityIO.Operation
import com.stripe.android.identity.utils.IdentityIO
import com.stripe.android.identity.utils.TestModeImage
import com.stripe.android.identity.viewmodel.IdentityViewModel.Companion.BACK
import com.stripe.android.identity.viewmodel.IdentityViewModel.Companion.FRONT
import com.stripe.android.mlcore.base.InterpreterInitializer
import com.stripe.android.testing.CoroutineTestRule
import com.stripe.android.testing.ViewModelStoreTestRule
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TestRule
import org.junit.runner.RunWith
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.argWhere
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.same
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.robolectric.RobolectricTestRunner
import java.io.File
import java.io.IOException
import java.util.concurrent.CountDownLatch
import kotlin.coroutines.CoroutineContext
import kotlin.test.assertFailsWith

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
internal class IdentityViewModelTest {
    @get:Rule
    var rule: TestRule = InstantTaskExecutorRule()

    @get:Rule
    val viewModelStoreRule = ViewModelStoreTestRule()

    @get:Rule
    val coroutineTestRule = CoroutineTestRule(UnconfinedTestDispatcher())

    private val mockVerificationPage = mock<VerificationPage> {
        on { documentCapture }.thenReturn(DOCUMENT_CAPTURE)
        on { selfieCapture }.thenReturn(SELFIE_CAPTURE)
        on { requirements }.thenReturn(REQUIREMENTS_NO_MISSING)
        on { livemode }.thenReturn(true)
        on { experiments }.thenReturn(emptyList())
    }

    private val mockIdentityRepository = mock<IdentityRepository> {
        on {
            retrieveVerificationPage(any(), any())
        }.thenReturn(mockVerificationPage)
    }
    private val mockIdentityModelFetcher = mock<IdentityModelFetcher> {
        on {
            fetchIdentityModel(eq(ID_DETECTOR_URL))
        }.thenReturn(ID_DETECTOR_FILE)
        on {
            fetchIdentityModel(eq(FACE_DETECTOR_URL))
        }.thenReturn(FACE_DETECTOR_FILE)
    }
    private val mockIdentityIO = mock<IdentityIO> {
        on { resizeUriAndCreateFileToUpload(any(), any(), any(), any(), any(), any()) }.thenReturn(
            IMAGE_FILE
        )

        on { resizeBitmapAndCreateFileToUpload(any(), any(), any(), any(), any()) }.thenReturn(
            IMAGE_FILE
        )

        on { createTestModeFileToUpload(TestModeImage.DOCUMENT_FRONT) }.thenReturn(
            TEST_MODE_DOCUMENT_FRONT_FILE
        )
        on { createTestModeFileToUpload(TestModeImage.DOCUMENT_BACK) }.thenReturn(
            TEST_MODE_DOCUMENT_BACK_FILE
        )
        on { createTestModeFileToUpload(TestModeImage.SELFIE) }.thenReturn(
            TEST_MODE_SELFIE_FILE
        )

        on { cropAndPadBitmap(any(), any(), any()) }.thenReturn(
            CROPPED_BITMAP
        )
    }
    private val mockSavedStateHandle = mock<SavedStateHandle> {
        on { getLiveData<Resource<VerificationPage>>(any(), any()) } doReturn MutableLiveData()
    }

    private val mockIdentityAnalyticsRequestFactory = mock<IdentityAnalyticsRequestFactory>()

    private val mockScreenTracker = mock<ScreenTracker>()
    private val mockController = mock<NavController>()
    private val mockCollectedDataParam = mock<CollectedDataParam>()
    private val mockOnMissingBack = mock<() -> Unit>()
    private val mockOnMissingPhoneOtp = mock<() -> Unit>()
    private val mockOnReadyToSubmit = mock<() -> Unit>()
    private val mockTfLiteInitializer = mock<InterpreterInitializer>()

    private val viewModel = createViewModel()

    private fun createViewModel(
        identityIO: IdentityIO = mockIdentityIO,
        repository: IdentityRepository = mockIdentityRepository,
        workContext: CoroutineContext = UnconfinedTestDispatcher(),
        savedStateHandle: SavedStateHandle = mockSavedStateHandle,
    ) = IdentityViewModel(
        ApplicationProvider.getApplicationContext(),
        IdentityVerificationSheetContract.Args(
            verificationSessionId = VERIFICATION_SESSION_ID,
            ephemeralKeySecret = EPHEMERAL_KEY,
            brandLogo = BRAND_LOGO,
            brandColor = null,
            biometricConsent = null,
            primaryButtonStyle = null,
            secondaryButtonStyle = null,
            injectorKey = DUMMY_INJECTOR_KEY,
            presentTime = 0
        ),
        repository,
        mockIdentityModelFetcher,
        identityIO,
        mockIdentityAnalyticsRequestFactory,
        mockScreenTracker,
        mock(),
        mockTfLiteInitializer,
        savedStateHandle,
        mock(),
        workContext,
        mock()
    ).also { viewModelStoreRule.track(it) }

    private fun mockUploadSuccess() = runBlocking {
        whenever(mockIdentityRepository.uploadImage(any(), any(), any(), any(), any())).thenReturn(
            UPLOADED_STRIPE_FILE
        )
    }

    private fun mockUploadFailure() = runBlocking {
        whenever(mockIdentityRepository.uploadImage(any(), any(), any(), any(), any())).thenThrow(
            UPLOADED_FAILURE_EXCEPTION
        )
    }

    @Test
    fun `resetDocumentUploadedState does reset _documentUploadedState`() {
        assertThat(viewModel.documentFrontUploadedState.value).isEqualTo(
            SingleSideDocumentUploadState()
        )
        assertThat(viewModel.documentBackUploadedState.value).isEqualTo(
            SingleSideDocumentUploadState()
        )
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
    fun `uploadManualResult bitmap front resizes file and notifies _documentUploadedState`() {
        testUploadManualBitmapSuccessResult(true)
    }

    @Test
    fun `uploadManualResult bitmap back resizes file and notifies _documentUploadedState`() {
        testUploadManualBitmapSuccessResult(false)
    }

    @Test
    fun `uploadManualResult test mode front uploads placeholder image`() = runBlocking<Unit> {
        mockUploadSuccess()

        viewModel.uploadManualResult(
            uri = mock(),
            isFront = true,
            isLiveMode = false,
            docCapturePage = DOCUMENT_CAPTURE,
            uploadMethod = DocumentUploadParam.UploadMethod.FILEUPLOAD,
            scanType = IdentityScanState.ScanType.DOC_FRONT
        )

        verify(mockIdentityIO).createTestModeFileToUpload(TestModeImage.DOCUMENT_FRONT)
        verify(mockIdentityRepository).uploadImage(
            eq(VERIFICATION_SESSION_ID),
            eq(EPHEMERAL_KEY),
            same(TEST_MODE_DOCUMENT_FRONT_FILE),
            any(),
            any()
        )
    }

    @Test
    fun `uploadManualResult test mode back uploads placeholder image`() = runBlocking<Unit> {
        mockUploadSuccess()

        viewModel.uploadManualResult(
            uri = mock(),
            isFront = false,
            isLiveMode = false,
            docCapturePage = DOCUMENT_CAPTURE,
            uploadMethod = DocumentUploadParam.UploadMethod.FILEUPLOAD,
            scanType = IdentityScanState.ScanType.DOC_BACK
        )

        verify(mockIdentityIO).createTestModeFileToUpload(TestModeImage.DOCUMENT_BACK)
        verify(mockIdentityRepository).uploadImage(
            eq(VERIFICATION_SESSION_ID),
            eq(EPHEMERAL_KEY),
            same(TEST_MODE_DOCUMENT_BACK_FILE),
            any(),
            any()
        )
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
        testUploadDocumentScanSuccessResult(isFront = true)
    }

    @Test
    fun `uploadScanResult back success uploads both files and notifies _documentUploadedState`() {
        testUploadDocumentScanSuccessResult(isFront = false)
    }

    @Test
    fun `uploadScanResult does not upload side frames when 3D is disabled`() = runBlocking {
        mockUploadSuccess()
        viewModel.uploadScanResult(
            FINAL_FACE_DETECTOR_RESULT,
            mockVerificationPage
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
        verify(mockIdentityAnalyticsRequestFactory, times(6)).imageUpload(
            anyOrNull(),
            anyOrNull(),
            anyOrNull(),
            anyOrNull(),
            anyOrNull(),
            anyOrNull()
        )
        assertThat(viewModel.selfieUploadState.value.leftFullFrameResult.status)
            .isEqualTo(Status.IDLE)
        assertThat(viewModel.selfieUploadState.value.rightFullFrameResult.status)
            .isEqualTo(Status.IDLE)
    }

    @Test
    fun `uploadScanResult uploads side full frames when 3D experiment is enabled`() = runBlocking {
        mockUploadSuccess()
        val verificationPage3D = mock<VerificationPage> {
            on { documentCapture }.thenReturn(DOCUMENT_CAPTURE)
            on { selfieCapture }.thenReturn(SELFIE_CAPTURE)
            on { requirements }.thenReturn(REQUIREMENTS_NO_MISSING)
            on { livemode }.thenReturn(true)
            on { experiments }.thenReturn(
                listOf(
                    VerificationPageStaticContentExperiment(
                        experimentName = IDPROD_3D_FACE_CAPTURE_MOBILE_EXPERIMENT,
                        eventName = "screen_presented",
                        eventMetadata = mapOf("screen_name" to "selfie")
                    )
                )
            )
        }

        viewModel.uploadScanResult(
            FINAL_FACE_DETECTOR_RESULT,
            verificationPage3D
        )

        listOf(
            FaceDetectorTransitioner.Selfie.FIRST,
            FaceDetectorTransitioner.Selfie.BEST,
            FaceDetectorTransitioner.Selfie.LAST
        ).forEach { selfie ->
            listOf(true, false).forEach { isHighRes ->
                testUploadSelfieScanSuccessResult(selfie, isHighRes)
            }
        }
        listOf(
            FaceDetectorTransitioner.Selfie.RIGHT,
            FaceDetectorTransitioner.Selfie.LEFT
        ).forEach { selfie ->
            testUploadSelfieScanSuccessResult(selfie, isHighRes = false)
        }
        verify(mockIdentityAnalyticsRequestFactory, times(8)).imageUpload(
            anyOrNull(),
            anyOrNull(),
            anyOrNull(),
            anyOrNull(),
            anyOrNull(),
            anyOrNull()
        )
    }

    @Test
    fun `uploadScanResult test mode selfie uploads placeholder image`() = runBlocking<Unit> {
        mockUploadSuccess()
        val testModeVerificationPage = mock<VerificationPage> {
            on { selfieCapture }.thenReturn(SELFIE_CAPTURE)
            on { livemode }.thenReturn(false)
            on { experiments }.thenReturn(emptyList())
        }

        viewModel.uploadScanResult(
            FINAL_FACE_DETECTOR_RESULT,
            testModeVerificationPage
        )

        verify(mockIdentityIO, times(6)).createTestModeFileToUpload(TestModeImage.SELFIE)
        verify(mockIdentityRepository, times(6)).uploadImage(
            eq(VERIFICATION_SESSION_ID),
            eq(EPHEMERAL_KEY),
            same(TEST_MODE_SELFIE_FILE),
            any(),
            any()
        )
    }

    @Test
    fun `uploadScanResult upload failure notifies _selfieUploadedState`() = runBlocking {
        mockUploadFailure()
        viewModel.uploadScanResult(
            FINAL_FACE_DETECTOR_RESULT,
            mockVerificationPage
        )
        verify(mockIdentityAnalyticsRequestFactory, times(0)).imageUpload(
            anyOrNull(),
            anyOrNull(),
            anyOrNull(),
            anyOrNull(),
            anyOrNull(),
            anyOrNull()
        )

        listOf(
            (viewModel.selfieUploadState.value.firstHighResResult),
            (viewModel.selfieUploadState.value.firstLowResResult),
            (viewModel.selfieUploadState.value.bestHighResResult),
            (viewModel.selfieUploadState.value.bestLowResResult),
            (viewModel.selfieUploadState.value.lastHighResResult),
            (viewModel.selfieUploadState.value.lastLowResResult)
        ).forEach { uploadedResult ->
            assertThat(uploadedResult).isEqualTo(
                Resource.error<UploadedResult>(
                    msg = "Failed to upload file : $IMAGE_FILE_NAME",
                    throwable = UPLOADED_FAILURE_EXCEPTION
                )
            )
        }
        assertThat(viewModel.selfieUploadState.value.leftFullFrameResult.status)
            .isEqualTo(Status.IDLE)
        assertThat(viewModel.selfieUploadState.value.rightFullFrameResult.status)
            .isEqualTo(Status.IDLE)
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

            assertThat(viewModel.missingRequirements.value).isEqualTo(
                REQUIREMENTS_NO_MISSING.missing.toSet()
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

    @Test
    fun verifyAnalyticsState() {
        // initialized with all null
        assertThat(viewModel.analyticsState.value.scanType).isNull()
        assertThat(viewModel.analyticsState.value.requireSelfie).isNull()
        assertThat(viewModel.analyticsState.value.docFrontUploadType).isNull()

        viewModel.updateAnalyticsState { oldState ->
            oldState.copy(
                scanType = IdentityScanState.ScanType.DOC_FRONT
            )
        }

        assertThat(viewModel.analyticsState.value.scanType).isEqualTo(
            IdentityScanState.ScanType.DOC_FRONT
        )
        assertThat(viewModel.analyticsState.value.requireSelfie).isNull()
        assertThat(viewModel.analyticsState.value.docFrontUploadType).isNull()

        viewModel.updateAnalyticsState { oldState ->
            oldState.copy(
                requireSelfie = false
            )
        }

        assertThat(viewModel.analyticsState.value.scanType).isEqualTo(
            IdentityScanState.ScanType.DOC_FRONT
        )
        assertThat(viewModel.analyticsState.value.requireSelfie).isEqualTo(
            false
        )
        assertThat(viewModel.analyticsState.value.docFrontUploadType).isNull()

        viewModel.updateAnalyticsState { oldState ->
            oldState.copy(
                docFrontUploadType = DocumentUploadParam.UploadMethod.MANUALCAPTURE
            )
        }

        assertThat(viewModel.analyticsState.value.scanType).isEqualTo(
            IdentityScanState.ScanType.DOC_FRONT
        )
        assertThat(viewModel.analyticsState.value.requireSelfie).isEqualTo(
            false
        )
        assertThat(viewModel.analyticsState.value.docFrontUploadType).isEqualTo(
            DocumentUploadParam.UploadMethod.MANUALCAPTURE
        )
    }

    @Test
    fun `trackScreenPresented includes previous screen name`() {
        val scanTypeCaptor = argumentCaptor<IdentityScanState.ScanType?>()
        val screenNameCaptor = argumentCaptor<String>()
        val previousScreenNameCaptor = argumentCaptor<String?>()

        viewModel.trackScreenPresented(
            scanType = IdentityScanState.ScanType.DOC_FRONT,
            screenName = SCREEN_NAME_CONSENT
        )
        viewModel.trackScreenPresented(
            scanType = IdentityScanState.ScanType.DOC_BACK,
            screenName = SCREEN_NAME_DOC_WARMUP
        )

        verify(mockIdentityAnalyticsRequestFactory, times(2)).screenPresented(
            scanTypeCaptor.capture(),
            screenNameCaptor.capture(),
            previousScreenNameCaptor.capture()
        )
        assertThat(scanTypeCaptor.allValues).containsExactly(
            IdentityScanState.ScanType.DOC_FRONT,
            IdentityScanState.ScanType.DOC_BACK
        ).inOrder()
        assertThat(screenNameCaptor.allValues).containsExactly(
            SCREEN_NAME_CONSENT,
            SCREEN_NAME_DOC_WARMUP
        ).inOrder()
        assertThat(previousScreenNameCaptor.allValues).containsExactly(
            null,
            SCREEN_NAME_CONSENT
        ).inOrder()
        assertThat(viewModel.analyticsLastScreenName).isEqualTo(SCREEN_NAME_DOC_WARMUP)
    }

    @Test
    fun `postVerificationPageDataAndMaybeNavigate - postFailure`() = runBlocking {
        viewModel._verificationPage.postValue(
            Resource.success(
                SUCCESS_VERIFICATION_PAGE_REQUIRE_SELFIE_LIVE_CAPTURE
            )
        )

        val throwable = java.lang.RuntimeException()
        whenever(
            mockIdentityRepository.postVerificationPageData(
                any(),
                any(),
                any(),
                any()
            )
        ).thenThrow(throwable)

        viewModel.postVerificationPageDataAndMaybeNavigate(
            mockController,
            mockCollectedDataParam,
            ConsentDestination.ROUTE.route,
            mockOnMissingBack,
            mockOnMissingPhoneOtp,
            mockOnReadyToSubmit
        )

        verify(mockIdentityRepository).postVerificationPageData(
            eq(VERIFICATION_SESSION_ID),
            eq(EPHEMERAL_KEY),
            same(mockCollectedDataParam),
            any()
        )
        assertThat(viewModel.errorCause.value).isEqualTo(throwable)
        verify(mockScreenTracker).screenTransitionStart(eq(SCREEN_NAME_CONSENT), any())
        verify(mockController).navigate(
            argWhere {
                it.startsWith(ErrorDestination.ERROR)
            },
            any<NavOptionsBuilder.() -> Unit>()
        )
    }

    @Test
    fun `postVerificationPageDataAndMaybeNavigate - missingConsent`() {
        testPostVerificationPageDataAndMaybeNavigate(
            VERIFICATION_PAGE_DATA_MISSING_CONSENT,
            ConsentDestination
        )
    }

    @Test
    fun `postVerificationPageDataAndMaybeNavigate - missSelfie`() {
        testPostVerificationPageDataAndMaybeNavigate(
            VERIFICATION_PAGE_DATA_MISSING_SELFIE,
            SelfieWarmupDestination
        )
    }

    @Test
    fun `postVerificationPageDataAndMaybeNavigate - missingBack`() {
        testPostVerificationPageDataAndMaybeNavigateWithCallback(
            VERIFICATION_PAGE_DATA_MISSING_BACK
        ) {
            verify(mockOnMissingBack).invoke()
        }
    }

    @Test
    fun `postVerificationPageDataAndMaybeNavigate - missingPhoneOtp`() {
        testPostVerificationPageDataAndMaybeNavigateWithCallback(
            VERIFICATION_PAGE_DATA_MISSING_PHONE_OTP
        ) {
            verify(mockOnMissingPhoneOtp).invoke()
        }
    }

    @Test
    fun `postVerificationPageDataAndMaybeNavigate - no missing`() {
        testPostVerificationPageDataAndMaybeNavigateWithCallback(
            CORRECT_WITH_SUBMITTED_SUCCESS_VERIFICATION_PAGE_DATA
        ) {
            verify(mockOnReadyToSubmit).invoke()
        }
    }

    @Test
    fun `forceConfirm unSupportedRequirement - navigate to error`() =
        runBlocking {
            // sending a requirement not in REQUIREMENTS_SUPPORTS_FORCE_CONFIRM
            viewModel.postVerificationPageDataForForceConfirm(
                requirementToForceConfirm = Requirement.FACE,
                navController = mockController
            )

            verify(mockController).navigate(
                argWhere {
                    it.startsWith(ErrorDestination.ERROR)
                },
                any<NavOptionsBuilder.() -> Unit>()
            )
        }

    @Test
    fun `forceConfirm front - missingBack - navigate to back`() =
        testForceConfirm(VERIFICATION_PAGE_DATA_MISSING_BACK) { failedCollectedDataParam ->
            // fulfilling front, should post with force confirm front
            viewModel.postVerificationPageDataForForceConfirm(
                requirementToForceConfirm = Requirement.IDDOCUMENTFRONT,
                navController = mockController
            )

            // verify postVerificationPageData with force confirming front
            verify(mockIdentityRepository).postVerificationPageData(
                eq(VERIFICATION_SESSION_ID),
                eq(EPHEMERAL_KEY),
                eq(
                    CollectedDataParam(
                        idDocumentFront = failedCollectedDataParam.idDocumentFront?.copy(
                            forceConfirm = true
                        )
                    )
                ),
                any()
            )

            verify(mockController).navigate(
                eq(DocumentScanDestination.routeWithArgs),
                any<NavOptionsBuilder.() -> Unit>()
            )
        }

    @Test
    fun `forceConfirm back - missingSelfie - navigate to selfie warmup`() =
        testForceConfirm(VERIFICATION_PAGE_DATA_MISSING_SELFIE) { failedCollectedDataParam ->
            // fulfilling back, should post with force confirm bcak
            viewModel.postVerificationPageDataForForceConfirm(
                requirementToForceConfirm = Requirement.IDDOCUMENTBACK,
                navController = mockController
            )

            // verify postVerificationPageData with force confirming back
            verify(mockIdentityRepository).postVerificationPageData(
                eq(VERIFICATION_SESSION_ID),
                eq(EPHEMERAL_KEY),
                eq(
                    CollectedDataParam(
                        idDocumentBack = failedCollectedDataParam.idDocumentBack?.copy(
                            forceConfirm = true
                        )
                    )
                ),
                any()
            )

            verify(mockController).navigate(
                eq(SELFIE_WARMUP),
                any<NavOptionsBuilder.() -> Unit>()
            )
        }

    @Test
    fun `forceConfirm back - noMissing - submit`() =
        testForceConfirm(CORRECT_WITH_SUBMITTED_SUCCESS_VERIFICATION_PAGE_DATA) { failedCollectedDataParam ->

            whenever(
                mockIdentityRepository.postVerificationPageSubmit(
                    any(),
                    any()
                )
            ).thenReturn(CORRECT_WITH_SUBMITTED_SUCCESS_VERIFICATION_PAGE_DATA)

            // fulfilling back, should post with force confirm bcak
            viewModel.postVerificationPageDataForForceConfirm(
                requirementToForceConfirm = Requirement.IDDOCUMENTBACK,
                navController = mockController
            )

            // verify postVerificationPageData with force confirming back
            verify(mockIdentityRepository).postVerificationPageData(
                eq(VERIFICATION_SESSION_ID),
                eq(EPHEMERAL_KEY),
                eq(
                    CollectedDataParam(
                        idDocumentBack = failedCollectedDataParam.idDocumentBack?.copy(
                            forceConfirm = true
                        )
                    )
                ),
                any()
            )

            // no missing, submit
            verify(mockIdentityRepository).postVerificationPageSubmit(
                eq(VERIFICATION_SESSION_ID),
                eq(EPHEMERAL_KEY),
            )
        }

    @Test
    fun `navigateToSelfieOrSubmit - requireSelfie`() = runBlocking {
        viewModel._verificationPage.postValue(
            Resource.success(
                SUCCESS_VERIFICATION_PAGE_REQUIRE_SELFIE_LIVE_CAPTURE
            )
        )
        viewModel.navigateToSelfieOrSubmit(
            mockController,
            ConsentDestination.ROUTE.route
        )
        verify(mockScreenTracker).screenTransitionStart(eq(SCREEN_NAME_CONSENT), any())
        verify(mockController).navigate(
            eq(SelfieWarmupDestination.routeWithArgs),
            any<NavOptionsBuilder.() -> Unit>()
        )
    }

    @Test
    fun `navigateToSelfieOrSubmit - not requireSelfie - hasError`() {
        runBlocking {
            whenever(
                mockIdentityRepository.postVerificationPageSubmit(
                    any(),
                    any()
                )
            ).thenReturn(VERIFICATION_PAGE_DATA_HAS_ERROR)

            viewModel._verificationPage.postValue(
                Resource.success(
                    SUCCESS_VERIFICATION_PAGE_REQUIRE_LIVE_CAPTURE
                )
            )

            viewModel.navigateToSelfieOrSubmit(
                mockController,
                ConsentDestination.ROUTE.route
            )

            verify(mockIdentityRepository).postVerificationPageSubmit(
                eq(VERIFICATION_SESSION_ID),
                eq(EPHEMERAL_KEY)
            )

            assertThat(viewModel.verificationPageSubmit.value).isEqualTo(Resource.success(Resource.DUMMY_RESOURCE))
            verify(mockScreenTracker).screenTransitionStart(eq(SCREEN_NAME_CONSENT), any())

            verify(mockController).navigate(
                argWhere {
                    it.startsWith(ErrorDestination.ERROR)
                },
                any<NavOptionsBuilder.() -> Unit>()
            )
        }
    }

    @Test
    fun `navigateToSelfieOrSubmit - not requireSelfie - noError`() {
        runBlocking {
            whenever(
                mockIdentityRepository.postVerificationPageSubmit(
                    any(),
                    any()
                )
            ).thenReturn(CORRECT_WITH_SUBMITTED_SUCCESS_VERIFICATION_PAGE_DATA)

            viewModel._verificationPage.postValue(
                Resource.success(
                    SUCCESS_VERIFICATION_PAGE_REQUIRE_LIVE_CAPTURE
                )
            )

            viewModel.navigateToSelfieOrSubmit(
                mockController,
                ConsentDestination.ROUTE.route
            )

            verify(mockIdentityRepository).postVerificationPageSubmit(
                eq(VERIFICATION_SESSION_ID),
                eq(EPHEMERAL_KEY)
            )

            assertThat(viewModel.verificationPageSubmit.value).isEqualTo(Resource.success(Resource.DUMMY_RESOURCE))
            verify(mockScreenTracker).screenTransitionStart(eq(SCREEN_NAME_CONSENT), any())

            verify(mockController).navigate(
                eq(ConfirmationDestination.routeWithArgs),
                any<NavOptionsBuilder.() -> Unit>()
            )
        }
    }

    @Test
    fun `checkPermissionAndNavigate - camera ready starts transition and navigates to document scan`() {
        val mockCameraPermissionEnsureable = mock<CameraPermissionEnsureable>()
        val onCameraReadyCaptor = argumentCaptor<() -> Unit>()

        viewModel.checkPermissionAndNavigate(
            navController = mockController,
            cameraPermissionEnsureable = mockCameraPermissionEnsureable,
            screenName = SCREEN_NAME_DOC_WARMUP
        )

        verify(mockScreenTracker).screenTransitionStart(eq(SCREEN_NAME_DOC_WARMUP), any())
        verify(mockCameraPermissionEnsureable).ensureCameraPermission(
            onCameraReadyCaptor.capture(),
            any()
        )

        onCameraReadyCaptor.firstValue.invoke()

        verify(mockController).navigate(
            eq(DocumentScanDestination.routeWithArgs),
            any<NavOptionsBuilder.() -> Unit>()
        )
    }

    @Test
    fun `checkPermissionAndNavigate - user denied starts transition and navigates to permission denied`() {
        val mockCameraPermissionEnsureable = mock<CameraPermissionEnsureable>()
        val onUserDeniedCaptor = argumentCaptor<() -> Unit>()

        viewModel.checkPermissionAndNavigate(
            navController = mockController,
            cameraPermissionEnsureable = mockCameraPermissionEnsureable,
            screenName = SCREEN_NAME_DOC_WARMUP
        )

        verify(mockScreenTracker).screenTransitionStart(eq(SCREEN_NAME_DOC_WARMUP), any())
        verify(mockCameraPermissionEnsureable).ensureCameraPermission(
            any(),
            onUserDeniedCaptor.capture()
        )

        onUserDeniedCaptor.firstValue.invoke()

        verify(mockController).navigate(
            eq(com.stripe.android.identity.navigation.CameraPermissionDeniedDestination.routeWithArgs),
            any<NavOptionsBuilder.() -> Unit>()
        )
    }

    @Test
    fun `submitAndNavigate - submitted and closed - navigate to success`() {
        runBlocking {
            whenever(
                mockIdentityRepository.postVerificationPageSubmit(
                    any(),
                    any()
                )
            ).thenReturn(SUBMITTED_AND_CLOSED_VERIFICATION_PAGE_DATA)

            viewModel._verificationPage.postValue(
                Resource.success(
                    SUCCESS_VERIFICATION_PAGE_REQUIRE_LIVE_CAPTURE
                )
            )

            viewModel.submitAndNavigate(
                mockController,
                ConsentDestination.ROUTE.route
            )

            verify(mockIdentityRepository).postVerificationPageSubmit(
                eq(VERIFICATION_SESSION_ID),
                eq(EPHEMERAL_KEY)
            )

            assertThat(viewModel.verificationPageSubmit.value).isEqualTo(Resource.success(Resource.DUMMY_RESOURCE))

            verify(mockController).navigate(
                eq(ConfirmationDestination.routeWithArgs),
                any<NavOptionsBuilder.() -> Unit>()
            )
        }
    }

    @Test
    fun `submitAndNavigate - submitted but not closed - fallback to consent`() {
        runBlocking {
            whenever(
                mockIdentityRepository.postVerificationPageSubmit(
                    any(),
                    any()
                )
            ).thenReturn(SUBMITTED_AND_NOT_CLOSED_VERIFICATION_PAGE_DATA)

            viewModel._verificationPage.postValue(
                Resource.success(
                    SUCCESS_VERIFICATION_PAGE_REQUIRE_LIVE_CAPTURE
                )
            )

            viewModel.submitAndNavigate(
                mockController,
                ConsentDestination.ROUTE.route
            )

            verify(mockIdentityRepository).postVerificationPageSubmit(
                eq(VERIFICATION_SESSION_ID),
                eq(EPHEMERAL_KEY)
            )

            assertThat(viewModel.verificationPageSubmit.value).isEqualTo(Resource.success(Resource.DUMMY_RESOURCE))

            verify(mockController).navigate(
                eq(ConsentDestination.routeWithArgs),
                any<NavOptionsBuilder.() -> Unit>()
            )
        }
    }

    @Test
    fun `submitAndNavigate - submitted but not closed and no missings - error`() {
        runBlocking {
            whenever(
                mockIdentityRepository.postVerificationPageSubmit(
                    any(),
                    any()
                )
            ).thenReturn(SUBMITTED_AND_NOT_CLOSED_NO_MISSING_VERIFICATION_PAGE_DATA)

            viewModel._verificationPage.postValue(
                Resource.success(
                    SUCCESS_VERIFICATION_PAGE_REQUIRE_LIVE_CAPTURE
                )
            )

            viewModel.submitAndNavigate(
                mockController,
                ConsentDestination.ROUTE.route
            )

            verify(mockIdentityRepository).postVerificationPageSubmit(
                eq(VERIFICATION_SESSION_ID),
                eq(EPHEMERAL_KEY)
            )

            assertThat(viewModel.verificationPageSubmit.value).isEqualTo(Resource.success(Resource.DUMMY_RESOURCE))

            verify(mockController).navigate(
                argWhere {
                    it.startsWith(ErrorDestination.ERROR)
                },
                any<NavOptionsBuilder.() -> Unit>()
            )
        }
    }

    @Test
    fun `verify tfLite initialization success`() {
        viewModel.initializeTfLite()
        val successCaptor = argumentCaptor<() -> Unit>()

        verify(mockTfLiteInitializer).initialize(any(), successCaptor.capture(), any())

        successCaptor.firstValue.invoke()

        assertThat(viewModel.isTfLiteInitialized.value).isTrue()
    }

    @Test
    fun `verify tfLite initialization failure`() {
        viewModel.initializeTfLite()
        val failureCaptor = argumentCaptor<(Exception) -> Unit>()

        verify(mockTfLiteInitializer).initialize(any(), any(), failureCaptor.capture())

        assertFailsWith<Exception> {
            failureCaptor.firstValue.invoke(Exception())
        }
    }

    private fun testPostVerificationPageDataAndMaybeNavigate(
        verificationPageData: VerificationPageData,
        targetTopLevelDestination: IdentityTopLevelDestination
    ) = runBlocking {
        viewModel._verificationPage.postValue(
            Resource.success(
                SUCCESS_VERIFICATION_PAGE_REQUIRE_SELFIE_LIVE_CAPTURE
            )
        )

        whenever(
            mockIdentityRepository.postVerificationPageData(
                any(),
                any(),
                any(),
                any()
            )
        ).thenReturn(verificationPageData)

        val collectedDataParam = CollectedDataParam(biometricConsent = true)
        viewModel.postVerificationPageDataAndMaybeNavigate(
            mockController,
            collectedDataParam,
            ConsentDestination.ROUTE.route,
            mockOnMissingBack,
            mockOnMissingPhoneOtp,
            mockOnReadyToSubmit
        )

        verify(mockIdentityRepository).postVerificationPageData(
            eq(VERIFICATION_SESSION_ID),
            eq(EPHEMERAL_KEY),
            same(collectedDataParam),
            any()
        )

        verify(mockScreenTracker).screenTransitionStart(eq(SCREEN_NAME_CONSENT), any())
        assertThat(viewModel.verificationPageData.value).isEqualTo(Resource.success(Resource.DUMMY_RESOURCE))
        assertThat(viewModel.collectedData.value).isEqualTo(collectedDataParam)
        assertThat(viewModel.missingRequirements.value).isEqualTo(
            verificationPageData.requirements.missings!!.toSet()
        )
        verify(mockController).navigate(
            eq(targetTopLevelDestination.routeWithArgs),
            any<NavOptionsBuilder.() -> Unit>()
        )
    }

    private fun testPostVerificationPageDataAndMaybeNavigateWithCallback(
        verificationPageData: VerificationPageData,
        callback: () -> Unit
    ) = runBlocking {
        viewModel._verificationPage.postValue(
            Resource.success(
                SUCCESS_VERIFICATION_PAGE_REQUIRE_SELFIE_LIVE_CAPTURE
            )
        )

        whenever(
            mockIdentityRepository.postVerificationPageData(
                any(),
                any(),
                any(),
                any()
            )
        ).thenReturn(verificationPageData)

        val collectedDataParam = CollectedDataParam(biometricConsent = true)
        viewModel.postVerificationPageDataAndMaybeNavigate(
            mockController,
            collectedDataParam,
            ConsentDestination.ROUTE.route,
            mockOnMissingBack,
            mockOnMissingPhoneOtp,
            mockOnReadyToSubmit
        )

        verify(mockIdentityRepository).postVerificationPageData(
            eq(VERIFICATION_SESSION_ID),
            eq(EPHEMERAL_KEY),
            same(collectedDataParam),
            any()
        )

        verify(mockScreenTracker).screenTransitionStart(eq(SCREEN_NAME_CONSENT), any())
        assertThat(viewModel.verificationPageData.value).isEqualTo(Resource.success(Resource.DUMMY_RESOURCE))
        assertThat(viewModel.collectedData.value).isEqualTo(collectedDataParam)
        assertThat(viewModel.missingRequirements.value).isEqualTo(
            verificationPageData.requirements.missings!!.toSet()
        )
        callback()
    }

    private fun testUploadManualSuccessResult(isFront: Boolean) = runBlocking {
        mockUploadSuccess()

        val mockUri = mock<Uri>()
        viewModel.uploadManualResult(
            mockUri,
            isFront,
            true,
            DOCUMENT_CAPTURE,
            DocumentUploadParam.UploadMethod.FILEUPLOAD,
            IdentityScanState.ScanType.DOC_FRONT
        )

        verify(mockIdentityIO).resizeUriAndCreateFileToUpload(
            same(mockUri),
            eq(VERIFICATION_SESSION_ID),
            eq(false),
            eq(if (isFront) FRONT else BACK),
            eq(HIGH_RES_IMAGE_MAX_DIMENSION),
            eq(HIGH_RES_COMPRESSION_QUALITY)
        )
        verify(mockIdentityIO, never()).createTestModeFileToUpload(any())
        verify(mockIdentityRepository).uploadImage(
            eq(VERIFICATION_SESSION_ID),
            eq(EPHEMERAL_KEY),
            same(IMAGE_FILE),
            any(),
            any()
        )

        verify(mockIdentityAnalyticsRequestFactory).imageUpload(
            anyOrNull(),
            anyOrNull(),
            anyOrNull(),
            anyOrNull(),
            anyOrNull(),
            anyOrNull()
        )

        if (isFront) {
            viewModel.documentFrontUploadedState.value.highResResult
        } else {
            viewModel.documentBackUploadedState.value.highResResult
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
            if (isFront) {
                FINAL_ID_DETECTOR_RESULT_FRONT
            } else {
                FINAL_ID_DETECTOR_RESULT_BACK
            },
            mockVerificationPage
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
                if (isFront) {
                    "${VERIFICATION_SESSION_ID}_$FRONT.jpeg"
                } else {
                    "${VERIFICATION_SESSION_ID}_$BACK.jpeg"
                }
            ),
            eq(HIGH_RES_IMAGE_MAX_DIMENSION),
            eq(HIGH_RES_COMPRESSION_QUALITY)
        )
        if (isFront) {
            viewModel.documentFrontUploadedState.value.highResResult
        } else {
            viewModel.documentBackUploadedState.value.highResResult
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
                if (isFront) {
                    "${VERIFICATION_SESSION_ID}_${FRONT}_full_frame.jpeg"
                } else {
                    "${VERIFICATION_SESSION_ID}_${BACK}_full_frame.jpeg"
                }
            ),
            eq(LOW_RES_IMAGE_MAX_DIMENSION),
            eq(LOW_RES_COMPRESSION_QUALITY)
        )

        if (isFront) {
            viewModel.documentFrontUploadedState.value.lowResResult
        } else {
            viewModel.documentBackUploadedState.value.lowResResult
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

    private fun testUploadManualBitmapSuccessResult(isFront: Boolean) = runBlocking {
        mockUploadSuccess()

        val bitmap = mock<Bitmap>()
        viewModel.uploadManualResult(
            bitmap,
            isFront,
            true,
            DOCUMENT_CAPTURE,
            DocumentUploadParam.UploadMethod.MANUALCAPTURE,
            if (isFront) {
                IdentityScanState.ScanType.DOC_FRONT
            } else {
                IdentityScanState.ScanType.DOC_BACK
            }
        )

        verify(mockIdentityIO).resizeBitmapAndCreateFileToUpload(
            same(bitmap),
            eq(VERIFICATION_SESSION_ID),
            eq("${VERIFICATION_SESSION_ID}_${if (isFront) FRONT else BACK}.jpeg"),
            eq(HIGH_RES_IMAGE_MAX_DIMENSION),
            eq(HIGH_RES_COMPRESSION_QUALITY)
        )

        verify(mockIdentityAnalyticsRequestFactory).imageUpload(
            anyOrNull(),
            anyOrNull(),
            anyOrNull(),
            anyOrNull(),
            anyOrNull(),
            anyOrNull()
        )

        if (isFront) {
            viewModel.documentFrontUploadedState.value.highResResult
        } else {
            viewModel.documentBackUploadedState.value.highResResult
        }.let { uploadedResult ->
            assertThat(uploadedResult).isEqualTo(
                Resource.success(
                    UploadedResult(
                        UPLOADED_STRIPE_FILE,
                        null,
                        DocumentUploadParam.UploadMethod.MANUALCAPTURE
                    )
                )
            )
        }
    }

    @Suppress("CyclomaticComplexMethod", "LongMethod")
    private fun testUploadSelfieScanSuccessResult(
        selfie: FaceDetectorTransitioner.Selfie,
        isHighRes: Boolean
    ) {
        val selfieFrame = SELFIE_FRAME_BY_VARIANT.getValue(selfie)
        if (isHighRes) { // high res
            verify(mockIdentityIO).cropAndPadBitmap(
                same(selfieFrame.first.cameraPreviewImage.image),
                same(selfieFrame.second.boundingBox),
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
                        FaceDetectorTransitioner.Selfie.LEFT,
                        FaceDetectorTransitioner.Selfie.RIGHT -> error("Side selfies are full-frame only")
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
                    FaceDetectorTransitioner.Selfie.LEFT,
                    FaceDetectorTransitioner.Selfie.RIGHT -> error("Side selfies are full-frame only")
                }
            ).isEqualTo(
                Resource.success(
                    UploadedResult(
                        UPLOADED_STRIPE_FILE
                    )
                )
            )
        } else { // low res
            verify(mockIdentityIO).resizeBitmapAndCreateFileToUpload(
                same(selfieFrame.first.cameraPreviewImage.image),
                eq(VERIFICATION_SESSION_ID),
                eq(
                    when (selfie) {
                        FaceDetectorTransitioner.Selfie.FIRST -> "${VERIFICATION_SESSION_ID}_face_first_full_frame.jpeg"
                        FaceDetectorTransitioner.Selfie.BEST -> "${VERIFICATION_SESSION_ID}_face_full_frame.jpeg"
                        FaceDetectorTransitioner.Selfie.LAST -> "${VERIFICATION_SESSION_ID}_face_last_full_frame.jpeg"
                        FaceDetectorTransitioner.Selfie.LEFT -> "${VERIFICATION_SESSION_ID}_face_left_full_frame.jpeg"
                        FaceDetectorTransitioner.Selfie.RIGHT -> "${VERIFICATION_SESSION_ID}_face_right_full_frame.jpeg"
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
                    FaceDetectorTransitioner.Selfie.LEFT -> viewModel.selfieUploadState.value.leftFullFrameResult
                    FaceDetectorTransitioner.Selfie.RIGHT -> viewModel.selfieUploadState.value.rightFullFrameResult
                }
            ).isEqualTo(
                Resource.success(
                    UploadedResult(
                        UPLOADED_STRIPE_FILE
                    )
                )
            )
        }
    }

    private fun testUploadManualFailureResult(isFront: Boolean) = runBlocking {
        mockUploadFailure()

        viewModel.uploadManualResult(
            mock<Uri>(),
            isFront,
            true,
            DOCUMENT_CAPTURE,
            DocumentUploadParam.UploadMethod.FILEUPLOAD,
            IdentityScanState.ScanType.DOC_FRONT
        )

        verify(mockIdentityAnalyticsRequestFactory, times(0)).imageUpload(
            anyOrNull(),
            anyOrNull(),
            anyOrNull(),
            anyOrNull(),
            anyOrNull(),
            anyOrNull()
        )

        if (isFront) {
            viewModel.documentFrontUploadedState.value.highResResult
        } else {
            viewModel.documentBackUploadedState.value.highResResult
        }.let { uploadedResult ->
            assertThat(uploadedResult).isEqualTo(
                Resource.error<UploadedResult>(
                    msg = "Failed to upload file : $IMAGE_FILE_NAME",
                    throwable = UPLOADED_FAILURE_EXCEPTION
                )
            )
        }
    }

    private fun testForceConfirm(
        verificationPageDataResponse: VerificationPageData,
        paramsCallback: suspend (CollectedDataParam) -> Unit
    ) = runBlocking {
        val failedCollectedDataParam =
            CollectedDataParam(
                idDocumentFront = DocumentUploadParam(
                    highResImage = "high/res/image/path",
                    uploadMethod = DocumentUploadParam.UploadMethod.AUTOCAPTURE
                ),
                idDocumentBack = DocumentUploadParam(
                    highResImage = "high/res/image/path",
                    uploadMethod = DocumentUploadParam.UploadMethod.AUTOCAPTURE
                )
            )
        viewModel._collectedData.update {
            failedCollectedDataParam
        }

        whenever(
            mockIdentityRepository.postVerificationPageData(
                any(),
                any(),
                any(),
                any()
            )
        ).thenReturn(verificationPageDataResponse)
        paramsCallback(
            failedCollectedDataParam
        )
    }

    @Test
    fun `manual image shows loading before preparation is dispatched`() = runImageScenario {
        viewModel.documentFrontUploadedState.test {
            assertThat(awaitItem().highResResult.status).isEqualTo(Status.IDLE)
            selectUri()
            assertThat(awaitItem().highResResult.status).isEqualTo(Status.LOADING)
            identityIO.calls.expectNoEvents()
            repository.uploadCalls.expectNoEvents()

            scope.advanceUntilIdle()

            assertThat(identityIO.calls.awaitItem().operation).isEqualTo(Operation.READ_URI)
            assertThat(repository.uploadCalls.awaitItem().file).isEqualTo(IMAGE_FILE)
            assertThat(awaitItem().highResResult.status).isEqualTo(Status.SUCCESS)
        }
    }

    @Test
    fun `manual image and test mode asset are prepared off Main`() = runImageScenario(realWorker = true) {
        val mainThread = Thread.currentThread()
        viewModel.documentFrontUploadedState.test {
            awaitItem()
            selectUri(isLiveMode = false)
            assertThat(awaitItem().highResResult.status).isEqualTo(Status.LOADING)
            val read = identityIO.calls.awaitItem()
            assertThat(read.operation).isEqualTo(Operation.READ_URI)
            assertThat(read.thread).isNotSameInstanceAs(mainThread)
            val placeholder = identityIO.calls.awaitItem()
            assertThat(placeholder.operation).isEqualTo(Operation.TEST_MODE)
            assertThat(placeholder.thread).isNotSameInstanceAs(mainThread)
            val upload = repository.uploadCalls.awaitItem()
            assertThat(upload.file).isEqualTo(TEST_MODE_DOCUMENT_FRONT_FILE)
            assertThat(upload.thread).isSameInstanceAs(mainThread)
            assertThat(awaitItem().highResResult.status).isEqualTo(Status.SUCCESS)
        }
    }

    @Test
    fun `manual bitmap is prepared off Main`() = runImageScenario(realWorker = true) {
        val mainThread = Thread.currentThread()
        viewModel.documentFrontUploadedState.test {
            awaitItem()
            selectBitmap()
            assertThat(awaitItem().highResResult.status).isEqualTo(Status.LOADING)
            val preparation = identityIO.calls.awaitItem()
            assertThat(preparation.operation).isEqualTo(Operation.RESIZE_BITMAP)
            assertThat(preparation.thread).isNotSameInstanceAs(mainThread)
            repository.uploadCalls.awaitItem()
            assertThat(awaitItem().highResResult.status).isEqualTo(Status.SUCCESS)
        }
    }

    @Test
    fun `scanned document cropping and resizing run off Main`() = runImageScenario(realWorker = true) {
        val mainThread = Thread.currentThread()
        viewModel.documentFrontUploadedState.test {
            awaitItem()
            viewModel.uploadScanResult(FINAL_ID_DETECTOR_RESULT_FRONT, mockVerificationPage)
            repeat(3) {
                assertThat(identityIO.calls.awaitItem().thread).isNotSameInstanceAs(mainThread)
            }
            repeat(2) { repository.uploadCalls.awaitItem() }
            var state = awaitItem()
            while (!state.isUploaded()) state = awaitItem()
            assertThat(state.highResResult.status).isEqualTo(Status.SUCCESS)
            assertThat(state.lowResResult.status).isEqualTo(Status.SUCCESS)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `selfie cropping and resizing run off Main`() = runImageScenario(realWorker = true) {
        val mainThread = Thread.currentThread()
        viewModel.selfieUploadState.test {
            awaitItem()
            viewModel.uploadScanResult(FINAL_FACE_DETECTOR_RESULT, mockVerificationPage)
            // Three cropped high-resolution images and three full-frame images.
            repeat(9) {
                assertThat(identityIO.calls.awaitItem().thread).isNotSameInstanceAs(mainThread)
            }
            repeat(6) { repository.uploadCalls.awaitItem() }
            var state = awaitItem()
            while (state.isAnyLoading()) state = awaitItem()
            assertThat(state.bestHighResResult.status).isEqualTo(Status.SUCCESS)
            assertThat(state.lastLowResResult.status).isEqualTo(Status.SUCCESS)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `bitmap preparation failure reports an error without uploading`() = runImageScenario {
        val failure = IOException("Cannot encode image")
        identityIO.preparationFailure = failure
        selectBitmap()
        scope.advanceUntilIdle()

        assertThat(identityIO.calls.awaitItem().operation).isEqualTo(Operation.RESIZE_BITMAP)
        val result = viewModel.documentFrontUploadedState.value.highResResult
        assertThat(result.status).isEqualTo(Status.ERROR)
        assertThat(result.throwable?.cause).isInstanceOf(IOException::class.java)
        assertThat(result.throwable?.cause?.message).isEqualTo(failure.message)
        repository.uploadCalls.expectNoEvents()
    }

    @Test
    fun `high resolution preparation failure still uploads the full frame`() = runImageScenario {
        identityIO.cropFailure = IOException("Cannot crop image")
        viewModel.uploadScanResult(FINAL_ID_DETECTOR_RESULT_FRONT, mockVerificationPage)
        scope.advanceUntilIdle()

        assertThat(identityIO.calls.awaitItem().operation).isEqualTo(Operation.CROP)
        assertThat(identityIO.calls.awaitItem().operation).isEqualTo(Operation.RESIZE_BITMAP)
        repository.uploadCalls.awaitItem()
        assertThat(viewModel.documentFrontUploadedState.value.highResResult.status).isEqualTo(Status.ERROR)
        assertThat(viewModel.documentFrontUploadedState.value.lowResResult.status).isEqualTo(Status.SUCCESS)
    }

    @Test
    fun `invalid file purpose reports a preparation error`() = runImageScenario {
        viewModel.uploadManualResult(
            uri = Uri.EMPTY,
            isFront = true,
            isLiveMode = true,
            docCapturePage = DOCUMENT_CAPTURE.copy(filePurpose = "unsupported"),
            uploadMethod = DocumentUploadParam.UploadMethod.FILEUPLOAD,
            scanType = IdentityScanState.ScanType.DOC_FRONT
        )
        scope.advanceUntilIdle()

        assertThat(viewModel.documentFrontUploadedState.value.highResResult.status).isEqualTo(Status.ERROR)
        identityIO.calls.expectNoEvents()
        repository.uploadCalls.expectNoEvents()
    }

    @Test
    fun `clearing a queued document cancels its preparation`() = runImageScenario {
        selectUri()
        assertThat(viewModel.documentFrontUploadedState.value.isLoading()).isTrue()
        viewModel.clearDocumentUploadedState()
        scope.advanceUntilIdle()

        assertThat(viewModel.documentFrontUploadedState.value.highResResult.status).isEqualTo(Status.IDLE)
        identityIO.calls.expectNoEvents()
        repository.uploadCalls.expectNoEvents()
        verify(mockIdentityAnalyticsRequestFactory, never()).genericError(
            throwable = any(), additionalMetadata = any(), overrideMessage = anyOrNull()
        )
    }

    @Test
    fun `clearing queued selfies cancels their preparation`() = runImageScenario {
        viewModel.uploadScanResult(FINAL_FACE_DETECTOR_RESULT, mockVerificationPage)
        assertThat(viewModel.selfieUploadState.value.isAnyLoading()).isTrue()
        viewModel.clearSelfieUploadedState()
        scope.advanceUntilIdle()

        assertThat(viewModel.selfieUploadState.value.bestHighResResult.status).isEqualTo(Status.IDLE)
        identityIO.calls.expectNoEvents()
        repository.uploadCalls.expectNoEvents()
        verify(mockIdentityAnalyticsRequestFactory, never()).genericError(
            throwable = any(), additionalMetadata = any(), overrideMessage = anyOrNull()
        )
    }

    @Test
    fun `preparation cancellation is not reported as an upload error`() = runImageScenario {
        identityIO.preparationFailure = CancellationException("Cancelled image")
        selectUri()
        scope.advanceUntilIdle()

        identityIO.calls.awaitItem()
        repository.uploadCalls.expectNoEvents()
        verify(mockIdentityAnalyticsRequestFactory, never()).genericError(
            throwable = any(), additionalMetadata = any(), overrideMessage = anyOrNull()
        )
    }

    @Test
    fun `upload cancellation is not reported as an upload error`() = runImageScenario {
        repository.result = Result.failure(CancellationException("Cancelled upload"))
        selectUri()
        scope.advanceUntilIdle()

        identityIO.calls.awaitItem()
        repository.uploadCalls.awaitItem()
        verify(mockIdentityAnalyticsRequestFactory, never()).genericError(
            throwable = any(), additionalMetadata = any(), overrideMessage = anyOrNull()
        )
    }

    @Test
    fun `replacement waits for an old upload to release its file`() = runImageScenario {
        val release = CompletableDeferred<Unit>()
        repository.uploadGate = release
        repository.finishUploadOnCancellation = true
        try {
            selectUri(Uri.parse("content://image/old"))
            scope.advanceUntilIdle()
            identityIO.calls.awaitItem()
            repository.uploadCalls.awaitItem()

            val replacement = Uri.parse("content://image/new")
            selectUri(replacement)
            scope.advanceUntilIdle()
            identityIO.calls.expectNoEvents()
            repository.uploadCalls.expectNoEvents()

            release.complete(Unit)
            scope.advanceUntilIdle()
            assertThat(identityIO.calls.awaitItem().uri).isEqualTo(replacement)
            repository.uploadCalls.awaitItem()
            assertThat(viewModel.documentFrontUploadedState.value.highResResult.status).isEqualTo(Status.SUCCESS)
            verify(mockIdentityAnalyticsRequestFactory, times(1)).imageUpload(
                anyOrNull(), anyOrNull(), anyOrNull(), anyOrNull(), anyOrNull(), anyOrNull()
            )
        } finally {
            release.complete(Unit)
        }
    }

    @Test
    fun `clearing a running preparation prevents stale upload and state`() = runImageScenario(realWorker = true) {
        val release = CountDownLatch(1)
        identityIO.preparationGate = release
        try {
            selectUri()
            identityIO.calls.awaitItem()
            viewModel.clearDocumentUploadedState()
        } finally {
            release.countDown()
        }
        // Joining children also waits for the non-interruptible preparation to return.
        viewModel.viewModelScope.coroutineContext[kotlinx.coroutines.Job]!!.children
            .filter { it.isCancelled }.forEach { it.join() }
        scope.advanceUntilIdle()
        assertThat(viewModel.documentFrontUploadedState.value.highResResult.status).isEqualTo(Status.IDLE)
        repository.uploadCalls.expectNoEvents()
        verify(mockIdentityAnalyticsRequestFactory, never()).genericError(
            throwable = any(), additionalMetadata = any(), overrideMessage = anyOrNull()
        )
    }

    @Test
    fun `replacement during encoding only uploads the newest selection`() = runImageScenario(realWorker = true) {
        val release = CountDownLatch(1)
        val original = Uri.parse("content://image/original")
        val replacement = Uri.parse("content://image/replacement")
        val replacementFile = File("replacement.jpeg")
        identityIO.filesByUri = mapOf(original to File("original.jpeg"), replacement to replacementFile)
        identityIO.preparationGate = release
        viewModel.documentFrontUploadedState.test {
            awaitItem()
            try {
                selectUri(original)
                assertThat(awaitItem().highResResult.status).isEqualTo(Status.LOADING)
                assertThat(identityIO.calls.awaitItem().uri).isEqualTo(original)
                selectUri(Uri.parse("content://image/superseded"))
                selectUri(replacement)
                scope.runCurrent()
                identityIO.calls.expectNoEvents()
                repository.uploadCalls.expectNoEvents()
            } finally {
                release.countDown()
            }
            assertThat(identityIO.calls.awaitItem().uri).isEqualTo(replacement)
            assertThat(repository.uploadCalls.awaitItem().file).isEqualTo(replacementFile)
            assertThat(awaitItem().highResResult.status).isEqualTo(Status.SUCCESS)
            verify(mockIdentityAnalyticsRequestFactory, times(1)).imageUpload(
                anyOrNull(), anyOrNull(), anyOrNull(), anyOrNull(), anyOrNull(), anyOrNull()
            )
        }
    }

    @Test
    fun `rapid queued selections prepare and upload only the latest image`() = runImageScenario {
        val latest = Uri.parse("content://image/latest")
        selectUri(Uri.parse("content://image/first"))
        selectUri(Uri.parse("content://image/second"))
        selectUri(latest)
        scope.advanceUntilIdle()

        assertThat(identityIO.calls.awaitItem().uri).isEqualTo(latest)
        repository.uploadCalls.awaitItem()
        assertThat(viewModel.documentFrontUploadedState.value.highResResult.status).isEqualTo(Status.SUCCESS)
    }

    @Test
    fun `late upload failure after reset does not publish state or error telemetry`() = runImageScenario {
        val release = CompletableDeferred<Unit>()
        repository.uploadGate = release
        repository.finishUploadOnCancellation = true
        repository.result = Result.failure(IOException("Late network failure"))
        viewModel.documentFrontUploadedState.test {
            awaitItem()
            try {
                selectUri()
                assertThat(awaitItem().highResResult.status).isEqualTo(Status.LOADING)
                scope.advanceUntilIdle()
                identityIO.calls.awaitItem()
                repository.uploadCalls.awaitItem()
                viewModel.clearDocumentUploadedState()
                assertThat(awaitItem().highResResult.status).isEqualTo(Status.IDLE)
            } finally {
                release.complete(Unit)
            }
            scope.advanceUntilIdle()
            expectNoEvents()
            verify(mockIdentityAnalyticsRequestFactory, never()).genericError(
                throwable = any(), additionalMetadata = any(), overrideMessage = anyOrNull()
            )
        }
    }

    @Test
    @Suppress("RestrictedApi") // Use the same clearing mechanism as ViewModelProvider's owner.
    fun `clearing the ViewModel during encoding prevents upload`() = runImageScenario(realWorker = true) {
        val store = ViewModelStore().apply { put("identity", viewModel) }
        val viewModelJob = requireNotNull(viewModel.viewModelScope.coroutineContext[Job])
        val release = CountDownLatch(1)
        identityIO.preparationGate = release
        viewModel.documentFrontUploadedState.test {
            awaitItem()
            try {
                selectUri()
                assertThat(awaitItem().highResResult.status).isEqualTo(Status.LOADING)
                identityIO.calls.awaitItem()
                store.clear()
                assertThat(viewModelJob.isCancelled).isTrue()
            } finally {
                release.countDown()
                store.clear()
            }
            viewModelJob.join()
            repository.uploadCalls.expectNoEvents()
            expectNoEvents()
            verify(mockIdentityAnalyticsRequestFactory, never()).genericError(
                throwable = any(), additionalMetadata = any(), overrideMessage = anyOrNull()
            )
        }
    }

    @Test
    fun `a pending front upload does not block the back upload`() = runImageScenario {
        val front = Uri.parse("content://image/front")
        val back = Uri.parse("content://image/back")
        val frontFile = File("front.jpeg")
        val backFile = File("back.jpeg")
        val release = CompletableDeferred<Unit>()
        identityIO.filesByUri = mapOf(front to frontFile, back to backFile)
        repository.uploadGatesByFile = mapOf(frontFile to release)
        try {
            selectUri(front)
            scope.advanceUntilIdle()
            assertThat(identityIO.calls.awaitItem().uri).isEqualTo(front)
            assertThat(repository.uploadCalls.awaitItem().file).isEqualTo(frontFile)
            assertThat(viewModel.documentFrontUploadedState.value.isLoading()).isTrue()

            selectUri(back, isFront = false)
            scope.advanceUntilIdle()
            assertThat(identityIO.calls.awaitItem().uri).isEqualTo(back)
            assertThat(repository.uploadCalls.awaitItem().file).isEqualTo(backFile)
            assertThat(viewModel.documentBackUploadedState.value.highResResult.status).isEqualTo(Status.SUCCESS)
            assertThat(viewModel.documentFrontUploadedState.value.isLoading()).isTrue()
        } finally {
            release.complete(Unit)
        }
        scope.advanceUntilIdle()
        assertThat(viewModel.documentFrontUploadedState.value.highResResult.status).isEqualTo(Status.SUCCESS)
    }

    @Test
    fun `selfie crop failures still upload every full frame`() = runImageScenario {
        identityIO.cropFailure = IOException("Cannot crop selfie")
        viewModel.uploadScanResult(FINAL_FACE_DETECTOR_RESULT, mockVerificationPage)
        scope.advanceUntilIdle()

        repeat(3) {
            assertThat(identityIO.calls.awaitItem().operation).isEqualTo(Operation.CROP)
            assertThat(identityIO.calls.awaitItem().operation).isEqualTo(Operation.RESIZE_BITMAP)
            repository.uploadCalls.awaitItem()
        }
        val state = viewModel.selfieUploadState.value
        assertThat(state.firstHighResResult.status).isEqualTo(Status.ERROR)
        assertThat(state.bestHighResResult.status).isEqualTo(Status.ERROR)
        assertThat(state.lastHighResResult.status).isEqualTo(Status.ERROR)
        assertThat(state.firstLowResResult.status).isEqualTo(Status.SUCCESS)
        assertThat(state.bestLowResResult.status).isEqualTo(Status.SUCCESS)
        assertThat(state.lastLowResResult.status).isEqualTo(Status.SUCCESS)
    }

    @Test
    fun `missing test mode asset fails without uploading the real document`() = runImageScenario {
        identityIO.placeholderFailure = IOException("Missing test-mode image")
        selectUri(isLiveMode = false)
        scope.advanceUntilIdle()

        assertThat(identityIO.calls.awaitItem().operation).isEqualTo(Operation.READ_URI)
        assertThat(identityIO.calls.awaitItem().operation).isEqualTo(Operation.TEST_MODE)
        assertThat(viewModel.documentFrontUploadedState.value.highResResult.status).isEqualTo(Status.ERROR)
        repository.uploadCalls.expectNoEvents()
    }

    @Test
    fun `a failed preparation releases the slot for a successful retry`() = runImageScenario {
        identityIO.preparationFailure = IOException("Failed image read")
        selectUri()
        scope.advanceUntilIdle()
        identityIO.calls.awaitItem()
        assertThat(viewModel.documentFrontUploadedState.value.highResResult.status).isEqualTo(Status.ERROR)
        repository.uploadCalls.expectNoEvents()

        identityIO.preparationFailure = null
        val retry = Uri.parse("content://image/retry")
        selectUri(retry)
        scope.advanceUntilIdle()
        assertThat(identityIO.calls.awaitItem().uri).isEqualTo(retry)
        repository.uploadCalls.awaitItem()
        assertThat(viewModel.documentFrontUploadedState.value.highResResult.status).isEqualTo(Status.SUCCESS)
    }

    private fun runImageScenario(
        realWorker: Boolean = false,
        block: suspend ImageScenario.() -> Unit,
    ) = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        val identityIO = FakeIdentityIO(IMAGE_FILE, CROPPED_BITMAP, TEST_MODE_DOCUMENT_FRONT_FILE)
        val repository = FakeImageUploadRepository(mockIdentityRepository)
        val viewModel = createViewModel(
            identityIO = identityIO,
            repository = repository,
            workContext = if (realWorker) Dispatchers.IO else StandardTestDispatcher(testScheduler),
            savedStateHandle = SavedStateHandle(),
        )
        try {
            ImageScenario(viewModel, identityIO, repository, this).block()
            identityIO.ensureAllEventsConsumed()
            repository.ensureAllEventsConsumed()
        } finally {
            viewModel.viewModelScope.cancel()
        }
    }

    private data class ImageScenario(
        val viewModel: IdentityViewModel,
        val identityIO: FakeIdentityIO,
        val repository: FakeImageUploadRepository,
        val scope: TestScope,
    ) {
        fun selectUri(uri: Uri = Uri.EMPTY, isLiveMode: Boolean = true, isFront: Boolean = true) {
            viewModel.uploadManualResult(
                uri, isFront, isLiveMode, DOCUMENT_CAPTURE,
                DocumentUploadParam.UploadMethod.FILEUPLOAD,
                if (isFront) IdentityScanState.ScanType.DOC_FRONT else IdentityScanState.ScanType.DOC_BACK
            )
        }

        fun selectBitmap() {
            viewModel.uploadManualResult(
                INPUT_BITMAP, true, true, DOCUMENT_CAPTURE,
                DocumentUploadParam.UploadMethod.MANUALCAPTURE, IdentityScanState.ScanType.DOC_FRONT
            )
        }
    }

    private companion object {
        const val VERIFICATION_SESSION_ID = "id_5678"
        const val EPHEMERAL_KEY = "eak_5678"
        val BRAND_LOGO = mock<Uri>()
        const val IMAGE_FILE_NAME = "fileName"
        val IMAGE_FILE = File(IMAGE_FILE_NAME)
        val TEST_MODE_DOCUMENT_FRONT_FILE = File("stripe_identity_test_mode_document_front.png")
        val TEST_MODE_DOCUMENT_BACK_FILE = File("stripe_identity_test_mode_document_back.png")
        val TEST_MODE_SELFIE_FILE = File("stripe_identity_test_mode_selfie.png")
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

        val REQUIREMENTS_NO_MISSING = VerificationPageRequirements(missing = listOf())

        val UPLOADED_STRIPE_FILE = StripeFile()
        val UPLOADED_FAILURE_EXCEPTION = APIException()

        val INPUT_BITMAP = mock<Bitmap>()
        val CROPPED_BITMAP = mock<Bitmap>()
        val EXTRACTED_BITMAP = mock<Bitmap>()
        val BOUNDING_BOX = mock<BoundingBox>()
        val ALL_SCORES = listOf(1f, 2f, 3f)
        val FINAL_ID_DETECTOR_RESULT_FRONT = IdentityAggregator.FinalResult(
            frame = AnalyzerInput(
                CameraPreviewImage(
                    INPUT_BITMAP,
                    mock()
                ),
                mock()
            ),
            result = IDDetectorOutput(
                boundingBox = BOUNDING_BOX,
                category = Category.ID_FRONT,
                resultScore = 0.8f,
                allScores = ALL_SCORES,
                blurScore = 1.0f,
                croppedImage = INPUT_BITMAP
            ),
            identityState = mock<IdentityScanState.Finished>()
        )
        val FINAL_ID_DETECTOR_RESULT_BACK = IdentityAggregator.FinalResult(
            frame = AnalyzerInput(
                CameraPreviewImage(
                    INPUT_BITMAP,
                    mock()
                ),
                mock()
            ),
            result = IDDetectorOutput(
                boundingBox = BOUNDING_BOX,
                category = Category.ID_BACK,
                resultScore = 0.8f,
                allScores = ALL_SCORES,
                blurScore = 1.0f,
                croppedImage = INPUT_BITMAP
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
            ) // last
        )
        val LEFT_SELFIE_FRAME =
            AnalyzerInput(
                cameraPreviewImage = CameraPreviewImage(
                    image = mock(),
                    viewBounds = mock()
                ),
                viewFinderBounds = mock()
            ) to FaceDetectorOutput(
                boundingBox = mock(),
                resultScore = 0.83f
            )
        val RIGHT_SELFIE_FRAME =
            AnalyzerInput(
                cameraPreviewImage = CameraPreviewImage(
                    image = mock(),
                    viewBounds = mock()
                ),
                viewFinderBounds = mock()
            ) to FaceDetectorOutput(
                boundingBox = mock(),
                resultScore = 0.84f
            )
        val SELFIE_FRAME_BY_VARIANT = mapOf(
            FaceDetectorTransitioner.Selfie.FIRST to FILTERED_FRAMES[FaceDetectorTransitioner.INDEX_FIRST],
            FaceDetectorTransitioner.Selfie.BEST to FILTERED_FRAMES[FaceDetectorTransitioner.INDEX_BEST],
            FaceDetectorTransitioner.Selfie.LAST to FILTERED_FRAMES[FaceDetectorTransitioner.INDEX_LAST],
            FaceDetectorTransitioner.Selfie.LEFT to LEFT_SELFIE_FRAME,
            FaceDetectorTransitioner.Selfie.RIGHT to RIGHT_SELFIE_FRAME
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
                    on { sideSelfies }.thenReturn(
                        listOf(
                            FaceDetectorTransitioner.Selfie.RIGHT,
                            FaceDetectorTransitioner.Selfie.LEFT
                        )
                    )
                    SELFIE_FRAME_BY_VARIANT.forEach { (selfie, frame) ->
                        on { frameForSelfie(selfie) }.thenReturn(frame)
                    }
                }
            )
        )

        val ID_DETECTOR_FILE = mock<File>()
        val FACE_DETECTOR_FILE = mock<File>()
    }
}
