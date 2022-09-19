package com.stripe.android.identity.navigation

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.fragment.app.testing.launchFragmentInContainer
import androidx.lifecycle.MutableLiveData
import androidx.navigation.Navigation
import androidx.navigation.testing.TestNavHostController
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import com.stripe.android.camera.CameraPermissionEnsureable
import com.stripe.android.identity.R
import com.stripe.android.identity.analytics.IdentityAnalyticsRequestFactory
import com.stripe.android.identity.analytics.IdentityAnalyticsRequestFactory.Companion.EVENT_CAMERA_PERMISSION_DENIED
import com.stripe.android.identity.analytics.IdentityAnalyticsRequestFactory.Companion.EVENT_CAMERA_PERMISSION_GRANTED
import com.stripe.android.identity.analytics.IdentityAnalyticsRequestFactory.Companion.EVENT_SCREEN_PRESENTED
import com.stripe.android.identity.analytics.IdentityAnalyticsRequestFactory.Companion.PARAM_EVENT_META_DATA
import com.stripe.android.identity.analytics.IdentityAnalyticsRequestFactory.Companion.PARAM_SCREEN_NAME
import com.stripe.android.identity.analytics.IdentityAnalyticsRequestFactory.Companion.SCREEN_NAME_DOC_SELECT
import com.stripe.android.identity.analytics.ScreenTracker
import com.stripe.android.identity.navigation.CameraPermissionDeniedFragment.Companion.ARG_SCAN_TYPE
import com.stripe.android.identity.navigation.DocSelectionFragment.Companion.DRIVING_LICENSE_KEY
import com.stripe.android.identity.navigation.DocSelectionFragment.Companion.ID_CARD_KEY
import com.stripe.android.identity.navigation.DocSelectionFragment.Companion.PASSPORT_KEY
import com.stripe.android.identity.networking.Resource
import com.stripe.android.identity.networking.models.ClearDataParam.Companion.DOC_SELECT_TO_UPLOAD
import com.stripe.android.identity.networking.models.ClearDataParam.Companion.DOC_SELECT_TO_UPLOAD_WITH_SELFIE
import com.stripe.android.identity.networking.models.CollectedDataParam
import com.stripe.android.identity.networking.models.VerificationPage
import com.stripe.android.identity.networking.models.VerificationPageData
import com.stripe.android.identity.networking.models.VerificationPageDataRequirements
import com.stripe.android.identity.networking.models.VerificationPageRequirements
import com.stripe.android.identity.networking.models.VerificationPageStaticContentDocumentCapturePage
import com.stripe.android.identity.networking.models.VerificationPageStaticContentDocumentSelectPage
import com.stripe.android.identity.viewModelFactoryFor
import com.stripe.android.identity.viewmodel.IdentityViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import org.junit.Ignore
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TestRule
import org.junit.runner.RunWith
import org.mockito.kotlin.KArgumentCaptor
import org.mockito.kotlin.any
import org.mockito.kotlin.argThat
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.robolectric.RobolectricTestRunner

@ExperimentalCoroutinesApi
@RunWith(RobolectricTestRunner::class)
internal class DocSelectionFragmentTest {
    @get:Rule
    var rule: TestRule = InstantTaskExecutorRule()

    private val verificationPage = mock<VerificationPage>()
    private val verificationPageLiveData = MutableLiveData<Resource<VerificationPage>>()
    private val mockScreenTracker = mock<ScreenTracker>()
    private val testDispatcher = UnconfinedTestDispatcher()

    private val mockIdentityViewModel = mock<IdentityViewModel> {
        on { identityAnalyticsRequestFactory }.thenReturn(
            IdentityAnalyticsRequestFactory(
                context = ApplicationProvider.getApplicationContext(),
                args = mock()
            )
        )
        on { screenTracker }.thenReturn(mockScreenTracker)
        on { uiContext } doReturn testDispatcher
        on { workContext } doReturn testDispatcher
        on { verificationPage } doReturn verificationPageLiveData
    }
    private val mockCameraPermissionEnsureable = mock<CameraPermissionEnsureable>()
    private val onCameraReadyCaptor = argumentCaptor<() -> Unit>()
    private val onUserDeniedCameraPermissionCaptor = argumentCaptor<() -> Unit>()


