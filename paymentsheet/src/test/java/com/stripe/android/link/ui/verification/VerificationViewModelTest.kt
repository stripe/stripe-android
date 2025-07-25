package com.stripe.android.link.ui.verification

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import com.stripe.android.core.Logger
import com.stripe.android.core.strings.resolvableString
import com.stripe.android.link.LinkActivityResult
import com.stripe.android.link.LinkLaunchMode
import com.stripe.android.link.TestFactory
import com.stripe.android.link.account.FakeLinkAccountManager
import com.stripe.android.link.account.LinkAccountManager
import com.stripe.android.link.analytics.FakeLinkEventsReporter
import com.stripe.android.link.analytics.LinkEventsReporter
import com.stripe.android.link.model.LinkAccount
import com.stripe.android.model.ConsumerSession
import com.stripe.android.testing.CoroutineTestRule
import com.stripe.android.testing.FakeLogger
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
internal class VerificationViewModelTest {
    private val dispatcher = UnconfinedTestDispatcher()

    @get:Rule
    val coroutineTestRule = CoroutineTestRule(dispatcher)

    @Test
    fun `init starts verification with link account manager`() = runTest(dispatcher) {
        val linkAccountManager = object : FakeLinkAccountManager() {
            var callCount = 0
            override suspend fun startVerification(): Result<LinkAccount> {
                callCount += 1
                return Result.success(TestFactory.LINK_ACCOUNT)
            }
        }

        createViewModel(
            linkAccountManager = linkAccountManager
        )

        assertThat(linkAccountManager.callCount).isEqualTo(1)
    }

    @Test
    fun `When startVerification fails then an error message is shown`() = runTest(dispatcher) {
        val errorMessage = "Error message"
        val linkAccountManager = FakeLinkAccountManager()
        linkAccountManager.startVerificationResult = Result.failure(RuntimeException(errorMessage))

        val viewModel = createViewModel(linkAccountManager = linkAccountManager)

        assertThat(viewModel.viewState.value.errorMessage).isEqualTo(errorMessage.resolvableString)
    }

    @Test
    fun `When confirmVerification succeeds then it navigates to Wallet`() =
        runTest(dispatcher) {
            val onVerificationSucceededCalls = arrayListOf<Unit>()
            fun onVerificationSucceeded() {
                onVerificationSucceededCalls.add(Unit)
            }

            val viewModel = createViewModel(
                onVerificationSucceeded = ::onVerificationSucceeded,
            )
            viewModel.onVerificationCodeEntered("code")

            assertThat(onVerificationSucceededCalls).hasSize(1)
        }

    @Test
    fun `When confirmVerification fails then an error message is shown`() =
        runTest(dispatcher) {
            val errorMessage = "Error message"
            val linkAccountManager = FakeLinkAccountManager()

            linkAccountManager.confirmVerificationResult = Result.failure(RuntimeException(errorMessage))

            val viewModel = createViewModel(
                linkAccountManager = linkAccountManager
            )
            viewModel.onVerificationCodeEntered("code")

            assertThat(viewModel.viewState.value.errorMessage).isEqualTo(errorMessage.resolvableString)
        }

    @Test
    fun `When confirmVerification fails then code is cleared`() = runTest(dispatcher) {
        val linkEventsReporter = object : FakeLinkEventsReporter() {
            override fun on2FAFailure() = Unit
        }
        val linkAccountManager = object : FakeLinkAccountManager() {
            var codeUsed: String? = null
            override suspend fun confirmVerification(code: String): Result<LinkAccount> {
                codeUsed = code
                return Result.failure(RuntimeException("Error"))
            }
        }

        val viewModel = createViewModel(
            linkAccountManager = linkAccountManager,
            linkEventsReporter = linkEventsReporter
        )
        viewModel.onVerificationCodeEntered("555555")

        assertThat(linkAccountManager.codeUsed).isEqualTo("555555")
        assertThat(viewModel.otpElement.controller.fieldValue.value).isEqualTo("")
    }

