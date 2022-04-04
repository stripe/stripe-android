package com.stripe.android.identity.navigation

import android.content.Context
import android.view.View
import android.widget.Button
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.fragment.app.testing.launchFragmentInContainer
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.Observer
import androidx.navigation.Navigation
import androidx.navigation.testing.TestNavHostController
import androidx.test.core.app.ApplicationProvider
import com.google.android.material.button.MaterialButton
import com.google.android.material.progressindicator.CircularProgressIndicator
import com.google.common.truth.Truth.assertThat
import com.stripe.android.core.model.StripeFile
import com.stripe.android.identity.R
import com.stripe.android.identity.SUCCESS_VERIFICATION_PAGE
import com.stripe.android.identity.camera.IDDetectorAggregator
import com.stripe.android.identity.camera.IdentityScanFlow
import com.stripe.android.identity.databinding.IdentityCameraScanFragmentBinding
import com.stripe.android.identity.networking.Resource
import com.stripe.android.identity.networking.models.ClearDataParam
import com.stripe.android.identity.networking.models.CollectedDataParam
import com.stripe.android.identity.networking.models.DocumentUploadParam
import com.stripe.android.identity.networking.models.IdDocumentParam
import com.stripe.android.identity.networking.models.VerificationPage
import com.stripe.android.identity.networking.models.VerificationPageStaticContentDocumentCapturePage
import com.stripe.android.identity.states.IdentityScanState
import com.stripe.android.identity.utils.PairMediatorLiveData
import com.stripe.android.identity.utils.SingleLiveEvent
import com.stripe.android.identity.viewModelFactoryFor
import com.stripe.android.identity.viewmodel.IdentityScanViewModel
import com.stripe.android.identity.viewmodel.IdentityViewModel
import com.stripe.android.identity.viewmodel.IdentityViewModel.UploadedResult
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TestRule
import org.junit.runner.RunWith
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.same
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.robolectric.RobolectricTestRunner
import java.io.File

@RunWith(RobolectricTestRunner::class)
class PassportScanFragmentTest {
    @get:Rule
    var rule: TestRule = InstantTaskExecutorRule()

    private val finalResultLiveData = SingleLiveEvent<IDDetectorAggregator.FinalResult>()
    private val displayStateChanged = SingleLiveEvent<Pair<IdentityScanState, IdentityScanState?>>()
    private val mockFrontUploaded: PairMediatorLiveData<UploadedResult> =
        mock()
    private val frontUploadedObserverCaptor =
        argumentCaptor<Observer<Resource<
                    Pair<UploadedResult, UploadedResult>
                    >>>()

    private val mockScanFlow = mock<IdentityScanFlow>()
    private val mockIdentityScanViewModel = mock<IdentityScanViewModel>().also {
        whenever(it.identityScanFlow).thenReturn(mockScanFlow)
        whenever(it.finalResult).thenReturn(finalResultLiveData)
        whenever(it.displayStateChanged).thenReturn(displayStateChanged)
    }

    private val mockPageAndModel = MediatorLiveData<Resource<Pair<VerificationPage, File>>>()
    private val mockIdentityViewModel = mock<IdentityViewModel>().also {
        whenever(it.pageAndModel).thenReturn(mockPageAndModel)
        whenever(it.frontUploaded).thenReturn(mockFrontUploaded)
    }

    @Before
    fun simulateModelDownloaded() {
        mockPageAndModel.postValue(Resource.success(Pair(SUCCESS_VERIFICATION_PAGE, mock())))
    }

    @Test
    fun `when scanned and file is uploaded, clicking button triggers navigation`() {
        simulateFrontScanned { _, _ ->
            runBlocking {
                // mock bothUploaded success
                frontUploadedObserverCaptor.firstValue.onChanged(
                    Resource.success(
                        Pair(
                            FRONT_HIGH_RES_RESULT,
                            FRONT_LOW_RES_RESULT
                        ),
                    )
                )

                // verify navigation attempts
                verify(mockIdentityViewModel).postVerificationPageData(
                    eq(
                        CollectedDataParam.createFromUploadedResultsForAutoCapture(
                            type = IdDocumentParam.Type.PASSPORT,
                            frontHighResResult = FRONT_HIGH_RES_RESULT,
                            frontLowResResult = FRONT_LOW_RES_RESULT
                        )
                    ),
                    eq(
                        ClearDataParam.UPLOAD_TO_CONFIRM
                    )
                )
            }
        }
    }