    private fun setUpSuccessVerificationPage(times: Int = 1) {
        val successCaptor: KArgumentCaptor<(VerificationPage) -> Unit> = argumentCaptor()
        verify(
            mockIdentityViewModel,
            times(times)
        ).observeForVerificationPage(
            any(),
            successCaptor.capture(),
            any()
        )
        successCaptor.lastValue(verificationPage)
    }


    @Test
    fun `analytics request is sent when response is valid`() {
        launchDocSelectionFragment { _, _ ->
            whenever(verificationPage.documentSelect).thenReturn(
                DOC_SELECT_MULTI_CHOICE
            )
            verificationPageLiveData.postValue(Resource.success(verificationPage))

            verify(mockIdentityViewModel).sendAnalyticsRequest(
                argThat {
                    eventName == EVENT_SCREEN_PRESENTED &&
                        (params[PARAM_EVENT_META_DATA] as Map<*, *>)[PARAM_SCREEN_NAME] == SCREEN_NAME_DOC_SELECT
                }
            )
        }
    }


    @Test
    @Ignore(
        "Jetpack compose test doesn't work with traditional navigation component in NavHostFragment, " +
            "update this test once all fragments are removed and the activity is implemented with NavHost"
    )
    fun `when scan is available, clicking continue navigates to scan without selfie`() {
        launchDocSelectionFragment { navController, _ ->
            runBlocking {
                whenever(verificationPage.documentSelect).thenReturn(
                    DOC_SELECT_SINGLE_CHOICE_DL
                )
                whenever(mockIdentityViewModel.postVerificationPageData(any(), any())).thenReturn(
                    MISSING_BACK_VERIFICATION_PAGE_DATA
                )
                // mock file is available
                whenever(mockIdentityViewModel.idDetectorModelFile).thenReturn(
                    MutableLiveData(
                        Resource.success(
                            mock()
                        )
                    )
                )
                setUpSuccessVerificationPage()
                // trigger navigation
//                binding.singleSelectionContinue.findViewById<MaterialButton>(R.id.button)
//                    .callOnClick()

                verify(mockScreenTracker).screenTransitionStart(eq(SCREEN_NAME_DOC_SELECT), any())

                verify(mockIdentityViewModel).postVerificationPageData(
                    eq(
                        CollectedDataParam(idDocumentType = CollectedDataParam.Type.DRIVINGLICENSE)
                    ),
                    eq(DOC_SELECT_TO_UPLOAD)
                )

                verify(mockCameraPermissionEnsureable).ensureCameraPermission(
                    onCameraReadyCaptor.capture(),
                    onUserDeniedCameraPermissionCaptor.capture()
                )

                // trigger permission granted
                onCameraReadyCaptor.firstValue()

                assertThat(navController.currentDestination?.id)
                    .isEqualTo(R.id.driverLicenseScanFragment)
            }
        }
    }

    @Test
    @Ignore(
        "Jetpack compose test doesn't work with traditional navigation component in NavHostFragment, " +
            "update this test once all fragments are removed and the activity is implemented with NavHost"
    )
    fun `when scan is available, clicking continue navigates to scan with selfie`() {
        launchDocSelectionFragment { navController, _ ->
            runBlocking {
                whenever(verificationPage.documentSelect).thenReturn(
                    DOC_SELECT_SINGLE_CHOICE_DL
                )
                whenever(verificationPage.selfieCapture).thenReturn(mock())
                whenever(mockIdentityViewModel.postVerificationPageData(any(), any())).thenReturn(
                    MISSING_BACK_VERIFICATION_PAGE_DATA
                )
                // mock file is available
                whenever(mockIdentityViewModel.idDetectorModelFile).thenReturn(
                    MutableLiveData(
                        Resource.success(
                            mock()
                        )
                    )
                )
                setUpSuccessVerificationPage()
                // trigger navigation
//                binding.singleSelectionContinue.findViewById<MaterialButton>(R.id.button)
//                    .callOnClick()

                verify(mockScreenTracker).screenTransitionStart(eq(SCREEN_NAME_DOC_SELECT), any())

                verify(mockIdentityViewModel).postVerificationPageData(
                    eq(
                        CollectedDataParam(idDocumentType = CollectedDataParam.Type.DRIVINGLICENSE)
                    ),
                    eq(DOC_SELECT_TO_UPLOAD_WITH_SELFIE)
                )

                verify(mockCameraPermissionEnsureable).ensureCameraPermission(
                    onCameraReadyCaptor.capture(),
                    onUserDeniedCameraPermissionCaptor.capture()
                )

                // trigger permission granted
                onCameraReadyCaptor.firstValue()

                assertThat(navController.currentDestination?.id)
                    .isEqualTo(R.id.driverLicenseScanFragment)
            }
        }
    }

