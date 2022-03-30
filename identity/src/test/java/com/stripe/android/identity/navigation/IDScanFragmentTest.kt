package com.stripe.android.identity.navigation

import android.content.Context
import android.view.View
import android.widget.Button
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.core.os.bundleOf
import androidx.fragment.app.testing.launchFragmentInContainer
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.Observer
import androidx.lifecycle.lifecycleScope
import androidx.navigation.Navigation
import androidx.navigation.testing.TestNavHostController
import androidx.test.core.app.ApplicationProvider
import com.google.android.material.button.MaterialButton
import com.google.android.material.progressindicator.CircularProgressIndicator
import com.google.common.truth.Truth.assertThat
import com.stripe.android.core.model.InternalStripeFile
import com.stripe.android.identity.R
import com.stripe.android.identity.SUCCESS_VERIFICATION_PAGE
import com.stripe.android.identity.camera.IDDetectorAggregator
import com.stripe.android.identity.camera.IdentityScanFlow
import com.stripe.android.identity.databinding.IdentityCameraScanFragmentBinding
import com.stripe.android.identity.navigation.IdentityCameraScanFragment.Companion.ARG_SHOULD_START_FROM_BACK
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
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.robolectric.RobolectricTestRunner
import java.io.File

@RunWith(RobolectricTestRunner::class)
internal class IDScanFragmentTest {
    @get:Rule
    var rule: TestRule = InstantTaskExecutorRule()

    private val finalResultLiveData = SingleLiveEvent<IDDetectorAggregator.FinalResult>()
    private val displayStateChanged = SingleLiveEvent<Pair<IdentityScanState, IdentityScanState?>>()
    private val mockBothUploaded: PairMediatorLiveData<Pair<UploadedResult, UploadedResult>> =
        mock()
    private val bothUploadedObserverCaptor =
        argumentCaptor<Observer<Resource<Pair<
                        Pair<UploadedResult, UploadedResult>, Pair<UploadedResult, UploadedResult>
                        >>>>()

    private val mockScanFlow = mock<IdentityScanFlow>()
    private val mockIdentityScanViewModel = mock<IdentityScanViewModel>().also {
        whenever(it.identityScanFlow).thenReturn(mockScanFlow)
        whenever(it.finalResult).thenReturn(finalResultLiveData)
        whenever(it.displayStateChanged).thenReturn(displayStateChanged)
    }

    private val mockPageAndModel = MediatorLiveData<Resource<Pair<VerificationPage, File>>>()
    private val mockIdentityViewModel = mock<IdentityViewModel>().also {
        whenever(it.pageAndModel).thenReturn(mockPageAndModel)
        whenever(it.bothUploaded).thenReturn(mockBothUploaded)
    }

    @Before
    fun simulateModelDownloaded() {
        mockPageAndModel.postValue(Resource.success(Pair(SUCCESS_VERIFICATION_PAGE, mock())))
    }

    @Test
    fun `when front is scanned clicking button triggers back scan`() {
        launchIDScanFragment().onFragment { idScanFragment ->
            // verify start to scan front
            assertThat(idScanFragment.cameraAdapter.isBoundToLifecycle()).isTrue()
            verify(mockScanFlow).startFlow(
                same(idScanFragment.requireContext()),
                any(),
                any(),
                same(idScanFragment.viewLifecycleOwner),
                same(idScanFragment.lifecycleScope),
                eq(IdentityScanState.ScanType.ID_FRONT)
            )

            // mock success of front scan
            val mockFrontFinalResult = mock<IDDetectorAggregator.FinalResult>().also {
                whenever(it.identityState).thenReturn(mock<IdentityScanState.Finished>())
            }
            finalResultLiveData.postValue(mockFrontFinalResult)
            whenever(mockIdentityScanViewModel.targetScanType).thenReturn(IdentityScanState.ScanType.ID_FRONT)
            verifyUploadedWithFinalResult(
                mockFrontFinalResult,
                targetType = IdentityScanState.ScanType.ID_FRONT
            )

            // stopScanning() is called
            verify(mockScanFlow).resetFlow()
            assertThat(idScanFragment.cameraAdapter.isBoundToLifecycle()).isFalse()

            // mock viewModel target change
            whenever(mockIdentityScanViewModel.targetScanType)
                .thenReturn(IdentityScanState.ScanType.ID_FRONT)

            // button clicked
            IdentityCameraScanFragmentBinding.bind(idScanFragment.requireView()).kontinue
                .findViewById<Button>(R.id.button).callOnClick()

            // verify start to scan back
            assertThat(idScanFragment.cameraAdapter.isBoundToLifecycle()).isTrue()
            verify(mockScanFlow).startFlow(
                same(idScanFragment.requireContext()),
                any(),
                any(),
                same(idScanFragment.viewLifecycleOwner),
                same(idScanFragment.lifecycleScope),
                eq(IdentityScanState.ScanType.ID_BACK)
            )
        }
    }

