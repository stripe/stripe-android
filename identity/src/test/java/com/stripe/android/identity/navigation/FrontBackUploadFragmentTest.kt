package com.stripe.android.identity.navigation

import android.net.Uri
import android.view.View
import android.widget.Button
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.fragment.app.testing.launchFragmentInContainer
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.Navigation
import androidx.navigation.testing.TestNavHostController
import androidx.test.core.app.ApplicationProvider
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.common.truth.Truth.assertThat
import com.stripe.android.identity.R
import com.stripe.android.identity.databinding.FrontBackUploadFragmentBinding
import com.stripe.android.identity.states.IdentityScanState
import com.stripe.android.identity.viewmodel.FrontBackUploadViewModel
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TestRule
import org.junit.runner.RunWith
import org.mockito.kotlin.KArgumentCaptor
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.mock
import org.mockito.kotlin.same
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.robolectric.RobolectricTestRunner
import org.robolectric.shadows.ShadowDialog

@RunWith(RobolectricTestRunner::class)
class FrontBackUploadFragmentTest {
    @get:Rule
    var rule: TestRule = InstantTaskExecutorRule()

    private val frontUploaded = MutableLiveData<Unit>()
    private val backUploaded = MutableLiveData<Unit>()
    private val uploadFinished = MediatorLiveData<Unit>()
    private val mockUri = mock<Uri>()

    private val mockFrontBackUploadViewModel = mock<FrontBackUploadViewModel>().also {
        whenever(it.frontUploaded).thenReturn(frontUploaded)
        whenever(it.backUploaded).thenReturn(backUploaded)
        whenever(it.uploadFinished).thenReturn(uploadFinished)
    }

