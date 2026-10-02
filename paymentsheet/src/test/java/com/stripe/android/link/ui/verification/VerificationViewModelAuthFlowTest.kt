package com.stripe.android.link.ui.verification

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.Turbine
import com.google.common.truth.Truth.assertThat
import com.stripe.android.core.utils.FeatureFlags
import com.stripe.android.link.LinkAccountUpdate
import com.stripe.android.link.LinkActivityResult
import com.stripe.android.link.LinkLaunchMode
import com.stripe.android.link.TestFactory
import com.stripe.android.link.WebLinkAuthChannel
import com.stripe.android.link.account.FakeLinkAccountManager
import com.stripe.android.link.account.FakeLinkAccountManager.ConfirmVerificationCall
import com.stripe.android.link.account.FakeLinkAccountManager.StartVerificationCall
import com.stripe.android.link.account.LinkAccountHolder
import com.stripe.android.link.analytics.FakeLinkEventsReporter
import com.stripe.android.link.model.LinkAccount
import com.stripe.android.link.ui.verification.LinkAuthFlowState.Action
import com.stripe.android.link.ui.verification.LinkAuthFlowState.Screen
import com.stripe.android.model.ConsumerSession.AuthenticationLevel
import com.stripe.android.model.ConsumerSession.VerificationFactor
import com.stripe.android.model.ConsumerSession.VerificationFactor.FactorType
import com.stripe.android.model.ConsumerSession.VerificationSession
import com.stripe.android.model.ConsumerSessionRefresh
import com.stripe.android.model.LinkBrand
import com.stripe.android.model.VerificationType
import com.stripe.android.paymentsheet.utils.ViewModelStoreTestRule
import com.stripe.android.testing.CoroutineTestRule
import com.stripe.android.testing.FakeLogger
import com.stripe.android.testing.FeatureFlagTestRule
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
internal class VerificationViewModelAuthFlowTest {
    private val dispatcher = UnconfinedTestDispatcher()

    @get:Rule
    val coroutineTestRule = CoroutineTestRule(dispatcher)

    @get:Rule
    val viewModelStoreRule = ViewModelStoreTestRule()

    @get:Rule
    val featureFlagRule = FeatureFlagTestRule(
        featureFlag = FeatureFlags.linkEmailOtpAndMfa,
        isEnabled = true,
    )

    @Test
    fun `init sends an SMS code and exposes the auth flow state`() = runScenario {
        assertThat(accountManager.awaitStartVerificationCall()).isEqualTo(SMS_START)
        val state = viewModel.viewState.value
        assertThat(state.isProcessing).isFalse()
        assertThat(state.authFlow?.screen).isEqualTo(Screen.Otp)
        assertThat(state.authFlow?.recipient).isEqualTo("+1 (•••) •••-••42")
        assertThat(state.authFlow?.codeEntryEnabled).isTrue()
        assertThat(state.authFlow?.actions).containsExactly(Action.Resend, Action.Email).inOrder()
        assertThat(state.authFlow?.canGoBack).isFalse()
        assertThat(backHandlers.awaitItem()).isNull()
    }

    @Test
    fun `verification only succeeds once every required factor is confirmed`() = runScenario(
        minimum = AuthenticationLevel.TwoFactorAuthentication,
        emailOtpRequiresAdditionalInfo = false,
    ) {
        accountManager.awaitStartVerificationCall()
        backHandlers.awaitItem()

        viewModel.otpElement.controller.onValueChanged(0, CODE)

        assertThat(accountManager.awaitConfirmVerificationCall()).isEqualTo(
            ConfirmVerificationCall(code = CODE, type = VerificationType.SMS, consentGranted = null)
        )
        assertThat(accountManager.awaitStartVerificationCall().type).isEqualTo(VerificationType.EMAIL)
        assertThat(viewModel.viewState.value.authFlow?.recipient).isEqualTo(TestFactory.EMAIL)
        assertThat(viewModel.otpElement.controller.fieldValue.value).isEmpty()
        verificationSucceededCalls.expectNoEvents()

        viewModel.otpElement.controller.onValueChanged(0, CODE)

        assertThat(accountManager.awaitConfirmVerificationCall().type).isEqualTo(VerificationType.EMAIL)
        verificationSucceededCalls.awaitItem()
    }