    @Test
    @Ignore(
        "Jetpack compose test doesn't work with traditional navigation component in NavHostFragment, " +
            "update this test once all fragments are removed and the activity is implemented with NavHost"
    )
    fun `when modelFile is unavailable and camera permission granted, clicking continue navigates to upload when requireLiveCapture is false without selfie`() {
        launchDocSelectionFragment { navController, _ ->
            runBlocking {
                whenever(verificationPage.documentSelect).thenReturn(
                    DOC_SELECT_SINGLE_CHOICE_DL
                )
                val mockDocumentCapture =
                    mock<VerificationPageStaticContentDocumentCapturePage>().also {
                        whenever(it.requireLiveCapture).thenReturn(false)
                    }
                whenever(verificationPage.documentCapture).thenReturn(
                    mockDocumentCapture
                )
                whenever(mockIdentityViewModel.postVerificationPageData(any(), any())).thenReturn(
                    MISSING_BACK_VERIFICATION_PAGE_DATA
                )
                // mock file is not available
                whenever(mockIdentityViewModel.idDetectorModelFile).thenReturn(
                    MutableLiveData(Resource.error())
                )
                setUpSuccessVerificationPage()

                // trigger navigation
//                binding.singleSelectionContinue.findViewById<MaterialButton>(R.id.button)
//                    .callOnClick()

                verify(mockScreenTracker).screenTransitionStart(eq(SCREEN_NAME_DOC_SELECT), any())

                verify(mockIdentityViewModel).postVerificationPageData(
                    eq(
                        CollectedDataParam(idDocumentType = CollectedDataParam.Type.DRIVINGLICENSE)
                    ),
                    eq(DOC_SELECT_TO_UPLOAD)
                )

                verify(mockCameraPermissionEnsureable).ensureCameraPermission(
                    onCameraReadyCaptor.capture(),
                    onUserDeniedCameraPermissionCaptor.capture()
                )

                // trigger permission granted
                onCameraReadyCaptor.firstValue()

                verify(mockIdentityViewModel).sendAnalyticsRequest(
                    argThat {
                        eventName == EVENT_CAMERA_PERMISSION_GRANTED
                    }
                )

                setUpSuccessVerificationPage(2)

                assertThat(navController.currentDestination?.id)
                    .isEqualTo(R.id.driverLicenseUploadFragment)
            }
        }
    }

