package com.stripe.android.link.ui.verification

import app.cash.turbine.Turbine
import com.google.common.truth.Truth.assertThat
import com.stripe.android.core.StripeError
import com.stripe.android.core.exception.APIException
import com.stripe.android.core.strings.resolvableString
import com.stripe.android.link.LinkAccountUpdate
import com.stripe.android.link.TestFactory
import com.stripe.android.link.account.FakeLinkAccountManager
import com.stripe.android.link.account.FakeLinkAccountManager.ConfirmVerificationCall
import com.stripe.android.link.account.FakeLinkAccountManager.StartVerificationCall
import com.stripe.android.link.analytics.FakeLinkEventsReporter
import com.stripe.android.link.model.LinkAccount
import com.stripe.android.link.ui.verification.LinkAuthFlowState.Action
import com.stripe.android.link.ui.verification.LinkAuthFlowState.Screen
import com.stripe.android.model.ConsumerSession.AuthenticationLevel
import com.stripe.android.model.ConsumerSession.VerificationFactor
import com.stripe.android.model.ConsumerSession.VerificationFactor.FactorType
import com.stripe.android.model.ConsumerSession.VerificationSession
import com.stripe.android.model.VerificationType
import com.stripe.android.paymentsheet.R
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
internal class LinkAuthFlowTest {

    @Test
    fun `does not send a code before start`() = runScenario {
        assertThat(flow.state.value.screen).isEqualTo(Screen.Otp)
        assertThat(flow.state.value.isLoading).isTrue()
        assertThat(flow.state.value.canSubmitCode).isFalse()
    }

    @Test
    fun `start sends an SMS code when SMS is the first usable factor`() = runScenario {
        flow.start()

        assertThat(accountManager.awaitStartVerificationCall()).isEqualTo(smsStart(isResend = false))
        val state = flow.state.value
        assertThat(state.screen).isEqualTo(Screen.Otp)
        assertThat(state.verificationType).isEqualTo(VerificationType.SMS)
        assertThat(state.recipient).isEqualTo("+1 (•••) •••-••42")
        assertThat(state.canSubmitCode).isTrue()
        assertThat(state.canGoBack).isFalse()
        assertThat(state.actions).containsExactly(Action.Resend, Action.Email).inOrder()
    }

    @Test
    fun `start reuses an SMS session that was already started`() = runScenario(
        initial = AccountSpec(sessions = listOf(startedSession(VerificationSession.SessionType.Sms))),
    ) {
        flow.start()

        assertThat(flow.state.value.screen).isEqualTo(Screen.Otp)
        assertThat(flow.state.value.verificationType).isEqualTo(VerificationType.SMS)
        assertThat(flow.state.value.canSubmitCode).isTrue()
    }

    @Test
    fun `start completes immediately when the account is already verified`() = runScenario(
        initial = AccountSpec(current = AuthenticationLevel.OneFactorAuthentication),
    ) {
        flow.start()

        assertThat(results.awaitItem()).isEqualTo(LinkAuthFlowResult.Completed)
    }

    @Test
    fun `start requires web auth when the session requires a webview`() = runScenario(
        initial = AccountSpec(webviewRequired = true),
    ) {
        flow.start()

        assertThat(results.awaitItem()).isEqualTo(LinkAuthFlowResult.RequiresWebAuth)
    }

    @Test
    fun `email-only account asks for the phone number before sending a code`() = runScenario(
        initial = AccountSpec(factors = listOf(EMAIL_FACTOR), emailOtpRequiresAdditionalInfo = true),
    ) {
        flow.start()

        assertThat(flow.state.value.screen).isEqualTo(Screen.PhoneMatch)
        assertThat(flow.state.value.phoneNumberLastTwoDigits).isEqualTo("42")

        flow.submitPhoneNumber(PHONE_NUMBER)

        assertThat(accountManager.awaitStartVerificationCall()).isEqualTo(emailStart(isResend = false))
        val state = flow.state.value
        assertThat(state.screen).isEqualTo(Screen.Otp)
        assertThat(state.verificationType).isEqualTo(VerificationType.EMAIL)
        assertThat(state.recipient).isEqualTo(TestFactory.EMAIL)
        assertThat(state.actions).containsExactly(Action.Resend)
    }