    private val idUploadFragmentViewModelFactory = object : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return mockFrontBackUploadViewModel as T
        }
    }

    @Test
    fun `when initialized viewmodel registers activityResultCaller and UI is correct`() {
        launchFragment().onFragment {
            verify(mockFrontBackUploadViewModel).registerActivityResultCaller(same(it))

            val binding = FrontBackUploadFragmentBinding.bind(it.requireView())

            assertThat(binding.selectFront.visibility).isEqualTo(View.VISIBLE)
            assertThat(binding.progressCircularFront.visibility).isEqualTo(View.GONE)
            assertThat(binding.finishedCheckMarkFront.visibility).isEqualTo(View.GONE)
            assertThat(binding.selectBack.visibility).isEqualTo(View.VISIBLE)
            assertThat(binding.progressCircularBack.visibility).isEqualTo(View.GONE)
            assertThat(binding.finishedCheckMarkBack.visibility).isEqualTo(View.GONE)
            assertThat(binding.kontinue.isEnabled).isEqualTo(false)

            assertThat(binding.titleText.text).isEqualTo(it.getString(R.string.file_upload))
            assertThat(binding.contentText.text).isEqualTo(it.getString(R.string.file_upload_content_id))
            assertThat(binding.labelFront.text).isEqualTo(it.getString(R.string.front_of_id))
            assertThat(binding.labelBack.text).isEqualTo(it.getString(R.string.back_of_id))
            assertThat(binding.finishedCheckMarkFront.contentDescription).isEqualTo(it.getString(R.string.front_of_id_selected))
            assertThat(binding.finishedCheckMarkBack.contentDescription).isEqualTo(it.getString(R.string.back_of_id_selected))
        }
    }

    @Test
    fun `verify select front take photo interactions`() {
        verifyFlow(IdentityScanState.ScanType.ID_FRONT, true)
    }

    @Test
    fun `verify select front choose file interactions`() {
        verifyFlow(IdentityScanState.ScanType.ID_FRONT, false)
    }

    @Test
    fun `verify select back take photo interactions`() {
        verifyFlow(IdentityScanState.ScanType.ID_BACK, true)
    }

    @Test
    fun `verify select back choose file interactions`() {
        verifyFlow(IdentityScanState.ScanType.ID_BACK, false)
    }

    @Test
    fun `verify front upload updates UI`() {
        launchFragment().onFragment {
            frontUploaded.postValue(Unit)
            val binding = FrontBackUploadFragmentBinding.bind(it.requireView())

            assertThat(binding.selectFront.visibility).isEqualTo(View.GONE)
            assertThat(binding.progressCircularFront.visibility).isEqualTo(View.GONE)
            assertThat(binding.finishedCheckMarkFront.visibility).isEqualTo(View.VISIBLE)
        }
    }

    @Test
    fun `verify back upload updates UI`() {
        launchFragment().onFragment {
            backUploaded.postValue(Unit)
            val binding = FrontBackUploadFragmentBinding.bind(it.requireView())

            assertThat(binding.selectBack.visibility).isEqualTo(View.GONE)
            assertThat(binding.progressCircularBack.visibility).isEqualTo(View.GONE)
            assertThat(binding.finishedCheckMarkBack.visibility).isEqualTo(View.VISIBLE)
        }
    }

    @Test
    fun `verify uploadFinished updates UI`() {
        launchFragment().onFragment {
            frontUploaded.postValue(Unit)
            backUploaded.postValue(Unit)
            uploadFinished.postValue(Unit)
            val binding = FrontBackUploadFragmentBinding.bind(it.requireView())

            assertThat(binding.selectFront.visibility).isEqualTo(View.GONE)
            assertThat(binding.progressCircularFront.visibility).isEqualTo(View.GONE)
            assertThat(binding.finishedCheckMarkFront.visibility).isEqualTo(View.VISIBLE)
            assertThat(binding.selectBack.visibility).isEqualTo(View.GONE)
            assertThat(binding.progressCircularBack.visibility).isEqualTo(View.GONE)
            assertThat(binding.finishedCheckMarkBack.visibility).isEqualTo(View.VISIBLE)

            assertThat(binding.kontinue.isEnabled).isTrue()
        }
    }

    @Test
    fun `verify when kontinue is clicked navigates to confirmation`() {
        launchFragment().onFragment {
            val navController = TestNavHostController(
                ApplicationProvider.getApplicationContext()
            )
            navController.setGraph(
                R.navigation.identity_nav_graph
            )
            navController.setCurrentDestination(R.id.IDUploadFragment)
            Navigation.setViewNavController(
                it.requireView(),
                navController
            )

            frontUploaded.postValue(Unit)
            backUploaded.postValue(Unit)
            uploadFinished.postValue(Unit)
            val binding = FrontBackUploadFragmentBinding.bind(it.requireView())
            binding.kontinue.callOnClick()

            assertThat(navController.currentDestination?.id)
                .isEqualTo(R.id.confirmationFragment)
        }
    }

    private fun verifyFlow(scanType: IdentityScanState.ScanType, isTakePhoto: Boolean) {
        launchFragment().onFragment {
            val binding = FrontBackUploadFragmentBinding.bind(it.requireView())
            // click select front button
            if (scanType == IdentityScanState.ScanType.ID_FRONT) {
                binding.selectFront.callOnClick()
            } else if (scanType == IdentityScanState.ScanType.ID_BACK) {
                binding.selectBack.callOnClick()
            }
            val dialog = ShadowDialog.getLatestDialog()

            // dialog shows up
            assertThat(dialog.isShowing).isTrue()
            assertThat(dialog).isInstanceOf(BottomSheetDialog::class.java)

            // click take photo button
            if (isTakePhoto) {
                dialog.findViewById<Button>(R.id.take_photo).callOnClick()
            } else {
                dialog.findViewById<Button>(R.id.choose_file).callOnClick()
            }

            // dialog dismissed
            assertThat(dialog.isShowing).isFalse()

            // viewmodel triggers

            val callbackCaptor: KArgumentCaptor<(Uri) -> Unit> = argumentCaptor()

            if (isTakePhoto) {
                if (scanType == IdentityScanState.ScanType.ID_FRONT) {
                    verify(mockFrontBackUploadViewModel).takePhotoFront(
                        same(it.requireContext()),
                        callbackCaptor.capture()
                    )
                } else if (scanType == IdentityScanState.ScanType.ID_BACK) {
                    verify(mockFrontBackUploadViewModel).takePhotoBack(
                        same(it.requireContext()),
                        callbackCaptor.capture()
                    )
                }
            } else {
                if (scanType == IdentityScanState.ScanType.ID_FRONT) {
                    verify(mockFrontBackUploadViewModel).chooseImageFront(callbackCaptor.capture())
                } else if (scanType == IdentityScanState.ScanType.ID_BACK) {
                    verify(mockFrontBackUploadViewModel).chooseImageBack(callbackCaptor.capture())
                }
            }

            // mock photo taken/image chosen
            callbackCaptor.firstValue(mockUri)

            // viewmodel triggers and UI updates
            if (scanType == IdentityScanState.ScanType.ID_FRONT) {
                verify(mockFrontBackUploadViewModel).uploadImageFront(
                    same(mockUri),
                    same(it.requireContext())
                )
                assertThat(binding.selectFront.visibility).isEqualTo(View.GONE)
                assertThat(binding.progressCircularFront.visibility).isEqualTo(View.VISIBLE)
                assertThat(binding.finishedCheckMarkFront.visibility).isEqualTo(View.GONE)
            } else if (scanType == IdentityScanState.ScanType.ID_BACK) {
                verify(mockFrontBackUploadViewModel).uploadImageBack(
                    same(mockUri),
                    same(it.requireContext())
                )
                assertThat(binding.selectBack.visibility).isEqualTo(View.GONE)
                assertThat(binding.progressCircularBack.visibility).isEqualTo(View.VISIBLE)
                assertThat(binding.finishedCheckMarkBack.visibility).isEqualTo(View.GONE)
            }
        }
    }

    private fun launchFragment() = launchFragmentInContainer(
        themeResId = R.style.Theme_MaterialComponents
    ) {
        TestFragment(idUploadFragmentViewModelFactory)
    }

    internal class TestFragment(frontBackUploadViewModelFactory: ViewModelProvider.Factory) :
        FrontBackUploadFragment(frontBackUploadViewModelFactory) {
        override val titleRes = R.string.file_upload
        override val contextRes = R.string.file_upload_content_id
        override val frontTextRes = R.string.front_of_id
        override val backTextRes = R.string.back_of_id
        override val frontCheckMarkContentDescription = R.string.front_of_id_selected
        override val backCheckMarkContentDescription = R.string.back_of_id_selected
        override val continueButtonNavigationId =
            R.id.action_IDUploadFragment_to_confirmationFragment
        override val frontScanType = IdentityScanState.ScanType.ID_FRONT
        override val backScanType = IdentityScanState.ScanType.ID_BACK
    }
}
