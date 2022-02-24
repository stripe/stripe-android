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
import com.stripe.android.identity.databinding.IdUploadFragmentBinding
import com.stripe.android.identity.states.IdentityScanState
import com.stripe.android.identity.viewmodel.IDUploadViewModel
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TestRule
import org.junit.runner.RunWith
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.same
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.robolectric.RobolectricTestRunner
import org.robolectric.shadows.ShadowDialog

@RunWith(RobolectricTestRunner::class)
class IDUploadFragmentTest {
    @get:Rule
    var rule: TestRule = InstantTaskExecutorRule()

    private val frontPicked = MutableLiveData<Uri>()
    private val backPicked = MutableLiveData<Uri>()
    private val frontUploaded = MutableLiveData<Unit>()
    private val backUploaded = MutableLiveData<Unit>()
    private val uploadFinished = MediatorLiveData<Unit>()
    private val mockUri = mock<Uri>()

    private val mockIdUploadViewModel = mock<IDUploadViewModel>().also {
        whenever(it.frontPicked).thenReturn(frontPicked)
        whenever(it.backPicked).thenReturn(backPicked)
        whenever(it.frontUploaded).thenReturn(frontUploaded)
        whenever(it.backUploaded).thenReturn(backUploaded)
        whenever(it.uploadFinished).thenReturn(uploadFinished)
    }

    private val idUploadFragmentViewModelFactory = object : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return mockIdUploadViewModel as T
        }
    }

    @Test
    fun `when initialized viewmodel registers activityResultCaller and UI is correct`() {
        launchIDUploadFragment().onFragment {
            verify(mockIdUploadViewModel).registerActivityResultCaller(same(it))

            val binding = IdUploadFragmentBinding.bind(it.requireView())

            assertThat(binding.selectFront.visibility).isEqualTo(View.VISIBLE)
            assertThat(binding.progressCircularFront.visibility).isEqualTo(View.GONE)
            assertThat(binding.finishedCheckMarkFront.visibility).isEqualTo(View.GONE)
            assertThat(binding.selectBack.visibility).isEqualTo(View.VISIBLE)
            assertThat(binding.progressCircularBack.visibility).isEqualTo(View.GONE)
            assertThat(binding.finishedCheckMarkBack.visibility).isEqualTo(View.GONE)
            assertThat(binding.kontinue.isEnabled).isEqualTo(false)
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
        launchIDUploadFragment().onFragment {
            frontUploaded.postValue(Unit)
            val binding = IdUploadFragmentBinding.bind(it.requireView())

            assertThat(binding.selectFront.visibility).isEqualTo(View.GONE)
            assertThat(binding.progressCircularFront.visibility).isEqualTo(View.GONE)
            assertThat(binding.finishedCheckMarkFront.visibility).isEqualTo(View.VISIBLE)
        }
    }

    @Test
    fun `verify back upload updates UI`() {
        launchIDUploadFragment().onFragment {
            backUploaded.postValue(Unit)
            val binding = IdUploadFragmentBinding.bind(it.requireView())

            assertThat(binding.selectBack.visibility).isEqualTo(View.GONE)
            assertThat(binding.progressCircularBack.visibility).isEqualTo(View.GONE)
            assertThat(binding.finishedCheckMarkBack.visibility).isEqualTo(View.VISIBLE)
        }
    }

    @Test
    fun `verify uploadFinished updates UI`() {
        launchIDUploadFragment().onFragment {
            frontUploaded.postValue(Unit)
            backUploaded.postValue(Unit)
            uploadFinished.postValue(Unit)
            val binding = IdUploadFragmentBinding.bind(it.requireView())

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
        launchIDUploadFragment().onFragment {
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
            val binding = IdUploadFragmentBinding.bind(it.requireView())
            binding.kontinue.callOnClick()

            assertThat(navController.currentDestination?.id)
                .isEqualTo(R.id.confirmationFragment)
        }
    }

    private fun verifyFlow(scanType: IdentityScanState.ScanType, isTakePhoto: Boolean) {
        launchIDUploadFragment().onFragment {
            val binding = IdUploadFragmentBinding.bind(it.requireView())
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
            if (isTakePhoto) {
                verify(mockIdUploadViewModel).takePhoto(
                    eq(scanType),
                    same(it.requireContext())
                )
            } else {
                verify(mockIdUploadViewModel).chooseImage(
                    eq(scanType),
                )
            }

            // mock photo taken/image chosen
            if (scanType == IdentityScanState.ScanType.ID_FRONT) {
                frontPicked.postValue(mockUri)
            } else if (scanType == IdentityScanState.ScanType.ID_BACK) {
                backPicked.postValue(mockUri)
            }

            // viewmodel triggers and UI updates
            if (scanType == IdentityScanState.ScanType.ID_FRONT) {
                verify(mockIdUploadViewModel).uploadImage(
                    same(mockUri),
                    eq(IdentityScanState.ScanType.ID_FRONT)
                )
                assertThat(binding.selectFront.visibility).isEqualTo(View.GONE)
                assertThat(binding.progressCircularFront.visibility).isEqualTo(View.VISIBLE)
                assertThat(binding.finishedCheckMarkFront.visibility).isEqualTo(View.GONE)
            } else if (scanType == IdentityScanState.ScanType.ID_BACK) {
                verify(mockIdUploadViewModel).uploadImage(
                    same(mockUri),
                    eq(IdentityScanState.ScanType.ID_BACK)
                )
                assertThat(binding.selectBack.visibility).isEqualTo(View.GONE)
                assertThat(binding.progressCircularBack.visibility).isEqualTo(View.VISIBLE)
                assertThat(binding.finishedCheckMarkBack.visibility).isEqualTo(View.GONE)
            }
        }
    }

    private fun launchIDUploadFragment() = launchFragmentInContainer(
        themeResId = R.style.Theme_MaterialComponents
    ) {
        IDUploadFragment(idUploadFragmentViewModelFactory)
    }
}