    @Test
    fun `phone match is shown when the requirement is unknown`() = runScenario(
        initial = AccountSpec(factors = listOf(EMAIL_FACTOR), emailOtpRequiresAdditionalInfo = null),
    ) {
        flow.start()

        assertThat(flow.state.value.screen).isEqualTo(Screen.PhoneMatch)
    }

    @Test
    fun `email code is sent directly when no additional info is required`() = runScenario(
        initial = AccountSpec(factors = listOf(EMAIL_FACTOR), emailOtpRequiresAdditionalInfo = false),
    ) {
        flow.start()

        assertThat(accountManager.awaitStartVerificationCall()).isEqualTo(
            StartVerificationCall(type = VerificationType.EMAIL, accountPhoneNumber = null, isResend = false)
        )
        assertThat(flow.state.value.screen).isEqualTo(Screen.Otp)
    }

    @Test
    fun `temporarily disabled SMS factor falls through to email`() = runScenario(
        initial = AccountSpec(
            factors = listOf(SMS_FACTOR.copy(temporarilyDisabled = true), EMAIL_FACTOR),
            emailOtpRequiresAdditionalInfo = false,
        ),
    ) {
        flow.start()

        assertThat(accountManager.awaitStartVerificationCall().type).isEqualTo(VerificationType.EMAIL)
    }

    @Test
    fun `unknown factors are ignored and the flow is blocked`() = runScenario(
        initial = AccountSpec(factors = listOf(SMS_FACTOR.copy(type = FactorType.Unknown))),
    ) {
        flow.start()

        assertThat(flow.state.value.screen).isEqualTo(Screen.Blocked)
        assertThat(flow.state.value.errorMessage).isEqualTo(resolvableString(R.string.stripe_link_auth_blocked))
    }

    @Test
    fun `factors outside the declared capabilities are not used`() = runScenario(
        initial = AccountSpec(factors = listOf(EMAIL_FACTOR)),
        capabilities = listOf(VerificationType.SMS),
    ) {
        flow.start()

        assertThat(flow.state.value.screen).isEqualTo(Screen.Blocked)
    }

    @Test
    fun `confirm completes when the minimum authentication level is reached`() = runScenario {
        flow.start()
        accountManager.awaitStartVerificationCall()

        flow.confirm(CODE)

        assertThat(accountManager.awaitConfirmVerificationCall()).isEqualTo(smsConfirm())
        assertThat(results.awaitItem()).isEqualTo(LinkAuthFlowResult.Completed)
    }

    @Test
    fun `MFA continues with email after SMS is confirmed`() = runScenario(
        initial = AccountSpec(
            minimum = AuthenticationLevel.TwoFactorAuthentication,
            emailOtpRequiresAdditionalInfo = false,
        ),
    ) {
        flow.start()
        accountManager.awaitStartVerificationCall()

        flow.confirm(CODE)

        assertThat(accountManager.awaitConfirmVerificationCall()).isEqualTo(smsConfirm())
        assertThat(accountManager.awaitStartVerificationCall().type).isEqualTo(VerificationType.EMAIL)
        val state = flow.state.value
        assertThat(state.step).isEqualTo(2)
        assertThat(state.verificationType).isEqualTo(VerificationType.EMAIL)
        assertThat(state.canGoBack).isFalse()
        assertThat(state.actions).containsExactly(Action.Resend)

        flow.confirm(CODE)

        assertThat(accountManager.awaitConfirmVerificationCall().type).isEqualTo(VerificationType.EMAIL)
        assertThat(results.awaitItem()).isEqualTo(LinkAuthFlowResult.Completed)
    }