    @Test
    fun `when started with startFromBack with true, scanning with ID_BACK`() {
        launchIDScanFragment(shouldStartFromBack = true).onFragment { idScanFragment ->
            // verify start to scan back
            assertThat(idScanFragment.cameraAdapter.isBoundToLifecycle()).isTrue()
            verify(mockScanFlow).startFlow(
                same(idScanFragment.requireContext()),
                any(),
                any(),
                same(idScanFragment.viewLifecycleOwner),
                same(idScanFragment.lifecycleScope),
                eq(IdentityScanState.ScanType.ID_BACK)
            )
        }
    }

    @Test
    fun `when both sides are scanned and files uploaded succeeded, clicking button triggers navigation`() {
        simulateBothSidesScanned { _, _ ->
            runBlocking {
                // mock bothUploaded success
                bothUploadedObserverCaptor.firstValue.onChanged(
                    Resource.success(
                        Pair(
                            Pair(FRONT_HIGH_RES_RESULT, FRONT_LOW_RES_RESULT),
                            Pair(BACK_HIGH_RES_RESULT, BACK_LOW_RES_RESULT),
                        )
                    )
                )

                // verify navigation attempts
                verify(mockIdentityViewModel).postVerificationPageData(
                    eq(
                        CollectedDataParam.createFromUploadedResultsForAutoCapture(
                            type = IdDocumentParam.Type.IDCARD,
                            frontHighResResult = FRONT_HIGH_RES_RESULT,
                            frontLowResResult = FRONT_LOW_RES_RESULT,
                            backHighResResult = BACK_HIGH_RES_RESULT,
                            backLowResResult = BACK_LOW_RES_RESULT,
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
    fun `when both sides are scanned but files uploaded failed, clicking button navigate to error`() {
        simulateBothSidesScanned { navController, _ ->
            // mock bothUploaded error
            bothUploadedObserverCaptor.firstValue.onChanged(
                Resource.error()
            )

            assertThat(navController.currentDestination?.id)
                .isEqualTo(R.id.errorFragment)
        }
    }

    @Test
    fun `when both sides are scanned and files are being uploaded, clicking button toggles loading state`() {
        simulateBothSidesScanned { _, binding ->
            // mock bothUploaded loading
            bothUploadedObserverCaptor.firstValue.onChanged(
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
        launchIDScanFragment().onFragment { idScanFragment ->
            assertThat(idScanFragment.cameraAdapter.isBoundToLifecycle()).isTrue()

            finalResultLiveData.postValue(
                mock<IDDetectorAggregator.FinalResult>().also {
                    whenever(it.identityState).thenReturn(mock<IdentityScanState.Finished>())
                }
            )

            verify(mockScanFlow).resetFlow()
            assertThat(idScanFragment.cameraAdapter.isBoundToLifecycle()).isFalse()
        }
    }

    @Test
    fun `when displayStateChanged to Initial UI is properly updated for ID_FRONT`() {
        whenever(mockIdentityScanViewModel.targetScanType).thenReturn(IdentityScanState.ScanType.ID_FRONT)
        postDisplayStateChangedDataAndVerifyUI(mock<IdentityScanState.Initial>()) { binding, context ->
            assertThat(binding.cameraView.viewFinderBackgroundView.visibility).isEqualTo(View.VISIBLE)
            assertThat(binding.cameraView.viewFinderWindowView.visibility).isEqualTo(View.VISIBLE)
            assertThat(binding.cameraView.viewFinderBorderView.visibility).isEqualTo(View.VISIBLE)
            assertThat(binding.checkMarkView.visibility).isEqualTo(View.GONE)
            assertThat(binding.kontinue.isEnabled).isFalse()
            assertThat(binding.headerTitle.text).isEqualTo(
                context.getText(R.string.front_of_id)
            )
            assertThat(binding.message.text).isEqualTo(
                context.getText(R.string.position_id_front)
            )
        }
    }

    @Test
    fun `when displayStateChanged to Initial UI is properly updated for ID_BACK`() {
        whenever(mockIdentityScanViewModel.targetScanType).thenReturn(IdentityScanState.ScanType.ID_BACK)
        postDisplayStateChangedDataAndVerifyUI(
            mock<IdentityScanState.Initial>(),
            shouldStartFromBack = true
        ) { binding, context ->
            assertThat(binding.cameraView.viewFinderBackgroundView.visibility).isEqualTo(View.VISIBLE)
            assertThat(binding.cameraView.viewFinderWindowView.visibility).isEqualTo(View.VISIBLE)
            assertThat(binding.cameraView.viewFinderBorderView.visibility).isEqualTo(View.VISIBLE)
            assertThat(binding.checkMarkView.visibility).isEqualTo(View.GONE)
            assertThat(binding.kontinue.isEnabled).isFalse()
            assertThat(binding.headerTitle.text).isEqualTo(
                context.getText(R.string.back_of_id)
            )
            assertThat(binding.message.text).isEqualTo(
                context.getText(R.string.position_id_back)
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
    fun `when displayStateChanged to Unsatisfied UI is properly updated for ID_FRONT`() {
        whenever(mockIdentityScanViewModel.targetScanType).thenReturn(IdentityScanState.ScanType.ID_FRONT)
        postDisplayStateChangedDataAndVerifyUI(mock<IdentityScanState.Unsatisfied>()) { binding, context ->
            assertThat(binding.cameraView.viewFinderBackgroundView.visibility).isEqualTo(View.VISIBLE)
            assertThat(binding.cameraView.viewFinderWindowView.visibility).isEqualTo(View.VISIBLE)
            assertThat(binding.cameraView.viewFinderBorderView.visibility).isEqualTo(View.VISIBLE)
            assertThat(binding.checkMarkView.visibility).isEqualTo(View.GONE)
            assertThat(binding.kontinue.isEnabled).isFalse()
            assertThat(binding.message.text).isEqualTo(
                context.getText(R.string.position_id_front)
            )
        }
    }

    @Test
    fun `when displayStateChanged to Unsatisfied UI is properly updated for ID_BACK`() {
        whenever(mockIdentityScanViewModel.targetScanType).thenReturn(IdentityScanState.ScanType.ID_BACK)
        postDisplayStateChangedDataAndVerifyUI(mock<IdentityScanState.Unsatisfied>()) { binding, context ->
            assertThat(binding.cameraView.viewFinderBackgroundView.visibility).isEqualTo(View.VISIBLE)
            assertThat(binding.cameraView.viewFinderWindowView.visibility).isEqualTo(View.VISIBLE)
            assertThat(binding.cameraView.viewFinderBorderView.visibility).isEqualTo(View.VISIBLE)
            assertThat(binding.checkMarkView.visibility).isEqualTo(View.GONE)
            assertThat(binding.kontinue.isEnabled).isFalse()
            assertThat(binding.message.text).isEqualTo(
                context.getText(R.string.position_id_back)
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

    private fun verifyUploadedWithFinalResult(
        finalResult: IDDetectorAggregator.FinalResult,
        time: Int = 1,
        targetType: IdentityScanState.ScanType
    ) {
        val successCaptor = argumentCaptor<(VerificationPage) -> Unit>()
        verify(mockIdentityViewModel, times(time)).observeForVerificationPage(
            any(),
            successCaptor.capture(),
            any()
        )

        val mockDocumentCapturePage = mock<VerificationPageStaticContentDocumentCapturePage>()
        val mockVerificationPage = mock<VerificationPage>().also { verificationPage ->
            whenever(verificationPage.documentCapture).thenReturn(mockDocumentCapturePage)
        }
        successCaptor.lastValue.invoke(mockVerificationPage)
        verify(mockIdentityViewModel).uploadScanResult(
            same(finalResult),
            same(mockDocumentCapturePage),
            eq(targetType)
        )
    }

    private fun simulateBothSidesScanned(afterScannedBlock: (TestNavHostController, IdentityCameraScanFragmentBinding) -> Unit) {
        launchIDScanFragment().onFragment { idScanFragment ->
            val navController = TestNavHostController(
                ApplicationProvider.getApplicationContext()
            )
            navController.setGraph(
                R.navigation.identity_nav_graph
            )
            navController.setCurrentDestination(R.id.IDScanFragment)
            Navigation.setViewNavController(
                idScanFragment.requireView(),
                navController
            )
            // scan front
            // mock success of front scan
            val mockFrontFinalResult = mock<IDDetectorAggregator.FinalResult>().also {
                whenever(it.identityState).thenReturn(mock<IdentityScanState.Finished>())
            }
            // mock viewModel target change
            whenever(mockIdentityScanViewModel.targetScanType)
                .thenReturn(IdentityScanState.ScanType.ID_FRONT)
            finalResultLiveData.postValue(mockFrontFinalResult)
            verifyUploadedWithFinalResult(
                mockFrontFinalResult,
                targetType = IdentityScanState.ScanType.ID_FRONT
            )

            // click continue, scan back
            val binding = IdentityCameraScanFragmentBinding.bind(idScanFragment.requireView())
            binding.kontinue.findViewById<Button>(R.id.button).callOnClick()

            // mock success of back scan
            val mockBackFinalResult = mock<IDDetectorAggregator.FinalResult>().also {
                whenever(it.identityState).thenReturn(mock<IdentityScanState.Finished>())
            }
            // mock viewModel target change
            whenever(mockIdentityScanViewModel.targetScanType)
                .thenReturn(IdentityScanState.ScanType.ID_BACK)
            finalResultLiveData.postValue(mockBackFinalResult)
            verifyUploadedWithFinalResult(
                mockBackFinalResult,
                2,
                targetType = IdentityScanState.ScanType.ID_BACK
            )

            // click continue, navigates
            binding.kontinue.findViewById<Button>(R.id.button).callOnClick()

            verify(mockBothUploaded).observe(any(), bothUploadedObserverCaptor.capture())

            afterScannedBlock(navController, binding)
        }
    }

    private fun launchIDScanFragment(shouldStartFromBack: Boolean = false) =
        launchFragmentInContainer(
            bundleOf(ARG_SHOULD_START_FROM_BACK to shouldStartFromBack),
            themeResId = R.style.Theme_MaterialComponents
        ) {
            IDScanFragment(
                viewModelFactoryFor(mockIdentityScanViewModel),
                viewModelFactoryFor(mockIdentityViewModel)
            )
        }

    private fun postDisplayStateChangedDataAndVerifyUI(
        newScanState: IdentityScanState,
        shouldStartFromBack: Boolean = false,
        check: (binding: IdentityCameraScanFragmentBinding, context: Context) -> Unit
    ) {
        launchIDScanFragment(shouldStartFromBack).onFragment {
            displayStateChanged.postValue((newScanState to mock()))
            check(IdentityCameraScanFragmentBinding.bind(it.requireView()), it.requireContext())
        }
    }

    private companion object {
        val FRONT_HIGH_RES_RESULT = UploadedResult(
            uploadedStripeFile = InternalStripeFile(
                id = "frontHighResResult"
            ),
            scores = listOf(0.1f, 0.2f, 0.3f, 0.4f, 0.5f),
            uploadMethod = DocumentUploadParam.UploadMethod.AUTOCAPTURE
        )
        val FRONT_LOW_RES_RESULT = UploadedResult(
            uploadedStripeFile = InternalStripeFile(
                id = "frontLowResResult"
            ),
            scores = listOf(0.1f, 0.2f, 0.3f, 0.4f, 0.5f),
            uploadMethod = DocumentUploadParam.UploadMethod.AUTOCAPTURE
        )
        val BACK_HIGH_RES_RESULT = UploadedResult(
            uploadedStripeFile = InternalStripeFile(
                id = "backHighResResult"
            ),
            scores = listOf(0.1f, 0.2f, 0.3f, 0.4f, 0.5f),
            uploadMethod = DocumentUploadParam.UploadMethod.AUTOCAPTURE
        )
        val BACK_LOW_RES_RESULT = UploadedResult(
            uploadedStripeFile = InternalStripeFile(
                id = "frontHighResResult"
            ),
            scores = listOf(0.1f, 0.2f, 0.3f, 0.4f, 0.5f),
            uploadMethod = DocumentUploadParam.UploadMethod.AUTOCAPTURE
        )
    }
}
