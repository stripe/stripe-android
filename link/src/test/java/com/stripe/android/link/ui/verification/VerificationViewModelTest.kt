package com.stripe.android.link.ui.verification

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import com.stripe.android.core.Logger
import com.stripe.android.core.strings.resolvableString
import com.stripe.android.link.LinkScreen
import com.stripe.android.link.TestFactory
import com.stripe.android.link.account.FakeLinkAccountManager
import com.stripe.android.link.account.LinkAccountManager
import com.stripe.android.link.analytics.FakeLinkEventsReporter
import com.stripe.android.link.analytics.LinkEventsReporter
import com.stripe.android.link.model.LinkAccount
import com.stripe.android.model.ConsumerSession
import com.stripe.android.testing.FakeLogger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
internal class VerificationViewModelTest {
    private val dispatcher = UnconfinedTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

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
            val screens = arrayListOf<LinkScreen>()
            fun navigateAndClearStack(screen: LinkScreen) {
                screens.add(screen)
            }

            val viewModel = createViewModel(
                navigateAndClearStack = ::navigateAndClearStack
            )
            viewModel.onVerificationCodeEntered("code")

            assertThat(screens).isEqualTo(listOf(LinkScreen.Wallet))
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

        val navScreens = arrayListOf<LinkScreen>()
        fun navigateAndClearStack(screen: LinkScreen) {
            navScreens.add(screen)
        }

        createViewModel(
            linkAccountManager = linkAccountManager,
            navigateAndClearStack = ::navigateAndClearStack
        ).onChangeEmailClicked()

        assertThat(linkAccountManager.callCount).isEqualTo(1)
        assertThat(navScreens).isEqualTo(listOf(LinkScreen.SignUp))
    }

    @Test
    fun `onBack triggers logout and sends analytics event`() = runTest(dispatcher) {
        val linkEventsReporter = object : FakeLinkEventsReporter() {
            var callCount = 0
            override fun on2FACancel() {
                callCount += 1
            }
        }
        val linkAccountManager = object : FakeLinkAccountManager() {
            var callCount = 0
            override suspend fun logOut(): Result<ConsumerSession> {
                callCount += 1
                return super.logOut()
            }
        }
        createViewModel(
            linkEventsReporter = linkEventsReporter,
            linkAccountManager = linkAccountManager
        ).onBack()
        assertThat(linkEventsReporter.callCount).isEqualTo(1)
        assertThat(linkAccountManager.callCount).isEqualTo(1)
    }

    @Test
    fun `Resending code is reflected in state`() = runTest(dispatcher) {
        val linkAccountManager = object : FakeLinkAccountManager() {
            override suspend fun startVerification(): Result<LinkAccount> {
                delay(100)
                return super.startVerification()
            }
        }

        val viewModel = createViewModel(linkAccountManager = linkAccountManager).apply {
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
    fun `Failing to resend code is reflected in state`() = runTest(dispatcher) {
        val linkAccountManager = object : FakeLinkAccountManager() {
            override suspend fun startVerification(): Result<LinkAccount> {
                delay(100)
                return Result.failure(RuntimeException("error"))
            }
        }

        val viewModel = createViewModel(linkAccountManager = linkAccountManager).apply {
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
            assertThat(finalState.errorMessage).isEqualTo("error".resolvableString)
        }
    }

    private fun createViewModel(
        linkAccountManager: LinkAccountManager = FakeLinkAccountManager(),
        linkEventsReporter: LinkEventsReporter = FakeLinkEventsReporter(),
        logger: Logger = FakeLogger(),
        navigateAndClearStack: (LinkScreen) -> Unit = {},
        goBack: () -> Unit = {},
    ): VerificationViewModel {
        return VerificationViewModel(
            linkAccountManager = linkAccountManager,
            linkEventsReporter = linkEventsReporter,
            logger = logger,
            navigateAndClearStack = navigateAndClearStack,
            goBack = goBack,
            linkAccount = TestFactory.LINK_ACCOUNT
        )
    }
}