    @Test
    fun `MFA continues with SMS after email is confirmed`() = runScenario(
        initial = AccountSpec(
            factors = listOf(SMS_FACTOR.copy(temporarilyDisabled = true), EMAIL_FACTOR),
            minimum = AuthenticationLevel.TwoFactorAuthentication,
            emailOtpRequiresAdditionalInfo = false,
        ),
    ) {
        flow.start()
        assertThat(accountManager.awaitStartVerificationCall().type).isEqualTo(VerificationType.EMAIL)
        accountState.onConfirm = { it.copy(factors = listOf(SMS_FACTOR, EMAIL_FACTOR)) }

        flow.confirm(CODE)

        accountManager.awaitConfirmVerificationCall()
        assertThat(accountManager.awaitStartVerificationCall().type).isEqualTo(VerificationType.SMS)
        assertThat(flow.state.value.step).isEqualTo(2)
    }

    @Test
    fun `legacy sessions without factors block instead of looping after confirm`() = runScenario(
        initial = AccountSpec(factors = null, minimum = AuthenticationLevel.TwoFactorAuthentication),
    ) {
        flow.start()
        assertThat(accountManager.awaitStartVerificationCall()).isEqualTo(smsStart(isResend = false))

        flow.confirm(CODE)

        accountManager.awaitConfirmVerificationCall()
        assertThat(flow.state.value.screen).isEqualTo(Screen.Blocked)
    }

    @Test
    fun `email code action shows phone match and back restores SMS without resending`() = runScenario {
        flow.start()
        accountManager.awaitStartVerificationCall()

        flow.sendToEmail()

        assertThat(flow.state.value.screen).isEqualTo(Screen.PhoneMatch)
        assertThat(flow.state.value.canGoBack).isTrue()

        flow.goBack()

        val state = flow.state.value
        assertThat(state.screen).isEqualTo(Screen.Otp)
        assertThat(state.verificationType).isEqualTo(VerificationType.SMS)
        assertThat(state.canSubmitCode).isTrue()
        assertThat(state.canGoBack).isFalse()
    }

    @Test
    fun `back from email code returns to phone match then SMS`() = runScenario {
        flow.start()
        accountManager.awaitStartVerificationCall()
        flow.sendToEmail()
        flow.submitPhoneNumber(PHONE_NUMBER)
        assertThat(accountManager.awaitStartVerificationCall()).isEqualTo(emailStart(isResend = false))
        assertThat(flow.state.value.canGoBack).isTrue()

        flow.goBack()

        assertThat(flow.state.value.screen).isEqualTo(Screen.PhoneMatch)

        flow.goBack()

        assertThat(flow.state.value.screen).isEqualTo(Screen.Otp)
        assertThat(flow.state.value.verificationType).isEqualTo(VerificationType.SMS)
    }

    @Test
    fun `switching to email again reuses the started email challenge`() = runScenario {
        flow.start()
        accountManager.awaitStartVerificationCall()
        flow.sendToEmail()
        flow.submitPhoneNumber(PHONE_NUMBER)
        accountManager.awaitStartVerificationCall()
        flow.goBack()
        flow.goBack()

        flow.sendToEmail()

        assertThat(flow.state.value.screen).isEqualTo(Screen.Otp)
        assertThat(flow.state.value.verificationType).isEqualTo(VerificationType.EMAIL)
        assertThat(flow.state.value.canSubmitCode).isTrue()
    }

    @Test
    fun `email resend reuses the phone number and starts a cooldown`() = runScenario(
        initial = AccountSpec(factors = listOf(EMAIL_FACTOR), emailOtpRequiresAdditionalInfo = true),
    ) {
        flow.start()
        flow.submitPhoneNumber(PHONE_NUMBER)
        accountManager.awaitStartVerificationCall()

        flow.resend()

        assertThat(eventsReporter.resendCalls.awaitItem()).isEqualTo(VerificationType.EMAIL.value)
        assertThat(accountManager.awaitStartVerificationCall()).isEqualTo(emailStart(isResend = true))
        assertThat(flow.state.value.resendSecondsRemaining).isEqualTo(10)
        assertThat(flow.state.value.canResend).isFalse()

        testScope.advanceTimeBy(4_000)
        testScope.runCurrent()

        assertThat(flow.state.value.resendSecondsRemaining).isEqualTo(6)

        testScope.advanceTimeBy(6_000)
        testScope.runCurrent()

        assertThat(flow.state.value.resendSecondsRemaining).isEqualTo(0)
        assertThat(flow.state.value.canResend).isTrue()
    }

