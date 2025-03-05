package com.stripe.android.identity.viewmodel

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import androidx.lifecycle.SavedStateHandle
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import com.stripe.android.core.injection.DUMMY_INJECTOR_KEY
import com.stripe.android.identity.IdentityVerificationSheetContract
import com.stripe.android.identity.analytics.IdentityAnalyticsRequestFactory
import com.stripe.android.identity.networking.IdentityRepository
import com.stripe.android.identity.networking.Resource
import com.stripe.android.identity.networking.Status
import com.stripe.android.identity.networking.models.VerificationPage
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TestRule
import org.junit.runner.RunWith
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.robolectric.RobolectricTestRunner

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
internal class IdentityViewModelTest {
    @get:Rule
    var rule: TestRule = InstantTaskExecutorRule()

    private val mockVerificationPage = mock<VerificationPage>()

    private val mockIdentityRepository = mock<IdentityRepository> {
        onBlocking {
            retrieveVerificationPage(any(), any())
        }.thenReturn(mockVerificationPage)
    }

    private val mockSavedStateHandle = mock<SavedStateHandle> {
        on { getLiveData<Resource<VerificationPage>>(any(), any()) } doReturn MutableLiveData()
    }

    private val mockIdentityAnalyticsRequestFactory = mock<IdentityAnalyticsRequestFactory>()

    private lateinit var viewModel: IdentityViewModel

    @Before
    fun setup() {
        viewModel = IdentityViewModel(
            ApplicationProvider.getApplicationContext(),
            IdentityVerificationSheetContract.Args(
                verificationSessionId = VERIFICATION_SESSION_ID,
                ephemeralKeySecret = EPHEMERAL_KEY,
                injectorKey = DUMMY_INJECTOR_KEY,
                presentTime = 0
            ),
            mockIdentityRepository,
            mockIdentityAnalyticsRequestFactory,
            mockSavedStateHandle,
        )
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
        }

    @Test
    fun `retrieveAndBufferVerificationPage handles error`() = runTest {
        val exception = IllegalStateException("Test error")
        whenever(mockIdentityRepository.retrieveVerificationPage(any(), any())).thenThrow(exception)

        viewModel.retrieveAndBufferVerificationPage()

        assertThat(viewModel.verificationPage.value?.status).isEqualTo(Status.ERROR)
        assertThat(viewModel.verificationPage.value?.throwable).isInstanceOf(IllegalStateException::class.java)
    }

    @Test
    fun `camera permission state is initially false`() = runTest {
        assertThat(viewModel.cameraPermissionGranted.first()).isFalse()
    }

    @Test
    fun `verificationPageData state is initially idle`() = runTest {
        assertThat(viewModel.verificationPageData.first().status).isEqualTo(Status.IDLE)
    }

    @Test
    fun `verificationPageSubmit state is initially idle`() = runTest {
        assertThat(viewModel.verificationPageSubmit.first().status).isEqualTo(Status.IDLE)
    }

    @Test
    fun `pageAndModelFiles updates when verificationPage updates`() {
        val observer = mock<Observer<Resource<IdentityViewModel.PageAndModelFiles>>>()
        viewModel.pageAndModelFiles.observeForever(observer)

        viewModel.retrieveAndBufferVerificationPage()

        verify(observer).onChanged(
            Resource.success(
                IdentityViewModel.PageAndModelFiles(
                    page = mockVerificationPage
                )
            )
        )
    }

    @Test
    fun `observeForVerificationPage invokes success callback on success`() {
        var successCalled = false
        var failureCalled = false

        viewModel.observeForVerificationPage(
            TestLifecycleOwner(),
            onSuccess = { successCalled = true },
            onFailure = { failureCalled = true }
        )

        viewModel.retrieveAndBufferVerificationPage()

        assertThat(successCalled).isTrue()
        assertThat(failureCalled).isFalse()
    }

    @Test
    fun `observeForVerificationPage invokes failure callback on error`() = runTest {
        whenever(mockIdentityRepository.retrieveVerificationPage(any(), any()))
            .thenThrow(IllegalStateException("Test error"))

        var successCalled = false
        var failureCalled = false

        viewModel.observeForVerificationPage(
            TestLifecycleOwner(),
            onSuccess = { successCalled = true },
            onFailure = { failureCalled = true }
        )

        viewModel.retrieveAndBufferVerificationPage()

        assertThat(successCalled).isFalse()
        assertThat(failureCalled).isTrue()
    }

    @Test
    fun `error logging triggers analytics`() {
        val testError = IllegalStateException("Test error")
        viewModel.errorCause.value = testError

        verify(mockIdentityAnalyticsRequestFactory).genericError(
            eq(testError.message),
            eq(testError.stackTraceToString())
        )
    }

    private companion object {
        const val VERIFICATION_SESSION_ID = "id_5678"
        const val EPHEMERAL_KEY = "eak_5678"
    }
}

// Test utility class
private class TestLifecycleOwner : LifecycleOwner {
    private val _lifecycle = LifecycleRegistry(this).apply {
        handleLifecycleEvent(Lifecycle.Event.ON_RESUME)
    }

    override val lifecycle: Lifecycle
        get() = _lifecycle
}
