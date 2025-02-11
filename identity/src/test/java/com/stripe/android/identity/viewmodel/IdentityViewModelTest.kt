package com.stripe.android.identity.viewmodel

import android.graphics.Bitmap
import android.net.Uri
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.SavedStateHandle
import androidx.navigation.NavController
import androidx.navigation.NavOptionsBuilder
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
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
import com.stripe.android.identity.networking.UploadedResult
import com.stripe.android.identity.networking.models.CollectedDataParam
import com.stripe.android.identity.networking.models.DocumentUploadParam
import com.stripe.android.identity.networking.models.Requirement
import com.stripe.android.identity.networking.models.VerificationPage
import com.stripe.android.identity.networking.models.VerificationPageData
import com.stripe.android.identity.networking.models.VerificationPageRequirements
import com.stripe.android.identity.networking.models.VerificationPageStaticContentDocumentCaptureModels
import com.stripe.android.identity.networking.models.VerificationPageStaticContentDocumentCapturePage
import com.stripe.android.identity.networking.models.VerificationPageStaticContentSelfieCapturePage
import com.stripe.android.identity.networking.models.VerificationPageStaticContentSelfieModels
import com.stripe.android.identity.states.FaceDetectorTransitioner
import com.stripe.android.identity.states.IdentityScanState
import com.stripe.android.identity.utils.IdentityIO
import com.stripe.android.identity.viewmodel.IdentityViewModel.Companion.BACK
import com.stripe.android.identity.viewmodel.IdentityViewModel.Companion.FRONT
import com.stripe.android.mlcore.base.InterpreterInitializer
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.UnconfinedTestDispatcher
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
import org.mockito.kotlin.same
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.robolectric.RobolectricTestRunner
import java.io.File
import kotlin.test.assertFailsWith

@RunWith(RobolectricTestRunner::class)
internal class IdentityViewModelTest {
    @get:Rule
    var rule: TestRule = InstantTaskExecutorRule()

    private val mockVerificationPage = mock<VerificationPage> {
        on { documentCapture }.thenReturn(DOCUMENT_CAPTURE)
        on { selfieCapture }.thenReturn(SELFIE_CAPTURE)
        on { requirements }.thenReturn(REQUIREMENTS_NO_MISSING)
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

    @OptIn(ExperimentalCoroutinesApi::class)
    private val viewModel = IdentityViewModel(
        ApplicationProvider.getApplicationContext(),
        IdentityVerificationSheetContract.Args(
            verificationSessionId = VERIFICATION_SESSION_ID,
            ephemeralKeySecret = EPHEMERAL_KEY,
            brandLogo = BRAND_LOGO,
            injectorKey = DUMMY_INJECTOR_KEY,
            presentTime = 0
        ),
        mockIdentityRepository,
        mockIdentityModelFetcher,
        mockIdentityIO,
        mockIdentityAnalyticsRequestFactory,
        mockScreenTracker,
        mock(),
        mockTfLiteInitializer,
        mockSavedStateHandle,
        mock(),
        UnconfinedTestDispatcher(),
        mock()
    )

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
    fun `uploadManualResult front failure notifies _documentUploadedState`() {
        testUploadManualFailureResult(true)
    }

    @Test
    fun `uploadManualResult back failure notifies _documentUploadedState`() {
        testUploadManualFailureResult(false)
    }

    @Test
    fun `legacy uploadScanResult front success uploads both files and notifies _documentUploadedState`() {
        testUploadDocumentScanSuccessResult(isFront = true)
    }

    @Test
    fun `legacy uploadScanResult back success uploads both files and notifies _documentUploadedState`() {
        testUploadDocumentScanSuccessResult(isFront = false)
    }

    @Test
    fun `uploadScanResult uploads all files and notifies _selfieUploadedState`() = runBlocking {
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
            verify(mockIdentityAnalyticsRequestFactory, times(6)).imageUpload(
                anyOrNull(),
                anyOrNull(),
                anyOrNull(),
                anyOrNull(),
                anyOrNull(),
                anyOrNull()
            )
            listOf(true, false).forEach { isHighRes ->
                testUploadSelfieScanSuccessResult(selfie, isHighRes)
            }
        }
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

            verify(mockController).navigate(
                eq(ConfirmationDestination.routeWithArgs),
                any<NavOptionsBuilder.() -> Unit>()
            )
        }
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
                FINAL_ID_DETECTOR_LEGACY_RESULT_FRONT
            } else {
                FINAL_ID_DETECTOR_LEGACY_RESULT_BACK
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
                        UPLOADED_STRIPE_FILE
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
                        UPLOADED_STRIPE_FILE
                    )
                )
            )
        }
    }

    private fun testUploadManualFailureResult(isFront: Boolean) = runBlocking {
        mockUploadFailure()

        viewModel.uploadManualResult(
            mock(),
            isFront,
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

        val REQUIREMENTS_NO_MISSING = VerificationPageRequirements(missing = listOf())

        val UPLOADED_STRIPE_FILE = StripeFile()
        val UPLOADED_FAILURE_EXCEPTION = APIException()

        val INPUT_BITMAP = mock<Bitmap>()
        val CROPPED_BITMAP = mock<Bitmap>()
        val EXTRACTED_BITMAP = mock<Bitmap>()
        val BOUNDING_BOX = mock<BoundingBox>()
        val ALL_SCORES = listOf(1f, 2f, 3f)
        val FINAL_ID_DETECTOR_LEGACY_RESULT_FRONT = IdentityAggregator.FinalResult(
            frame = AnalyzerInput(
                CameraPreviewImage(
                    INPUT_BITMAP,
                    mock()
                ),
                mock()
            ),
            result = IDDetectorOutput.Legacy(
                boundingBox = BOUNDING_BOX,
                category = Category.ID_FRONT,
                resultScore = 0.8f,
                allScores = ALL_SCORES,
                blurScore = 1.0f
            ),
            identityState = mock<IdentityScanState.Finished>()
        )
        val FINAL_ID_DETECTOR_LEGACY_RESULT_BACK = IdentityAggregator.FinalResult(
            frame = AnalyzerInput(
                CameraPreviewImage(
                    INPUT_BITMAP,
                    mock()
                ),
                mock()
            ),
            result = IDDetectorOutput.Legacy(
                boundingBox = BOUNDING_BOX,
                category = Category.ID_BACK,
                resultScore = 0.8f,
                allScores = ALL_SCORES,
                blurScore = 1.0f
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
    }
}