    @Test
    fun `email code shows phone match and sends the entered number`() = runScenario {
        accountManager.awaitStartVerificationCall()
        backHandlers.awaitItem()

        viewModel.onEmailCodeClicked()

        assertThat(viewModel.viewState.value.authFlow?.screen).isEqualTo(Screen.PhoneMatch)
        assertThat(viewModel.viewState.value.authFlow?.phoneNumberLastTwoDigits).isEqualTo("42")
        assertThat(backHandlers.awaitItem()).isNotNull()

        viewModel.phoneNumberController.onRawValueChange("5555555542")
        viewModel.onPhoneNumberSubmitted()

        assertThat(accountManager.awaitStartVerificationCall()).isEqualTo(
            StartVerificationCall(type = VerificationType.EMAIL, accountPhoneNumber = "+15555555542", isResend = false)
        )
        // Back is unavailable while the email code is being sent, then returns to phone match.
        assertThat(backHandlers.awaitItem()).isNull()
        assertThat(backHandlers.awaitItem()).isNotNull()
        assertThat(viewModel.viewState.value.authFlow?.screen).isEqualTo(Screen.Otp)
        assertThat(viewModel.viewState.value.authFlow?.recipient).isEqualTo(TestFactory.EMAIL)
    }

    @Test
    fun `registered back handler returns to the previous challenge`() = runScenario {
        accountManager.awaitStartVerificationCall()
        assertThat(backHandlers.awaitItem()).isNull()
        viewModel.onEmailCodeClicked()
        val handler = backHandlers.awaitItem()

        requireNotNull(handler).invoke()

        assertThat(viewModel.viewState.value.authFlow?.screen).isEqualTo(Screen.Otp)
        assertThat(viewModel.viewState.value.authFlow?.canGoBack).isFalse()
        assertThat(backHandlers.awaitItem()).isNull()
        assertThat(viewModel.onNavigateBack()).isFalse()
    }

    @Test
    fun `onBack cancels the flow and dismisses`() = runScenario {
        accountManager.awaitStartVerificationCall()
        backHandlers.awaitItem()

        viewModel.onBack()

        eventsReporter.cancelCalls.awaitItem()
        dismissCalls.awaitItem()
    }

    @Test
    fun `change email cancels the flow and requests a new email`() = runScenario {
        accountManager.awaitStartVerificationCall()
        backHandlers.awaitItem()

        viewModel.onChangeEmailButtonClicked()

        eventsReporter.cancelCalls.awaitItem()
        changeEmailCalls.awaitItem()
    }

    @Test
    fun `authentication mode dismisses with completed once verified`() = runScenario(
        linkLaunchMode = LinkLaunchMode.Authentication(),
    ) {
        accountManager.awaitStartVerificationCall()
        backHandlers.awaitItem()

        viewModel.otpElement.controller.onValueChanged(0, CODE)

        accountManager.awaitConfirmVerificationCall()
        assertThat(dismissWithResultCalls.awaitItem()).isInstanceOf(LinkActivityResult.Completed::class.java)
    }

    private fun runScenario(
        minimum: AuthenticationLevel = AuthenticationLevel.OneFactorAuthentication,
        emailOtpRequiresAdditionalInfo: Boolean? = true,
        linkLaunchMode: LinkLaunchMode = LinkLaunchMode.PaymentMethodSelection(null),
        block: suspend Scenario.() -> Unit,
    ) = runTest(dispatcher) {
        val accountManager = createAccountManager(minimum, emailOtpRequiresAdditionalInfo)
        val eventsReporter = AuthFlowEventsReporter()
        val backHandlers = Turbine<(() -> Unit)?>()
        val verificationSucceededCalls = Turbine<ConsumerSessionRefresh?>()
        val changeEmailCalls = Turbine<Unit>()
        val dismissCalls = Turbine<Unit>()
        val dismissWithResultCalls = Turbine<LinkActivityResult>()

        val viewModel = VerificationViewModel(
            linkAccount = requireNotNull(accountManager.linkAccountHolder.linkAccountInfo.value.account),
            linkAccountHolder = LinkAccountHolder(SavedStateHandle()),
            linkAccountManager = accountManager,
            linkEventsReporter = eventsReporter,
            logger = FakeLogger(),
            linkLaunchMode = linkLaunchMode,
            webLinkAuthChannel = WebLinkAuthChannel(),
            isDialog = false,
            linkBrand = LinkBrand.Link,
            onVerificationSucceeded = { verificationSucceededCalls.add(it) },
            setScreenBackHandler = { backHandlers.add(it) },
            onChangeEmailRequested = { changeEmailCalls.add(Unit) },
            onDismissClicked = { dismissCalls.add(Unit) },
            dismissWithResult = { dismissWithResultCalls.add(it) },
        ).also { viewModelStoreRule.track(it) }

        Scenario(
            viewModel = viewModel,
            accountManager = accountManager,
            eventsReporter = eventsReporter,
            backHandlers = backHandlers,
            verificationSucceededCalls = verificationSucceededCalls,
            changeEmailCalls = changeEmailCalls,
            dismissCalls = dismissCalls,
            dismissWithResultCalls = dismissWithResultCalls,
        ).block()

        accountManager.ensureVerificationEventsConsumed()
        eventsReporter.cancelCalls.ensureAllEventsConsumed()
        backHandlers.ensureAllEventsConsumed()
        verificationSucceededCalls.ensureAllEventsConsumed()
        changeEmailCalls.ensureAllEventsConsumed()
        dismissCalls.ensureAllEventsConsumed()
        dismissWithResultCalls.ensureAllEventsConsumed()
    }

