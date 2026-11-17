package com.stripe.android.paymentelement.embedded.linkinline

import app.cash.turbine.Turbine
import com.google.common.truth.Truth.assertThat
import com.stripe.android.link.LinkAccountUpdate
import com.stripe.android.link.TestFactory
import com.stripe.android.link.account.FakeLinkAccountManager
import com.stripe.android.link.analytics.FakeLinkEventsReporter
import com.stripe.android.link.model.LinkAccount
import com.stripe.android.link.ui.verification.LinkAuthFlow
import com.stripe.android.link.ui.verification.LinkAuthFlowState
import com.stripe.android.model.ConsumerSession.AuthenticationLevel
import com.stripe.android.model.ConsumerSession.VerificationFactor
import com.stripe.android.model.ConsumerSession.VerificationFactor.FactorType
import com.stripe.android.model.ConsumerSession.VerificationSession
import com.stripe.android.model.VerificationType
import com.stripe.android.paymentelement.embedded.linkinline.EmbeddedLinkInlineVerificationEligibility.IneligibleReason
import com.stripe.android.paymentelement.embedded.linkinline.EmbeddedLinkInlineVerificationEligibility.Inputs
import com.stripe.android.paymentelement.embedded.linkinline.EmbeddedLinkInlineVerificationEligibility.Result
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
internal class EmbeddedLinkInlineVerificationEligibilityTest {

    @Test
    fun `returning customer who needs to verify is eligible with an SMS code step`() = runScenario {
        val result = evaluate()

        assertThat(result).isInstanceOf(Result.Eligible::class.java)
        val eligible = result as Result.Eligible
        assertThat(eligible.linkAccount).isSameInstanceAs(account)
        val state = eligible.authFlow.state.value
        assertThat(state.screen).isEqualTo(LinkAuthFlowState.Screen.Otp)
        assertThat(state.verificationType).isEqualTo(VerificationType.SMS)
        assertThat(eligible.authFlow.hasStarted).isFalse()
        prepareCalls.awaitItem()
    }

    @Test
    fun `server flag off is disabled_by_server before every other reason`() = runScenario(
        account = null,
    ) {
        val result = evaluate(
            inputs = eligibleInputs().copy(
                isEnabledByServer = false,
                isAllowedByMerchant = false,
                isLinkDisplayed = false,
            ),
        )

        assertIneligible(result, IneligibleReason.DisabledByServer)
    }

    @Test
    fun `merchant opt out is disabled_by_merchant`() = runScenario {
        val result = evaluate(
            inputs = eligibleInputs().copy(isAllowedByMerchant = false, isLinkDisplayed = false),
        )

        assertIneligible(result, IneligibleReason.DisabledByMerchant)
    }

    @Test
    fun `hidden Link row is link_not_displayed`() = runScenario(account = null) {
        val result = evaluate(inputs = eligibleInputs().copy(isLinkDisplayed = false))

        assertIneligible(result, IneligibleReason.LinkNotDisplayed)
    }

    @Test
    fun `no Link account is not_returning_link_user`() = runScenario(account = null) {
        val result = evaluate()

        assertIneligible(result, IneligibleReason.NotReturningLinkUser)
    }

    @Test
    fun `verified account is already_verified`() = runScenario(account = TestFactory.LINK_ACCOUNT) {
        val result = evaluate(inputs = eligibleInputs().copy(canUseNativeLink = false))

        assertIneligible(result, IneligibleReason.AlreadyVerified)
    }

    @Test
    fun `device without native Link is native_link_unavailable`() = runScenario(
        account = needsVerificationAccount(webviewRequired = true),
    ) {
        val result = evaluate(inputs = eligibleInputs().copy(canUseNativeLink = false))

        assertIneligible(result, IneligibleReason.NativeLinkUnavailable)
    }

    @Test
    fun `account that requires the web view is web_view_required`() = runScenario(
        account = needsVerificationAccount(webviewRequired = true),
    ) {
        val result = evaluate()

        assertIneligible(result, IneligibleReason.WebViewRequired)
    }

    @Test
    fun `account with a code already sent is verification_already_started`() = runScenario(
        account = needsVerificationAccount(
            sessions = listOf(VerificationSession(VerificationSession.SessionType.Sms, STARTED)),
        ),
    ) {
        val result = evaluate(inputs = eligibleInputs().copy(hasSavedPaymentMethods = true))

        assertIneligible(result, IneligibleReason.VerificationAlreadyStarted)
    }

    @Test
    fun `customer with saved payment methods is has_saved_payment_methods`() = runScenario {
        registry.recordDismissal(TestFactory.EMAIL)

        val result = evaluate(inputs = eligibleInputs().copy(hasSavedPaymentMethods = true))

        assertIneligible(result, IneligibleReason.HasSavedPaymentMethods)
    }

    @Test
    fun `email that dismissed the card is dismissed_this_session`() = runScenario {
        registry.recordDismissal(TestFactory.EMAIL.uppercase())
        registry.claim(OTHER_OWNER)

        val result = evaluate()

        assertIneligible(result, IneligibleReason.DismissedThisSession)
    }

    @Test
    fun `card held by another element is shown_elsewhere`() = runScenario {
        registry.claim(OTHER_OWNER)
        registry.recordAutoSend(TestFactory.EMAIL)

        val result = evaluate()

        assertIneligible(result, IneligibleReason.ShownElsewhere)
    }