    @Test
    @Ignore(
        "Jetpack compose test doesn't work with traditional navigation component in NavHostFragment, " +
            "update this test once all fragments are removed and the activity is implemented with NavHost"
    )
    fun `when modelFile is unavailable and camera permission granted, clicking continue navigates to upload when requireLiveCapture is false with selfie`() {
        launchDocSelectionFragment { navController, _ ->
            runBlocking {
                whenever(verificationPage.documentSelect).thenReturn(
                    DOC_SELECT_SINGLE_CHOICE_DL
                )
                whenever(verificationPage.selfieCapture).thenReturn(mock())
                val mockDocumentCapture =
                    mock<VerificationPageStaticContentDocumentCapturePage>().also {
                        whenever(it.requireLiveCapture).thenReturn(false)
                    }
                whenever(verificationPage.documentCapture).thenReturn(
                    mockDocumentCapture
                )
                whenever(mockIdentityViewModel.postVerificationPageData(any(), any())).thenReturn(
                    MISSING_BACK_VERIFICATION_PAGE_DATA
                )
                // mock file is not available
                whenever(mockIdentityViewModel.idDetectorModelFile).thenReturn(
                    MutableLiveData(Resource.error())
                )
                setUpSuccessVerificationPage()

                // trigger navigation
//                binding.singleSelectionContinue.findViewById<MaterialButton>(R.id.button)
//                    .callOnClick()

                verify(mockScreenTracker).screenTransitionStart(eq(SCREEN_NAME_DOC_SELECT), any())

                verify(mockIdentityViewModel).postVerificationPageData(
                    eq(
                        CollectedDataParam(idDocumentType = CollectedDataParam.Type.DRIVINGLICENSE)
                    ),
                    eq(DOC_SELECT_TO_UPLOAD_WITH_SELFIE)
                )

                verify(mockCameraPermissionEnsureable).ensureCameraPermission(
                    onCameraReadyCaptor.capture(),
                    onUserDeniedCameraPermissionCaptor.capture()
                )

                // trigger permission granted
                onCameraReadyCaptor.firstValue()

                verify(mockIdentityViewModel).sendAnalyticsRequest(
                    argThat {
                        eventName == EVENT_CAMERA_PERMISSION_GRANTED
                    }
                )

                setUpSuccessVerificationPage(2)

                assertThat(navController.currentDestination?.id)
                    .isEqualTo(R.id.driverLicenseUploadFragment)
            }
        }
    }

    @Test
    @Ignore(
        "Jetpack compose test doesn't work with traditional navigation component in NavHostFragment, " +
            "update this test once all fragments are removed and the activity is implemented with NavHost"
    )
    fun `when modelFile is unavailable and camera permission granted, clicking continue navigates to error when requireLiveCapture is true`() {
        launchDocSelectionFragment { navController, _ ->
            whenever(verificationPage.documentSelect).thenReturn(
                DOC_SELECT_SINGLE_CHOICE_DL
            )
            val mockDocumentCapture =
                mock<VerificationPageStaticContentDocumentCapturePage>().also {
                    whenever(it.requireLiveCapture).thenReturn(true)
                }
            whenever(verificationPage.documentCapture).thenReturn(
                mockDocumentCapture
            )
            runBlocking {
                whenever(mockIdentityViewModel.postVerificationPageData(any(), any())).thenReturn(
                    MISSING_BACK_VERIFICATION_PAGE_DATA
                )
            }
            // mock scan is not available
            whenever(mockIdentityViewModel.idDetectorModelFile).thenReturn(
                MutableLiveData(Resource.error())
            )
            setUpSuccessVerificationPage()

            // trigger navigation
//            binding.singleSelectionContinue.findViewById<MaterialButton>(R.id.button).callOnClick()

            verify(mockCameraPermissionEnsureable).ensureCameraPermission(
                onCameraReadyCaptor.capture(),
                onUserDeniedCameraPermissionCaptor.capture()
            )

            // trigger permission granted
            onCameraReadyCaptor.firstValue()

            verify(mockIdentityViewModel).sendAnalyticsRequest(
                argThat {
                    eventName == EVENT_CAMERA_PERMISSION_GRANTED
                }
            )

            setUpSuccessVerificationPage(2)

            assertThat(navController.currentDestination?.id)
                .isEqualTo(R.id.errorFragment)
        }
    }

