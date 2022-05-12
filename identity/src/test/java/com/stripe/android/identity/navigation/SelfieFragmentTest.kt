package com.stripe.android.identity.navigation

import android.view.View
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.fragment.app.testing.launchFragmentInContainer
import androidx.lifecycle.MediatorLiveData
import androidx.navigation.Navigation
import androidx.navigation.testing.TestNavHostController
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import com.stripe.android.identity.R
import com.stripe.android.identity.camera.IdentityAggregator
import com.stripe.android.identity.camera.IdentityScanFlow
import com.stripe.android.identity.databinding.SelfieScanFragmentBinding
import com.stripe.android.identity.networking.Resource
import com.stripe.android.identity.networking.models.VerificationPage
import com.stripe.android.identity.states.IdentityScanState
import com.stripe.android.identity.utils.SingleLiveEvent
import com.stripe.android.identity.viewModelFactoryFor
import com.stripe.android.identity.viewmodel.IdentityScanViewModel
import com.stripe.android.identity.viewmodel.IdentityViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TestRule
import org.junit.runner.RunWith
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.robolectric.RobolectricTestRunner
import java.io.File

@RunWith(RobolectricTestRunner::class)
internal class SelfieFragmentTest {
    @get:Rule
    var rule: TestRule = InstantTaskExecutorRule()

    private val finalResultLiveData = SingleLiveEvent<IdentityAggregator.FinalResult>()
    private val displayStateChanged = SingleLiveEvent<Pair<IdentityScanState, IdentityScanState?>>()
    private val mockScanFlow = mock<IdentityScanFlow>()

    private val mockIdentityScanViewModel = mock<IdentityScanViewModel> {
        on { it.identityScanFlow } doReturn mockScanFlow
        on { it.finalResult } doReturn finalResultLiveData
        on { it.displayStateChanged } doReturn displayStateChanged
    }

    private val mockPageAndModel = MediatorLiveData<Resource<Pair<VerificationPage, File>>>()
    private val uploadState =
        MutableStateFlow(IdentityViewModel.UploadState())

    private val mockIdentityViewModel = mock<IdentityViewModel> {
        on { pageAndModel } doReturn mockPageAndModel
        on { uploadState } doReturn uploadState
    }

    @Test
    fun `when initialized UI is reset`() {
        launchSelfieFragment { binding, _ ->
            assertThat(binding.scanningView.visibility).isEqualTo(View.VISIBLE)
            assertThat(binding.resultView.visibility).isEqualTo(View.GONE)
            assertThat(binding.kontinue.isEnabled).isEqualTo(false)
        }
    }

    @Test
    fun `when finished UI is toggled`() {
        launchSelfieFragment { binding, _ ->
            // mock success of front scan
            displayStateChanged.postValue((mock<IdentityScanState.Finished>() to mock()))

            assertThat(binding.scanningView.visibility).isEqualTo(View.GONE)
            assertThat(binding.resultView.visibility).isEqualTo(View.VISIBLE)
            assertThat(binding.kontinue.isEnabled).isEqualTo(true)
        }
    }

    private fun launchSelfieFragment(testBlock: (SelfieScanFragmentBinding, TestNavHostController) -> Unit) =
        launchFragmentInContainer(
            themeResId = R.style.Theme_MaterialComponents
        ) {
            SelfieFragment(
                viewModelFactoryFor(mockIdentityScanViewModel),
                viewModelFactoryFor(mockIdentityViewModel)
            )
        }.onFragment {
            val navController = TestNavHostController(
                ApplicationProvider.getApplicationContext()
            )
            navController.setGraph(
                R.navigation.identity_nav_graph
            )
            navController.setCurrentDestination(R.id.couldNotCaptureFragment)
            Navigation.setViewNavController(
                it.requireView(),
                navController
            )
            testBlock(SelfieScanFragmentBinding.bind(it.requireView()), navController)
        }
}