    @Test
    fun `onChangeEmailClicked triggers logout`() = runTest(dispatcher) {
        val linkAccountManager = object : FakeLinkAccountManager() {
            var callCount = 0
            override suspend fun logOut(): Result<ConsumerSession> {
                callCount += 1
                return super.logOut()
            }
        }

        val onChangeEmailRequestedCalls = arrayListOf<Unit>()
        fun onChangeEmailRequested() {
            onChangeEmailRequestedCalls.add(Unit)
        }

        createViewModel(
            linkAccountManager = linkAccountManager,
            onChangeEmailRequested = ::onChangeEmailRequested,
        ).onChangeEmailButtonClicked()

        assertThat(linkAccountManager.callCount).isEqualTo(1)
        assertThat(onChangeEmailRequestedCalls).containsExactly(Unit)
    }

    @Test
    fun `Resending code is reflected in state`() = runTest(dispatcher) {
        val linkAccountManager = object : FakeLinkAccountManager() {
            override suspend fun startVerification(): Result<LinkAccount> {
                delay(100)
                return super.startVerification()
            }
        }

        val viewModel = createViewModel(linkAccountManager = linkAccountManager)

        viewModel.viewState.test {
            val originalState = awaitItem()
            assertThat(originalState.isSendingNewCode).isFalse()
            assertThat(originalState.didSendNewCode).isFalse()

            viewModel.resendCode()

            val intermediateState = awaitItem()
            assertThat(intermediateState.isSendingNewCode).isTrue()
            assertThat(intermediateState.didSendNewCode).isFalse()

            // The delay in the FakeLinkAccountManager will cause the state to be updated

            val finalState = awaitItem()
            assertThat(finalState.isSendingNewCode).isFalse()
            assertThat(finalState.didSendNewCode).isTrue()
        }
    }

    @Test
    fun `Failing to resend code is reflected in state`() = runTest(dispatcher) {
        val linkAccountManager = object : FakeLinkAccountManager() {
            override suspend fun startVerification(): Result<LinkAccount> {
                delay(100)
                return Result.failure(RuntimeException("error"))
            }
        }

        val viewModel = createViewModel(linkAccountManager = linkAccountManager)

        viewModel.viewState.test {
            val originalState = awaitItem()
            assertThat(originalState.isSendingNewCode).isFalse()
            assertThat(originalState.didSendNewCode).isFalse()
            assertThat(originalState.errorMessage).isNull()

            viewModel.resendCode()

            val intermediateState = awaitItem()
            assertThat(intermediateState.isSendingNewCode).isTrue()
            assertThat(intermediateState.didSendNewCode).isFalse()
            assertThat(intermediateState.errorMessage).isNull()

            // The delay in the FakeLinkAccountManager will cause the state to be updated

            val finalState = awaitItem()
            assertThat(finalState.isSendingNewCode).isFalse()
            assertThat(finalState.didSendNewCode).isFalse()
            assertThat(finalState.errorMessage).isEqualTo("error".resolvableString)
        }
    }

    private fun createViewModel(
        linkAccountManager: LinkAccountManager = FakeLinkAccountManager(),
        linkEventsReporter: LinkEventsReporter = FakeLinkEventsReporter(),
        logger: Logger = FakeLogger(),
        linkLaunchMode: LinkLaunchMode = LinkLaunchMode.PaymentMethodSelection(null),
        onVerificationSucceeded: () -> Unit = { },
        onChangeEmailRequested: () -> Unit = {},
        onDismissClicked: () -> Unit = {},
        dismissWithResult: (LinkActivityResult) -> Unit = { },
    ): VerificationViewModel {
        return VerificationViewModel(
            linkAccount = TestFactory.LINK_ACCOUNT,
            linkAccountManager = linkAccountManager,
            linkEventsReporter = linkEventsReporter,
            logger = logger,
            linkLaunchMode = linkLaunchMode,
            isDialog = false,
            onVerificationSucceeded = onVerificationSucceeded,
            onChangeEmailRequested = onChangeEmailRequested,
            onDismissClicked = onDismissClicked,
            dismissWithResult = dismissWithResult
        )
    }
}