    @Test
    @Ignore(
        "Jetpack compose test doesn't work with traditional navigation component in NavHostFragment, " +
            "update this test once all fragments are removed and the activity is implemented with NavHost"
    )
    fun `when camera permission is denied, clicking continue navigates to CameraPermissionDeniedFragment when requireLiveCapture is false`() {
        launchDocSelectionFragment { navController, _ ->
            whenever(verificationPage.documentSelect).thenReturn(
                DOC_SELECT_SINGLE_CHOICE_DL
            )
            val mockDocumentCapture =
                mock<VerificationPageStaticContentDocumentCapturePage>().also {
                    whenever(it.requireLiveCapture).thenReturn(false)
                }
            whenever(verificationPage.documentCapture).thenReturn(
                mockDocumentCapture
            )
            runBlocking {
                whenever(mockIdentityViewModel.postVerificationPageData(any(), any())).thenReturn(
                    MISSING_BACK_VERIFICATION_PAGE_DATA
                )
            }
            // mock file is available
            whenever(mockIdentityViewModel.idDetectorModelFile).thenReturn(
                MutableLiveData(Resource.success(mock()))
            )
            setUpSuccessVerificationPage()

            // trigger navigation
//            binding.singleSelectionContinue.findViewById<MaterialButton>(R.id.button).callOnClick()

            verify(mockCameraPermissionEnsureable).ensureCameraPermission(
                onCameraReadyCaptor.capture(),
                onUserDeniedCameraPermissionCaptor.capture()
            )

            // trigger permission denied
            onUserDeniedCameraPermissionCaptor.firstValue()

            verify(mockIdentityViewModel).sendAnalyticsRequest(
                argThat {
                    eventName == EVENT_CAMERA_PERMISSION_DENIED
                }
            )

            setUpSuccessVerificationPage(2)

            assertThat(
                requireNotNull(navController.backStack.last().arguments)
                    [ARG_SCAN_TYPE]
            ).isEqualTo(CollectedDataParam.Type.DRIVINGLICENSE)

            assertThat(navController.currentDestination?.id)
                .isEqualTo(R.id.cameraPermissionDeniedFragment)
        }
    }

    @Test
    @Ignore(
        "Jetpack compose test doesn't work with traditional navigation component in NavHostFragment, " +
            "update this test once all fragments are removed and the activity is implemented with NavHost"
    )
    fun `when camera permission is denied, clicking continue navigates to CameraPermissionDeniedFragment when requireLiveCapture is true`() {
        launchDocSelectionFragment { navController, _ ->
            whenever(verificationPage.documentSelect).thenReturn(
                DOC_SELECT_SINGLE_CHOICE_DL
            )
            val mockDocumentCapture =
                mock<VerificationPageStaticContentDocumentCapturePage>().also {
                    whenever(it.requireLiveCapture).thenReturn(true)
                }
            whenever(verificationPage.documentCapture).thenReturn(
                mockDocumentCapture
            )
            runBlocking {
                whenever(mockIdentityViewModel.postVerificationPageData(any(), any())).thenReturn(
                    MISSING_BACK_VERIFICATION_PAGE_DATA
                )
            }
            // mock file is available
            whenever(mockIdentityViewModel.idDetectorModelFile).thenReturn(
                MutableLiveData(Resource.success(mock()))
            )
            setUpSuccessVerificationPage()

            // trigger navigation
//            binding.singleSelectionContinue.findViewById<MaterialButton>(R.id.button).callOnClick()

            verify(mockCameraPermissionEnsureable).ensureCameraPermission(
                onCameraReadyCaptor.capture(),
                onUserDeniedCameraPermissionCaptor.capture()
            )

            // trigger permission denied
            onUserDeniedCameraPermissionCaptor.firstValue()

            verify(mockIdentityViewModel).sendAnalyticsRequest(
                argThat {
                    eventName == EVENT_CAMERA_PERMISSION_DENIED
                }
            )

            setUpSuccessVerificationPage(2)

            assertThat(navController.currentDestination?.id)
                .isEqualTo(R.id.cameraPermissionDeniedFragment)

            assertThat(requireNotNull(navController.backStack.last().arguments).isEmpty).isTrue()
        }
    }