    @Test
    fun `card held by the same element is eligible`() = runScenario {
        registry.claim(OWNER)

        val result = evaluate()

        assertThat(result).isInstanceOf(Result.Eligible::class.java)
        prepareCalls.awaitItem()
    }

    @Test
    fun `code sent automatically to the email recently is code_recently_sent`() = runScenario {
        registry.recordAutoSend(TestFactory.EMAIL)

        val result = evaluate()

        assertIneligible(result, IneligibleReason.CodeRecentlySent)
    }

    @Test
    fun `account whose first step is a phone match is unsupported_verification`() = runScenario(
        account = needsVerificationAccount(factors = listOf(EMAIL_FACTOR)),
    ) {
        val result = evaluate()

        assertIneligible(result, IneligibleReason.UnsupportedVerification)
        prepareCalls.awaitItem()
    }

    @Test
    fun `account without a usable factor is unsupported_verification`() = runScenario(
        account = needsVerificationAccount(factors = emptyList()),
    ) {
        val result = evaluate()

        assertIneligible(result, IneligibleReason.UnsupportedVerification)
        prepareCalls.awaitItem()
    }

    @Test
    fun `only returning customers who need to verify while the feature is on are reported`() {
        val unreported = IneligibleReason.entries.filterNot { it.isReported }

        assertThat(unreported).containsExactly(
            IneligibleReason.DisabledByServer,
            IneligibleReason.LinkNotDisplayed,
            IneligibleReason.NotReturningLinkUser,
            IneligibleReason.AlreadyVerified,
        )
    }

    @Test
    fun `reasons have the analytics values shared with iOS`() {
        assertThat(IneligibleReason.entries.map { it.analyticsValue }).containsExactly(
            "disabled_by_server",
            "disabled_by_merchant",
            "link_not_displayed",
            "not_returning_link_user",
            "already_verified",
            "native_link_unavailable",
            "web_view_required",
            "verification_already_started",
            "has_saved_payment_methods",
            "dismissed_this_session",
            "shown_elsewhere",
            "code_recently_sent",
            "unsupported_verification",
        ).inOrder()
    }

    private fun assertIneligible(result: Result, reason: IneligibleReason) {
        assertThat(result).isEqualTo(Result.Ineligible(reason))
    }

    private fun runScenario(
        account: LinkAccount? = needsVerificationAccount(),
        block: suspend Scenario.() -> Unit,
    ) = runTest {
        val accountManager = FakeLinkAccountManager()
        accountManager.linkAccountHolder.set(LinkAccountUpdate.Value(account))
        val prepareCalls = Turbine<Unit>()
        val registry = EmbeddedLinkInlineAuthRegistry(now = { testScheduler.currentTime })

        Scenario(
            account = account,
            registry = registry,
            prepareCalls = prepareCalls,
            prepareAuthFlow = {
                prepareCalls.add(Unit)
                LinkAuthFlow(
                    linkAccountManager = accountManager,
                    // Any logged event fails the test: preparing an attempt must not report anything.
                    linkEventsReporter = FakeLinkEventsReporter(),
                    capabilities = listOf(VerificationType.SMS, VerificationType.EMAIL),
                    consentGranted = { null },
                    scope = backgroundScope,
                    now = { testScheduler.currentTime },
                    onFinish = { error("A prepared attempt must not finish") },
                )
            },
        ).block()

        prepareCalls.ensureAllEventsConsumed()
        // Preparing an attempt must not send a code.
        accountManager.ensureVerificationEventsConsumed()
    }

    private class Scenario(
        val account: LinkAccount?,
        val registry: EmbeddedLinkInlineAuthRegistry,
        val prepareCalls: Turbine<Unit>,
        private val prepareAuthFlow: () -> LinkAuthFlow,
    ) {
        fun eligibleInputs() = Inputs(
            isEnabledByServer = true,
            isAllowedByMerchant = true,
            isLinkDisplayed = true,
            linkAccount = account,
            canUseNativeLink = true,
            hasSavedPaymentMethods = false,
        )

        fun evaluate(inputs: Inputs = eligibleInputs()): Result {
            return EmbeddedLinkInlineVerificationEligibility.evaluate(
                inputs = inputs,
                registry = registry,
                owner = OWNER,
                prepareAuthFlow = prepareAuthFlow,
            )
        }
    }

    private companion object {
        val OWNER = Any()
        val OTHER_OWNER = Any()
        val STARTED = VerificationSession.SessionState.Started

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

        fun needsVerificationAccount(
            factors: List<VerificationFactor> = listOf(SMS_FACTOR, EMAIL_FACTOR),
            sessions: List<VerificationSession> = emptyList(),
            webviewRequired: Boolean = false,
        ) = LinkAccount(
            TestFactory.CONSUMER_SESSION.copy(
                verificationSessions = sessions,
                availableVerificationFactors = factors,
                currentAuthenticationLevel = AuthenticationLevel.NotAuthenticated,
                minimumAuthenticationLevel = AuthenticationLevel.OneFactorAuthentication,
                emailOtpRequiresAdditionalInfo = true,
                mobileFallbackWebviewParams = TestFactory.MOBILE_FALLBACK_WEBVIEW_PARAMS.takeIf { webviewRequired },
            )
        )
    }
}