    @Test
    fun `failed resend does not start a cooldown`() = runScenario {
        flow.start()
        accountManager.awaitStartVerificationCall()
        accountManager.startVerificationResultProvider = { Result.failure(APIException(message = "Nope")) }

        flow.resend()

        assertThat(eventsReporter.resendCalls.awaitItem()).isEqualTo(VerificationType.SMS.value)
        assertThat(accountManager.awaitStartVerificationCall()).isEqualTo(smsStart(isResend = true))
        val state = flow.state.value
        assertThat(state.errorMessage).isEqualTo("Nope".resolvableString)
        assertThat(state.resendSecondsRemaining).isEqualTo(0)
        assertThat(state.canResend).isTrue()
    }

    @Test
    fun `back preserves the cooldown of the previous challenge`() = runScenario {
        flow.start()
        accountManager.awaitStartVerificationCall()
        flow.resend()
        eventsReporter.resendCalls.awaitItem()
        accountManager.awaitStartVerificationCall()

        flow.sendToEmail()
        flow.goBack()

        assertThat(flow.state.value.verificationType).isEqualTo(VerificationType.SMS)
        assertThat(flow.state.value.resendSecondsRemaining).isEqualTo(10)
        assertThat(flow.state.value.canResend).isFalse()
    }

    @Test
    fun `phone number mismatch returns to phone match with an error`() = runScenario {
        flow.start()
        accountManager.awaitStartVerificationCall()
        flow.sendToEmail()
        accountManager.startVerificationResultProvider = {
            Result.failure(stripeException(code = "phone_number_mismatch", message = "Wrong number"))
        }

        flow.submitPhoneNumber(PHONE_NUMBER)

        accountManager.awaitStartVerificationCall()
        assertThat(flow.state.value.screen).isEqualTo(Screen.PhoneMatch)
        assertThat(flow.state.value.errorMessage).isEqualTo("Wrong number".resolvableString)
    }

    @Test
    fun `confirm failure shows the error and resets the code`() = runScenario {
        flow.start()
        accountManager.awaitStartVerificationCall()
        val inputRevision = flow.state.value.inputRevision
        accountManager.confirmVerificationResultProvider = {
            Result.failure(stripeException(code = "consumer_verification_code_invalid", message = "Bad code"))
        }

        flow.confirm(CODE)

        accountManager.awaitConfirmVerificationCall()
        assertThat(flow.state.value.errorMessage).isEqualTo("Bad code".resolvableString)
        assertThat(flow.state.value.inputRevision).isGreaterThan(inputRevision)
        assertThat(flow.state.value.canSubmitCode).isTrue()
    }

    @Test
    fun `max attempts disables code entry but keeps resend`() = runScenario {
        flow.start()
        accountManager.awaitStartVerificationCall()
        accountManager.confirmVerificationResultProvider = {
            Result.failure(stripeException(code = "consumer_verification_max_attempts_exceeded", message = "Max"))
        }

        flow.confirm(CODE)

        accountManager.awaitConfirmVerificationCall()
        assertThat(flow.state.value.canSubmitCode).isFalse()
        assertThat(flow.state.value.canResend).isTrue()
        assertThat(flow.state.value.errorMessage).isEqualTo("Max".resolvableString)
    }

    @Test
    fun `expired code sends a new code automatically`() = runScenario {
        flow.start()
        accountManager.awaitStartVerificationCall()
        accountManager.confirmVerificationResultProvider = {
            Result.failure(stripeException(code = "consumer_verification_expired", message = "Expired"))
        }

        flow.confirm(CODE)

        accountManager.awaitConfirmVerificationCall()
        assertThat(accountManager.awaitStartVerificationCall()).isEqualTo(smsStart(isResend = false))
        assertThat(flow.state.value.canSubmitCode).isTrue()
    }