    private fun launchDocSelectionFragment(
        testBlock: (
            navController: TestNavHostController,
            docSelectionFragment: DocSelectionFragment
        ) -> Unit
    ) = launchFragmentInContainer(
        themeResId = R.style.Theme_MaterialComponents
    ) {
        DocSelectionFragment(
            viewModelFactoryFor(mockIdentityViewModel),
            mockCameraPermissionEnsureable
        )
    }.onFragment {
        val navController = TestNavHostController(
            ApplicationProvider.getApplicationContext()
        )
        navController.setGraph(
            R.navigation.identity_nav_graph
        )
        navController.setCurrentDestination(R.id.docSelectionFragment)
        Navigation.setViewNavController(
            it.requireView(),
            navController
        )
        runBlocking {
            verify(mockScreenTracker).screenTransitionFinish(eq(SCREEN_NAME_DOC_SELECT))
        }
        testBlock(navController, it)
    }

    private companion object {
        const val DOCUMENT_SELECT_TITLE = "title"
        const val DOCUMENT_SELECT_BUTTON_TEXT = "button text"
        const val PASSPORT_BUTTON_TEXT = "Passport"
        const val ID_BUTTON_TEXT = "ID"
        const val DRIVING_LICENSE_BUTTON_TEXT = "Driver's license"
        const val PASSPORT_BODY_TEXT = "Passport body"
        const val ID_BODY_TEXT = "ID body"
        const val DRIVING_LICENSE_BODY_TEXT = "Driver's license body"
        const val PASSPORT_SINGLE_BODY_TEXT = "Passport single selection body"
        const val ID_SINGLE_BODY_TEXT = "ID single selection body"
        const val DRIVING_LICENSE_SINGLE_BODY_TEXT = "Driver's license single selection body"

        val DOC_SELECT_MULTI_CHOICE = VerificationPageStaticContentDocumentSelectPage(
            title = DOCUMENT_SELECT_TITLE,
            idDocumentTypeAllowlist = mapOf(
                PASSPORT_KEY to PASSPORT_BUTTON_TEXT,
                ID_CARD_KEY to ID_BUTTON_TEXT,
                DRIVING_LICENSE_KEY to DRIVING_LICENSE_BUTTON_TEXT
            ),
            buttonText = DOCUMENT_SELECT_BUTTON_TEXT,
            body = null
        )

        val DOC_SELECT_SINGLE_CHOICE_PASSPORT = VerificationPageStaticContentDocumentSelectPage(
            title = DOCUMENT_SELECT_TITLE,
            idDocumentTypeAllowlist = mapOf(
                PASSPORT_KEY to PASSPORT_BODY_TEXT
            ),
            buttonText = DOCUMENT_SELECT_BUTTON_TEXT,
            body = PASSPORT_SINGLE_BODY_TEXT
        )

        val DOC_SELECT_SINGLE_CHOICE_ID = VerificationPageStaticContentDocumentSelectPage(
            title = DOCUMENT_SELECT_TITLE,
            idDocumentTypeAllowlist = mapOf(
                ID_CARD_KEY to ID_BODY_TEXT
            ),
            buttonText = DOCUMENT_SELECT_BUTTON_TEXT,
            body = ID_SINGLE_BODY_TEXT
        )

        val DOC_SELECT_SINGLE_CHOICE_DL = VerificationPageStaticContentDocumentSelectPage(
            title = DOCUMENT_SELECT_TITLE,
            idDocumentTypeAllowlist = mapOf(
                DRIVING_LICENSE_KEY to DRIVING_LICENSE_BODY_TEXT
            ),
            buttonText = DOCUMENT_SELECT_BUTTON_TEXT,
            body = DRIVING_LICENSE_SINGLE_BODY_TEXT
        )

        val DOC_SELECT_ZERO_CHOICE = VerificationPageStaticContentDocumentSelectPage(
            title = DOCUMENT_SELECT_TITLE,
            idDocumentTypeAllowlist = emptyMap(),
            buttonText = DOCUMENT_SELECT_BUTTON_TEXT,
            body = null
        )

        val MISSING_BACK_VERIFICATION_PAGE_DATA = VerificationPageData(
            id = "id",
            objectType = "type",
            requirements = VerificationPageDataRequirements(
                errors = emptyList(),
                missings = listOf(VerificationPageRequirements.Missing.IDDOCUMENTBACK)
            ),
            status = VerificationPageData.Status.VERIFIED,
            submitted = false
        )
    }
}