    @Test
    fun `when scanned but files uploaded failed, clicking button navigate to error`() {
        simulateFrontScanned { navController, _ ->
            // mock bothUploaded error
            frontUploadedObserverCaptor.firstValue.onChanged(
                Resource.error()
            )

            assertThat(navController.currentDestination?.id)
                .isEqualTo(R.id.errorFragment)
        }
    }

    @Test
    fun `when scanned and files are being uploaded, clicking button toggles loading state`() {
        simulateFrontScanned { _, binding ->
            // mock bothUploaded loading
            frontUploadedObserverCaptor.firstValue.onChanged(
                Resource.loading()
            )

            assertThat(
                binding.kontinue.findViewById<MaterialButton>(R.id.button).isEnabled
            ).isFalse()
            assertThat(
                binding.kontinue.findViewById<CircularProgressIndicator>(R.id.indicator).visibility
            ).isEqualTo(
                View.VISIBLE
            )
        }
    }

    @Test
    fun `when final result is received scanFlow is reset and cameraAdapter is unbound`() {
        launchPassportScanFragment().onFragment { passportScanFragment ->
            assertThat(passportScanFragment.cameraAdapter.isBoundToLifecycle()).isTrue()

            finalResultLiveData.postValue(
                mock<IDDetectorAggregator.FinalResult>().also {
                    whenever(it.identityState).thenReturn(mock<IdentityScanState.Finished>())
                }
            )

            verify(mockScanFlow).resetFlow()
            assertThat(passportScanFragment.cameraAdapter.isBoundToLifecycle()).isFalse()
        }
    }

    @Test
    fun `when displayStateChanged to Initial UI is properly updated`() {
        postDisplayStateChangedDataAndVerifyUI(mock<IdentityScanState.Initial>()) { binding, context ->
            assertThat(binding.cameraView.viewFinderBackgroundView.visibility).isEqualTo(View.VISIBLE)
            assertThat(binding.cameraView.viewFinderWindowView.visibility).isEqualTo(View.VISIBLE)
            assertThat(binding.cameraView.viewFinderBorderView.visibility).isEqualTo(View.VISIBLE)
            assertThat(binding.checkMarkView.visibility).isEqualTo(View.GONE)
            assertThat(binding.kontinue.isEnabled).isFalse()
            assertThat(binding.headerTitle.text).isEqualTo(
                context.getText(R.string.passport)
            )
            assertThat(binding.message.text).isEqualTo(
                context.getText(R.string.position_passport)
            )
        }
    }

    @Test
    fun `when displayStateChanged to Found UI is properly updated`() {
        postDisplayStateChangedDataAndVerifyUI(mock<IdentityScanState.Found>()) { binding, context ->
            assertThat(binding.cameraView.viewFinderBackgroundView.visibility).isEqualTo(View.VISIBLE)
            assertThat(binding.cameraView.viewFinderWindowView.visibility).isEqualTo(View.VISIBLE)
            assertThat(binding.cameraView.viewFinderBorderView.visibility).isEqualTo(View.VISIBLE)
            assertThat(binding.checkMarkView.visibility).isEqualTo(View.GONE)
            assertThat(binding.kontinue.isEnabled).isFalse()
            assertThat(binding.message.text).isEqualTo(
                context.getText(R.string.hold_still)
            )
        }
    }

    @Test
    fun `when displayStateChanged to Unsatisfied UI is properly updated`() {
        postDisplayStateChangedDataAndVerifyUI(mock<IdentityScanState.Unsatisfied>()) { binding, context ->
            assertThat(binding.cameraView.viewFinderBackgroundView.visibility).isEqualTo(View.VISIBLE)
            assertThat(binding.cameraView.viewFinderWindowView.visibility).isEqualTo(View.VISIBLE)
            assertThat(binding.cameraView.viewFinderBorderView.visibility).isEqualTo(View.VISIBLE)
            assertThat(binding.checkMarkView.visibility).isEqualTo(View.GONE)
            assertThat(binding.kontinue.isEnabled).isFalse()
            assertThat(binding.message.text).isEqualTo(
                context.getText(R.string.position_passport)
            )
        }
    }

    @Test
    fun `when displayStateChanged to Satisfied UI is properly updated`() {
        postDisplayStateChangedDataAndVerifyUI(mock<IdentityScanState.Satisfied>()) { binding, context ->
            assertThat(binding.cameraView.viewFinderBackgroundView.visibility).isEqualTo(View.VISIBLE)
            assertThat(binding.cameraView.viewFinderWindowView.visibility).isEqualTo(View.VISIBLE)
            assertThat(binding.cameraView.viewFinderBorderView.visibility).isEqualTo(View.VISIBLE)
            assertThat(binding.checkMarkView.visibility).isEqualTo(View.GONE)
            assertThat(binding.kontinue.isEnabled).isFalse()
            assertThat(binding.message.text).isEqualTo(
                context.getText(R.string.scanned)
            )
        }
    }

