package com.stripe.android.identity.navigation

import android.net.Uri
import android.view.View
import android.widget.Button
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.fragment.app.testing.launchFragmentInContainer
import androidx.lifecycle.MutableLiveData
import androidx.navigation.Navigation
import androidx.navigation.testing.TestNavHostController
import androidx.test.core.app.ApplicationProvider
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.common.truth.Truth.assertThat
import com.stripe.android.identity.R
import com.stripe.android.identity.databinding.PassportUploadFragmentBinding
import com.stripe.android.identity.viewModelFactoryFor
import com.stripe.android.identity.viewmodel.PassportUploadViewModel
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
class PassportUploadFragmentTest {
    @get:Rule
    var rule: TestRule = InstantTaskExecutorRule()

    private val uploaded = MutableLiveData<Unit>()
    private val mockUri = mock<Uri>()

    private val mockPassportUploadViewModel = mock<PassportUploadViewModel>().also {
        whenever(it.uploaded).thenReturn(uploaded)
    }

    @Test
    fun `when initialized viewmodel registers activityResultCaller and UI is correct`() {
        launchFragment().onFragment {
            verify(mockPassportUploadViewModel).registerActivityResultCaller(same(it))

            val binding = PassportUploadFragmentBinding.bind(it.requireView())

            assertThat(binding.select.visibility).isEqualTo(View.VISIBLE)
            assertThat(binding.progressCircular.visibility).isEqualTo(View.GONE)
            assertThat(binding.finishedCheckMark.visibility).isEqualTo(View.GONE)
            assertThat(binding.kontinue.isEnabled).isEqualTo(false)
        }
    }

    @Test
    fun `verify take photo interactions`() {
        verifyFlow(true)
    }

    @Test
    fun `verify choose file interactions`() {
        verifyFlow(false)
    }

    @Test
    fun `verify upload updates UI`() {
        launchFragment().onFragment {
            uploaded.postValue(Unit)
            val binding = PassportUploadFragmentBinding.bind(it.requireView())

            assertThat(binding.select.visibility).isEqualTo(View.GONE)
            assertThat(binding.progressCircular.visibility).isEqualTo(View.GONE)
            assertThat(binding.finishedCheckMark.visibility).isEqualTo(View.VISIBLE)
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
            navController.setCurrentDestination(R.id.passportUploadFragment)
            Navigation.setViewNavController(
                it.requireView(),
                navController
            )

            uploaded.postValue(Unit)
            val binding = PassportUploadFragmentBinding.bind(it.requireView())
            binding.kontinue.callOnClick()

            assertThat(navController.currentDestination?.id)
                .isEqualTo(R.id.confirmationFragment)
        }
    }

    private fun verifyFlow(isTakePhoto: Boolean) {
        launchFragment().onFragment {
            val binding = PassportUploadFragmentBinding.bind(it.requireView())
            binding.select.callOnClick()

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
                verify(mockPassportUploadViewModel).takePhoto(
                    same(it.requireContext()),
                    callbackCaptor.capture()
                )
            } else {
                verify(mockPassportUploadViewModel).chooseImage(callbackCaptor.capture())
            }

            // mock photo taken/image chosen
            callbackCaptor.firstValue(mockUri)

            // viewmodel triggers and UI updates
            verify(mockPassportUploadViewModel).uploadImage(
                same(mockUri),
                same(it.requireContext())
            )
            assertThat(binding.select.visibility).isEqualTo(View.GONE)
            assertThat(binding.progressCircular.visibility).isEqualTo(View.VISIBLE)
            assertThat(binding.finishedCheckMark.visibility).isEqualTo(View.GONE)
        }
    }

    private fun launchFragment() = launchFragmentInContainer(
        themeResId = R.style.Theme_MaterialComponents
    ) {
        PassportUploadFragment(viewModelFactoryFor(mockPassportUploadViewModel))
    }
}
