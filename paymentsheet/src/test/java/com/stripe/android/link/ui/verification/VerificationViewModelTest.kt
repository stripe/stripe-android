package com.stripe.android.link.ui.verification

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import com.stripe.android.core.Logger
import com.stripe.android.core.strings.resolvableString
import com.stripe.android.link.LinkActivityResult
import com.stripe.android.link.LinkLaunchMode
import com.stripe.android.link.TestFactory
import com.stripe.android.link.WebLinkAuthChannel
import com.stripe.android.link.account.FakeLinkAccountManager
import com.stripe.android.link.account.LinkAccountHolder
import com.stripe.android.link.account.LinkAccountManager
import com.stripe.android.link.analytics.FakeLinkEventsReporter
import com.stripe.android.link.analytics.LinkEventsReporter
import com.stripe.android.link.model.ConsentPresentation
import com.stripe.android.link.model.LinkAccount
import com.stripe.android.link.model.LinkAuthIntentInfo
import com.stripe.android.model.ConsentUi
import com.stripe.android.model.ConsumerSession
import com.stripe.android.model.ConsumerSessionRefresh
import com.stripe.android.testing.CoroutineTestRule
import com.stripe.android.testing.FakeLogger
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.mock
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
            fun onVerificationSucceeded(refresh: ConsumerSessionRefresh?) {
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
            override suspend fun confirmVerification(code: String, consentGranted: Boolean?): Result<LinkAccount> {
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

    @Test
    fun `LinkAccount consent presentation affects viewState consentSection`() = runTest(dispatcher) {
        val consentSection = ConsentUi.ConsentSection("Hello")
        run {
            val vm = createViewModel(linkAccount = linkAccountWithInlineConsent(consentSection))
            assertThat(vm.viewState.value.consentSection).isEqualTo(consentSection)
        }

        run {
            val vm = createViewModel(linkAccount = linkAccountWithFullscreenConsent())
            assertThat(vm.viewState.value.consentSection).isNull()
        }
    }

    @Test
    fun `onConsentShown allows consent to be granted in verification`() = runTest(dispatcher) {
        val linkAccountManager = object : FakeLinkAccountManager() {
            var consentGrantedValue: Boolean? = null
            override suspend fun confirmVerification(code: String, consentGranted: Boolean?): Result<LinkAccount> {
                consentGrantedValue = consentGranted
                return Result.success(TestFactory.LINK_ACCOUNT)
            }
        }
        val viewModel = createViewModel(linkAccountManager = linkAccountManager)

        // First verify without consent shown
        viewModel.onVerificationCodeEntered("123456")
        assertThat(linkAccountManager.consentGrantedValue).isNull()

        // Reset and test with consent shown
        linkAccountManager.consentGrantedValue = null
        viewModel.onConsentShown()
        viewModel.onVerificationCodeEntered("123456")
        assertThat(linkAccountManager.consentGrantedValue).isTrue()
    }

    @Test
    fun `confirmVerification success behavior varies by launch mode`() = runTest(dispatcher) {
        // Authentication mode should dismiss with result
        run {
            var resultCaptured: LinkActivityResult? = null
            val dismissWithResult = { result: LinkActivityResult -> resultCaptured = result }
            val viewModel = createViewModel(
                linkLaunchMode = LinkLaunchMode.Authentication(),
                dismissWithResult = dismissWithResult
            )
            viewModel.onVerificationCodeEntered("123456")
            assertThat(resultCaptured).isInstanceOf(LinkActivityResult.Completed::class.java)
        }

        // Authorization mode with inline consent should dismiss with consent granted
        run {
            var result: LinkActivityResult? = null
            val linkAccountManager = object : FakeLinkAccountManager() {
                override suspend fun confirmVerification(code: String, consentGranted: Boolean?): Result<LinkAccount> {
                    return Result.success(linkAccountWithInlineConsent())
                }
            }
            val viewModel = createViewModel(
                linkAccountManager = linkAccountManager,
                linkLaunchMode = LinkLaunchMode.Authorization("auth_123"),
                dismissWithResult = { result = it }
            )
            viewModel.onVerificationCodeEntered("123456")
            val completedResult = result as? LinkActivityResult.Completed
            assertThat(completedResult?.authorizationConsentGranted).isTrue()
        }

        // PaymentMethodSelection mode should call onVerificationSucceeded
        run {
            var onVerificationSucceededCalls = 0
            var result: LinkActivityResult? = null
            val viewModel = createViewModel(
                linkLaunchMode = LinkLaunchMode.PaymentMethodSelection(null),
                onVerificationSucceeded = { onVerificationSucceededCalls += 1 },
                dismissWithResult = { result = it }
            )
            viewModel.onVerificationCodeEntered("123456")
            assertThat(onVerificationSucceededCalls).isEqualTo(1)
            assertThat(result).isNull()
        }
    }

    @Test
    fun `Authorization mode with inline consent and onConsentShown works end to end`() = runTest(dispatcher) {
        val linkAccountManager = object : FakeLinkAccountManager() {
            var consentGrantedValue: Boolean? = null
            override suspend fun confirmVerification(code: String, consentGranted: Boolean?): Result<LinkAccount> {
                consentGrantedValue = consentGranted
                return Result.success(linkAccountWithInlineConsent(mock()))
            }
        }

        var result: LinkActivityResult? = null
        val viewModel = createViewModel(
            linkAccountManager = linkAccountManager,
            linkLaunchMode = LinkLaunchMode.Authorization("auth_123"),
            dismissWithResult = { result = it }
        )

        viewModel.onConsentShown()
        viewModel.onVerificationCodeEntered("123456")

        // Verify consent was passed correctly
        assertThat(linkAccountManager.consentGrantedValue).isTrue()

        // Verify correct result
        val completedResult = result as? LinkActivityResult.Completed
        assertThat(completedResult?.authorizationConsentGranted).isTrue()
    }

    @Test
    fun `When confirmVerification succeeds then consentGranted parameter is passed correctly`() = runTest(dispatcher) {
        val linkAccountManager = object : FakeLinkAccountManager() {
            var consentGrantedValue: Boolean? = null
            override suspend fun confirmVerification(code: String, consentGranted: Boolean?): Result<LinkAccount> {
                consentGrantedValue = consentGranted
                return Result.success(TestFactory.LINK_ACCOUNT)
            }
        }
        var onVerificationSucceededCalls = 0
        val viewModel = createViewModel(
            linkAccountManager = linkAccountManager,
            onVerificationSucceeded = { onVerificationSucceededCalls += 1 },
        )

        // Test without consent shown
        viewModel.onVerificationCodeEntered("123456")
        assertThat(linkAccountManager.consentGrantedValue).isNull()
        assertThat(onVerificationSucceededCalls).isEqualTo(1)

        // Reset and test with consent shown
        linkAccountManager.consentGrantedValue = null
        onVerificationSucceededCalls = 0
        viewModel.onConsentShown()
        viewModel.onVerificationCodeEntered("654321")
        assertThat(linkAccountManager.consentGrantedValue).isTrue()
        assertThat(onVerificationSucceededCalls).isEqualTo(1)
    }

    // Utility functions for test setup
    private fun linkAccountWithInlineConsent(consentSection: ConsentUi.ConsentSection = mock()) =
        TestFactory.LINK_ACCOUNT.copy(
            linkAuthIntentInfo = LinkAuthIntentInfo("auth_123", ConsentPresentation.Inline(consentSection))
        )

    private fun linkAccountWithFullscreenConsent() =
        TestFactory.LINK_ACCOUNT.copy(
            linkAuthIntentInfo = LinkAuthIntentInfo("auth_123", ConsentPresentation.FullScreen(mock()))
        )

    private fun createViewModel(
        linkAccount: LinkAccount = TestFactory.LINK_ACCOUNT,
        linkAccountHolder: LinkAccountHolder = LinkAccountHolder(SavedStateHandle()),
        linkAccountManager: LinkAccountManager = FakeLinkAccountManager(),
        linkEventsReporter: LinkEventsReporter = FakeLinkEventsReporter(),
        logger: Logger = FakeLogger(),
        linkLaunchMode: LinkLaunchMode = LinkLaunchMode.PaymentMethodSelection(null),
        webLinkAuthChannel: WebLinkAuthChannel = WebLinkAuthChannel(),
        onVerificationSucceeded: (refresh: ConsumerSessionRefresh?) -> Unit = {},
        onChangeEmailRequested: () -> Unit = {},
        onDismissClicked: () -> Unit = {},
        dismissWithResult: (LinkActivityResult) -> Unit = { },
    ): VerificationViewModel {
        return VerificationViewModel(
            linkAccount = linkAccount,
            linkAccountHolder = linkAccountHolder,
            linkAccountManager = linkAccountManager,
            linkEventsReporter = linkEventsReporter,
            logger = logger,
            linkLaunchMode = linkLaunchMode,
            webLinkAuthChannel = webLinkAuthChannel,
            isDialog = false,
            onVerificationSucceeded = onVerificationSucceeded,
            onChangeEmailRequested = onChangeEmailRequested,
            onDismissClicked = onDismissClicked,
            dismissWithResult = dismissWithResult
        )
    }
}