    @Test
    fun `expired session is recovered and the request replayed`() = runScenario {
        flow.start()
        accountManager.awaitStartVerificationCall()
        val defaultConfirm = accountManager.confirmVerificationResultProvider
        var attempts = 0
        accountManager.confirmVerificationResultProvider = { call ->
            attempts += 1
            if (attempts == 1) {
                Result.failure(stripeException(code = "consumer_session_expired", message = "Expired"))
            } else {
                defaultConfirm(call)
            }
        }
        accountManager.recoverSessionResult = Result.success(accountState.spec.toLinkAccount())

        flow.confirm(CODE)

        assertThat(accountManager.awaitConfirmVerificationCall()).isEqualTo(smsConfirm())
        accountManager.awaitRecoverSessionCall()
        assertThat(accountManager.awaitConfirmVerificationCall()).isEqualTo(smsConfirm())
        assertThat(results.awaitItem()).isEqualTo(LinkAuthFlowResult.Completed)
    }

    @Test
    fun `failed session recovery finishes with the original error`() = runScenario {
        flow.start()
        accountManager.awaitStartVerificationCall()
        val error = stripeException(code = "consumer_session_credentials_invalid", message = "Invalid")
        accountManager.confirmVerificationResultProvider = { Result.failure(error) }
        accountManager.recoverSessionResult = Result.failure(APIException(message = "Lookup failed"))

        flow.confirm(CODE)

        accountManager.awaitConfirmVerificationCall()
        accountManager.awaitRecoverSessionCall()
        assertThat(results.awaitItem()).isEqualTo(LinkAuthFlowResult.Failed(error))
    }

    @Test
    fun `cancel reports canceled and ignores late responses`() = runScenario {
        flow.start()
        accountManager.awaitStartVerificationCall()
        val response = CompletableDeferred<Result<LinkAccount>>()
        accountManager.confirmVerificationResultProvider = { response.await() }
        flow.confirm(CODE)
        accountManager.awaitConfirmVerificationCall()

        flow.cancel(switchAccount = false)

        eventsReporter.cancelCalls.awaitItem()
        assertThat(results.awaitItem()).isEqualTo(LinkAuthFlowResult.Canceled)

        response.complete(Result.success(accountState.spec.copy(current = MINIMUM_REACHED).toLinkAccount()))
    }

    @Test
    fun `cancel with switch account reports switch account`() = runScenario {
        flow.cancel(switchAccount = true)

        eventsReporter.cancelCalls.awaitItem()
        assertThat(results.awaitItem()).isEqualTo(LinkAuthFlowResult.SwitchAccount)
    }

    private fun runScenario(
        initial: AccountSpec = AccountSpec(),
        capabilities: List<VerificationType> = listOf(VerificationType.SMS, VerificationType.EMAIL),
        block: suspend Scenario.() -> Unit,
    ) = runTest(UnconfinedTestDispatcher()) {
        val accountState = AccountState(spec = initial)
        val accountManager = FakeLinkAccountManager()
        accountManager.setsAccountOnVerification = true
        accountManager.linkAccountHolder.set(LinkAccountUpdate.Value(initial.toLinkAccount()))
        accountManager.startVerificationResultProvider = { call ->
            accountState.spec = accountState.spec.copy(
                sessions = listOf(startedSession(call.type.sessionType)),
            )
            Result.success(accountState.spec.toLinkAccount())
        }
        accountManager.confirmVerificationResultProvider = {
            accountState.spec = accountState.onConfirm(
                accountState.spec.copy(current = accountState.spec.current.next(), sessions = emptyList())
            )
            Result.success(accountState.spec.toLinkAccount())
        }
        val eventsReporter = AuthFlowEventsReporter()
        val results = Turbine<LinkAuthFlowResult>()

        val flow = LinkAuthFlow(
            linkAccountManager = accountManager,
            linkEventsReporter = eventsReporter,
            capabilities = capabilities,
            consentGranted = { null },
            scope = backgroundScope,
            now = { testScheduler.currentTime },
            onFinish = { results.add(it) },
        )

        Scenario(
            flow = flow,
            accountManager = accountManager,
            accountState = accountState,
            eventsReporter = eventsReporter,
            results = results,
            testScope = this,
        ).block()

        accountManager.ensureVerificationEventsConsumed()
        eventsReporter.ensureAllEventsConsumed()
        results.ensureAllEventsConsumed()
    }

