package com.stripe.android.link.ui.verification

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.asLiveData
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryOwner
import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import com.stripe.android.core.Logger
import com.stripe.android.core.injection.Injectable
import com.stripe.android.core.injection.NonFallbackInjector
import com.stripe.android.link.LinkScreen
import com.stripe.android.link.account.LinkAccountManager
import com.stripe.android.link.analytics.LinkEventsReporter
import com.stripe.android.link.model.AccountStatus
import com.stripe.android.link.model.LinkAccount
import com.stripe.android.link.model.Navigator
import com.stripe.android.link.ui.ErrorMessage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.any
import org.mockito.kotlin.doSuspendableAnswer
import org.mockito.kotlin.mock
import org.mockito.kotlin.spy
import org.mockito.kotlin.stub
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.robolectric.RobolectricTestRunner
import kotlin.test.AfterTest
import kotlin.test.BeforeTest

@RunWith(RobolectricTestRunner::class)
class VerificationViewModelTest {
    private val linkAccountManager = mock<LinkAccountManager>()
    private val linkEventsReporter = mock<LinkEventsReporter>()
    private val navigator = mock<Navigator>()
    private val logger = Logger.noop()
    private val linkAccount = mock<LinkAccount>().apply {
        whenever(accountStatus).thenReturn(AccountStatus.VerificationStarted)
    }

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `init sends analytics event`() = runTest {
        createViewModel()
        verify(linkEventsReporter).on2FAStart()
    }

    @Test
    fun `startVerification triggers verification start`() = runTest {
        whenever(linkAccountManager.startVerification())
            .thenReturn(Result.success(mock()))

        val viewModel = createViewModel()
        viewModel.startVerification()

        verify(linkAccountManager).startVerification()
    }

    @Test
    fun `When startVerification fails then an error message is shown`() = runTest {
        val errorMessage = "Error message"
        whenever(linkAccountManager.startVerification())
            .thenReturn(Result.failure(RuntimeException(errorMessage)))

        val viewModel = createViewModel()
        viewModel.startVerification()

        assertThat(viewModel.viewState.value.errorMessage).isEqualTo(ErrorMessage.Raw(errorMessage))
    }

    @Test
    fun `When confirmVerification succeeds then it navigates to Wallet and analytics event is sent`() =
        runTest {
            whenever(linkAccountManager.confirmVerification(any()))
                .thenReturn(Result.success(mock()))

            val viewModel = createViewModel()
            viewModel.onVerificationCodeEntered("code")

            verify(navigator).navigateTo(LinkScreen.Wallet, true)
            verify(linkEventsReporter).on2FAComplete()
        }

    @Test
    fun `When confirmVerification fails then an error message is shown and analytics event is sent`() =
        runTest {
            val errorMessage = "Error message"
            whenever(linkAccountManager.confirmVerification(any()))
                .thenReturn(Result.failure(RuntimeException(errorMessage)))

            val viewModel = createViewModel()
            viewModel.onVerificationCodeEntered("code")

            assertThat(viewModel.viewState.value.errorMessage).isEqualTo(ErrorMessage.Raw(errorMessage))
            verify(linkEventsReporter).on2FAFailure()
        }

    @Test
    fun `When confirmVerification fails then code is cleared`() =
        runTest {
            whenever(linkAccountManager.confirmVerification(any()))
                .thenReturn(Result.failure(RuntimeException("Error")))
            val viewModel = createViewModel()

            var otp = ""
            viewModel.otpElement.controller.fieldValue.asLiveData().observeForever {
                otp = it
            }

            for (i in 0 until viewModel.otpElement.controller.otpLength) {
                viewModel.otpElement.controller.onValueChanged(i, "1")
            }

            assertThat(otp).isEqualTo("")
        }

    @Test
    fun `onBack triggers logout and sends analytics event`() = runTest {
        createViewModel().onBack()
        verify(linkEventsReporter).on2FACancel()
        verify(linkAccountManager).logout()
    }

    @Test
    fun `Resending code is reflected in state`() = runTest {
        linkAccountManager.stub {
            onBlocking { startVerification() }.doSuspendableAnswer {
                delay(100)
                Result.success(mock())
            }
        }

        val viewModel = createViewModel().apply {
            resendCode()
        }

        viewModel.viewState.test {
            val intermediateState = awaitItem()
            assertThat(intermediateState.isSendingNewCode).isTrue()
            assertThat(intermediateState.didSendNewCode).isFalse()

            val finalState = awaitItem()
            assertThat(finalState.isSendingNewCode).isFalse()
            assertThat(finalState.didSendNewCode).isTrue()
        }
    }

    @Test
    fun `Failing to resend code is reflected in state`() = runTest {
        linkAccountManager.stub {
            onBlocking { startVerification() }.doSuspendableAnswer {
                delay(100)
                Result.failure(RuntimeException("error"))
            }
        }

        val viewModel = createViewModel().apply {
            resendCode()
        }

        viewModel.viewState.test {
            val intermediateState = awaitItem()
            assertThat(intermediateState.isSendingNewCode).isTrue()
            assertThat(intermediateState.didSendNewCode).isFalse()
            assertThat(intermediateState.errorMessage).isNull()

            val finalState = awaitItem()
            assertThat(finalState.isSendingNewCode).isFalse()
            assertThat(finalState.didSendNewCode).isFalse()
            assertThat(finalState.errorMessage).isEqualTo(ErrorMessage.Raw("error"))
        }
    }

    @Test
    fun `Factory gets initialized by Injector`() {
        val vmToBeReturned = mock<VerificationViewModel>()

        val mockSavedStateRegistryOwner = mock<SavedStateRegistryOwner>()
        val mockSavedStateRegistry = mock<SavedStateRegistry>()
        val mockLifeCycle = mock<Lifecycle>()

        whenever(mockSavedStateRegistryOwner.savedStateRegistry).thenReturn(mockSavedStateRegistry)
        whenever(mockSavedStateRegistryOwner.lifecycle).thenReturn(mockLifeCycle)
        whenever(mockLifeCycle.currentState).thenReturn(Lifecycle.State.CREATED)

        val injector = object : NonFallbackInjector {
            override fun inject(injectable: Injectable<*>) {
                val factory = injectable as VerificationViewModel.Factory
                factory.viewModel = vmToBeReturned
            }
        }

        val factory = VerificationViewModel.Factory(
            mock(),
            injector
        )
        val factorySpy = spy(factory)
        val createdViewModel = factorySpy.create(VerificationViewModel::class.java)
        assertThat(createdViewModel).isEqualTo(vmToBeReturned)
    }

    private fun createViewModel() = VerificationViewModel(
        linkAccountManager,
        linkEventsReporter,
        navigator,
        logger
    ).apply {
        init(this@VerificationViewModelTest.linkAccount)
    }
}