    @Test
    fun `when displayStateChanged to Finished UI is properly updated`() {
        postDisplayStateChangedDataAndVerifyUI(mock<IdentityScanState.Finished>()) { binding, context ->
            assertThat(binding.cameraView.viewFinderBackgroundView.visibility).isEqualTo(View.INVISIBLE)
            assertThat(binding.cameraView.viewFinderWindowView.visibility).isEqualTo(View.INVISIBLE)
            assertThat(binding.cameraView.viewFinderBorderView.visibility).isEqualTo(View.INVISIBLE)
            assertThat(binding.checkMarkView.visibility).isEqualTo(View.VISIBLE)
            assertThat(binding.kontinue.isEnabled).isTrue()
            assertThat(binding.message.text).isEqualTo(
                context.getText(R.string.scanned)
            )
        }
    }

    private fun simulateFrontScanned(afterScannedBlock: (TestNavHostController, IdentityCameraScanFragmentBinding) -> Unit) {
        launchPassportScanFragment().onFragment { passportScanFragment ->
            val navController = TestNavHostController(
                ApplicationProvider.getApplicationContext()
            )
            navController.setGraph(
                R.navigation.identity_nav_graph
            )
            navController.setCurrentDestination(R.id.passportScanFragment)
            Navigation.setViewNavController(
                passportScanFragment.requireView(),
                navController
            )
            // start scan
            // mock success of scan
            val mockFrontFinalResult = mock<IDDetectorAggregator.FinalResult>().also {
                whenever(it.identityState).thenReturn(mock<IdentityScanState.Finished>())
            }
            finalResultLiveData.postValue(mockFrontFinalResult)

            val successCaptor = argumentCaptor<(VerificationPage) -> Unit>()
            verify(mockIdentityViewModel).observeForVerificationPage(
                any(),
                successCaptor.capture(),
                any()
            )

            val mockDocumentCapturePage = mock<VerificationPageStaticContentDocumentCapturePage>()
            val mockVerificationPage = mock<VerificationPage>().also { verificationPage ->
                whenever(verificationPage.documentCapture).thenReturn(mockDocumentCapturePage)
            }
            whenever(mockIdentityScanViewModel.targetScanType).thenReturn(IdentityScanState.ScanType.PASSPORT)
            successCaptor.lastValue.invoke(mockVerificationPage)
            verify(mockIdentityViewModel).uploadScanResult(
                same(mockFrontFinalResult),
                same(mockDocumentCapturePage),
                eq(IdentityScanState.ScanType.PASSPORT)
            )

            // click continue, trigger navigation
            val binding = IdentityCameraScanFragmentBinding.bind(passportScanFragment.requireView())
            binding.kontinue.findViewById<Button>(R.id.button).callOnClick()

            verify(mockFrontUploaded).observe(any(), frontUploadedObserverCaptor.capture())

            afterScannedBlock(navController, binding)
        }
    }

    private fun launchPassportScanFragment() = launchFragmentInContainer(
        themeResId = R.style.Theme_MaterialComponents
    ) {
        PassportScanFragment(
            viewModelFactoryFor(mockIdentityScanViewModel),
            viewModelFactoryFor(mockIdentityViewModel)
        )
    }

    private fun postDisplayStateChangedDataAndVerifyUI(
        newScanState: IdentityScanState,
        check: (binding: IdentityCameraScanFragmentBinding, context: Context) -> Unit
    ) {
        launchPassportScanFragment().onFragment {
            displayStateChanged.postValue((newScanState to mock()))
            check(IdentityCameraScanFragmentBinding.bind(it.requireView()), it.requireContext())
        }
    }

    private companion object {
        val FRONT_HIGH_RES_RESULT = UploadedResult(
            uploadedStripeFile = StripeFile(
                id = "frontHighResResult"
            ),
            scores = listOf(0.1f, 0.2f, 0.3f, 0.4f, 0.5f),
            uploadMethod = DocumentUploadParam.UploadMethod.AUTOCAPTURE
        )
        val FRONT_LOW_RES_RESULT = UploadedResult(
            uploadedStripeFile = StripeFile(
                id = "frontLowResResult"
            ),
            scores = listOf(0.1f, 0.2f, 0.3f, 0.4f, 0.5f),
            uploadMethod = DocumentUploadParam.UploadMethod.AUTOCAPTURE
        )
    }
}