    private class Scenario(
        val flow: LinkAuthFlow,
        val accountManager: FakeLinkAccountManager,
        val accountState: AccountState,
        val eventsReporter: AuthFlowEventsReporter,
        val results: Turbine<LinkAuthFlowResult>,
        val testScope: TestScope,
    )

    private class AccountState(
        var spec: AccountSpec,
        var onConfirm: (AccountSpec) -> AccountSpec = { it },
    )

    private data class AccountSpec(
        val factors: List<VerificationFactor>? = listOf(SMS_FACTOR, EMAIL_FACTOR),
        val current: AuthenticationLevel = AuthenticationLevel.NotAuthenticated,
        val minimum: AuthenticationLevel = AuthenticationLevel.OneFactorAuthentication,
        val sessions: List<VerificationSession> = emptyList(),
        val emailOtpRequiresAdditionalInfo: Boolean? = true,
        val webviewRequired: Boolean = false,
    ) {
        fun toLinkAccount() = LinkAccount(
            TestFactory.CONSUMER_SESSION.copy(
                verificationSessions = sessions,
                availableVerificationFactors = factors,
                currentAuthenticationLevel = current,
                minimumAuthenticationLevel = minimum,
                emailOtpRequiresAdditionalInfo = emailOtpRequiresAdditionalInfo,
                mobileFallbackWebviewParams = TestFactory.MOBILE_FALLBACK_WEBVIEW_PARAMS.takeIf { webviewRequired },
            )
        )
    }

    private class AuthFlowEventsReporter : FakeLinkEventsReporter() {
        val cancelCalls = Turbine<Unit>()
        val resendCalls = Turbine<String>()

        override fun on2FACancel() {
            cancelCalls.add(Unit)
        }

        override fun on2FAResendCode(verificationType: String) {
            resendCalls.add(verificationType)
        }

        fun ensureAllEventsConsumed() {
            cancelCalls.ensureAllEventsConsumed()
            resendCalls.ensureAllEventsConsumed()
        }
    }

    private companion object {
        const val CODE = "123456"
        const val PHONE_NUMBER = "+15555555542"
        val MINIMUM_REACHED = AuthenticationLevel.OneFactorAuthentication

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

        fun smsStart(isResend: Boolean) = StartVerificationCall(
            type = VerificationType.SMS,
            accountPhoneNumber = null,
            isResend = isResend,
        )

        fun emailStart(isResend: Boolean) = StartVerificationCall(
            type = VerificationType.EMAIL,
            accountPhoneNumber = PHONE_NUMBER,
            isResend = isResend,
        )

        fun smsConfirm() = ConfirmVerificationCall(
            code = CODE,
            type = VerificationType.SMS,
            consentGranted = null,
        )

        fun startedSession(type: VerificationSession.SessionType) = VerificationSession(
            type = type,
            state = VerificationSession.SessionState.Started,
        )

        fun stripeException(code: String, message: String) = APIException(
            stripeError = StripeError(code = code, message = message),
        )

        val VerificationType.sessionType: VerificationSession.SessionType
            get() = when (this) {
                VerificationType.SMS -> VerificationSession.SessionType.Sms
                VerificationType.EMAIL -> VerificationSession.SessionType.Email
            }

        fun AuthenticationLevel.next(): AuthenticationLevel = when (this) {
            AuthenticationLevel.NotAuthenticated -> AuthenticationLevel.OneFactorAuthentication
            else -> AuthenticationLevel.TwoFactorAuthentication
        }
    }
}