    /**
     * A fake whose start/confirm responses track a server-side session: start marks a STARTED session of the requested
     * type, and each confirm raises the authentication level by one factor.
     */
    private fun createAccountManager(
        minimum: AuthenticationLevel,
        emailOtpRequiresAdditionalInfo: Boolean?,
    ): FakeLinkAccountManager {
        var current = AuthenticationLevel.NotAuthenticated
        var sessions = emptyList<VerificationSession>()
        fun account() = LinkAccount(
            TestFactory.CONSUMER_SESSION.copy(
                verificationSessions = sessions,
                availableVerificationFactors = listOf(SMS_FACTOR, EMAIL_FACTOR),
                currentAuthenticationLevel = current,
                minimumAuthenticationLevel = minimum,
                emailOtpRequiresAdditionalInfo = emailOtpRequiresAdditionalInfo,
            )
        )

        val accountManager = FakeLinkAccountManager()
        accountManager.setsAccountOnVerification = true
        accountManager.linkAccountHolder.set(LinkAccountUpdate.Value(account()))
        accountManager.startVerificationResultProvider = { call ->
            val type = when (call.type) {
                VerificationType.SMS -> VerificationSession.SessionType.Sms
                VerificationType.EMAIL -> VerificationSession.SessionType.Email
            }
            sessions = listOf(VerificationSession(type, VerificationSession.SessionState.Started))
            Result.success(account())
        }
        accountManager.confirmVerificationResultProvider = {
            current = if (current == AuthenticationLevel.NotAuthenticated) {
                AuthenticationLevel.OneFactorAuthentication
            } else {
                AuthenticationLevel.TwoFactorAuthentication
            }
            sessions = emptyList()
            Result.success(account())
        }
        return accountManager
    }

    private class Scenario(
        val viewModel: VerificationViewModel,
        val accountManager: FakeLinkAccountManager,
        val eventsReporter: AuthFlowEventsReporter,
        val backHandlers: Turbine<(() -> Unit)?>,
        val verificationSucceededCalls: Turbine<ConsumerSessionRefresh?>,
        val changeEmailCalls: Turbine<Unit>,
        val dismissCalls: Turbine<Unit>,
        val dismissWithResultCalls: Turbine<LinkActivityResult>,
    )

    private class AuthFlowEventsReporter : FakeLinkEventsReporter() {
        val cancelCalls = Turbine<Unit>()

        override fun on2FACancel() {
            cancelCalls.add(Unit)
        }
    }

    private companion object {
        const val CODE = "123456"

        val SMS_START = StartVerificationCall(type = VerificationType.SMS, accountPhoneNumber = null, isResend = false)

        val SMS_FACTOR = VerificationFactor(
            type = FactorType.Sms,
            id = "sms_1",
            providesFurtherVerification = true,
            temporarilyDisabled = false,
        )
        val EMAIL_FACTOR = VerificationFactor(
            type = FactorType.Email,
            id = "email_1",
            providesFurtherVerification = true,
            temporarilyDisabled = false,
        )
    }
}
