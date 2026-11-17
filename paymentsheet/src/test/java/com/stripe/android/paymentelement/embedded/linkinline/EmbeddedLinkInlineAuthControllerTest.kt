package com.stripe.android.paymentelement.embedded.linkinline

import app.cash.turbine.Turbine
import com.google.common.truth.Truth.assertThat
import com.stripe.android.common.analytics.experiment.LoggableExperiment
import com.stripe.android.core.StripeError
import com.stripe.android.core.exception.APIException
import com.stripe.android.link.LinkAccountUpdate
import com.stripe.android.link.TestFactory
import com.stripe.android.link.account.FakeLinkAccountManager
import com.stripe.android.link.analytics.FakeLinkEventsReporter
import com.stripe.android.link.analytics.LinkAuthSurface
import com.stripe.android.link.model.LinkAccount
import com.stripe.android.link.ui.verification.LinkAuthFlow
import com.stripe.android.link.ui.verification.LinkAuthFlowState
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadataFactory
import com.stripe.android.model.ConsumerSession.AuthenticationLevel
import com.stripe.android.model.ConsumerSession.VerificationFactor
import com.stripe.android.model.ConsumerSession.VerificationFactor.FactorType
import com.stripe.android.model.ConsumerSession.VerificationSession
import com.stripe.android.model.ElementsSession
import com.stripe.android.model.ElementsSession.ExperimentAssignment
import com.stripe.android.model.LinkBrand
import com.stripe.android.model.VerificationType
import com.stripe.android.paymentelement.EmbeddedPaymentElement
import com.stripe.android.paymentelement.embedded.linkinline.EmbeddedLinkInlineAuthController.CodeEntry
import com.stripe.android.paymentelement.embedded.linkinline.EmbeddedLinkInlineAuthController.ConfirmVerificationResult
import com.stripe.android.paymentelement.embedded.linkinline.EmbeddedLinkInlineAuthController.Phase
import com.stripe.android.paymentelement.embedded.linkinline.EmbeddedLinkInlineAuthEvent.DialogReason
import com.stripe.android.paymentelement.embedded.linkinline.EmbeddedLinkInlineAuthEvent.FallbackReason
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.analytics.FakeEventReporter
import com.stripe.android.uicore.elements.OTPElement
import com.stripe.android.uicore.elements.OTPElementFactory
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.cancel
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
internal class EmbeddedLinkInlineAuthControllerTest {

    @Before
    fun resetRegistry() {
        EmbeddedLinkInlineAuthRegistry.shared.reset()
    }

    @Test
    fun `treatment sends one code the first time the element is visible`() = runScenario(
        exposure = EXPOSURE,
    ) {
        controller.elementBecameVisible()
        controller.elementBecameVisible()

        assertThat(eventReporter.experimentExposureCalls.awaitItem().experiment).isEqualTo(EXPOSURE)
        assertThat(eventReporter.embeddedLinkInlineAuthEventCalls.awaitItem())
            .isEqualTo(EmbeddedLinkInlineAuthEvent.Shown)
        val start = accountManager.awaitStartVerificationCall()
        assertThat(start.type).isEqualTo(VerificationType.SMS)
        assertThat(start.analyticsSurface).isEqualTo(LinkAuthSurface.EmbeddedInline)
        assertThat(controller.card.value?.state?.isCodeEntryEnabled).isTrue()
        assertThat(registry.ineligibleReason(TestFactory.EMAIL, OTHER_ELEMENT))
            .isEqualTo(EmbeddedLinkInlineVerificationEligibility.IneligibleReason.ShownElsewhere)
    }

    @Test
    fun `nothing is sent before the element is visible`() = runScenario {
        assertThat(controller.card.value).isNotNull()
        assertThat(controller.card.value?.state?.isCodeEntryEnabled).isFalse()
        assertThat(controller.needsVisibility.value).isTrue()
    }

    @Test
    fun `control logs exposure without a card or a code`() = runScenario(
        showsCard = false,
        exposure = EXPOSURE,
    ) {
        assertThat(controller.card.value).isNull()

        controller.elementBecameVisible()

        assertThat(eventReporter.experimentExposureCalls.awaitItem().experiment).isEqualTo(EXPOSURE)
        assertThat(controller.needsVisibility.value).isFalse()
    }

    @Test
    fun `no assignment shows the card without exposure`() = runScenario(exposure = null) {
        controller.elementBecameVisible()

        assertThat(eventReporter.embeddedLinkInlineAuthEventCalls.awaitItem())
            .isEqualTo(EmbeddedLinkInlineAuthEvent.Shown)
        accountManager.awaitStartVerificationCall()
        assertThat(controller.card.value).isNotNull()
    }

    @Test
    fun `failed attestation collapses the card without sending a code`() = runScenario(
        requiresAttestation = true,
        attestationSucceeds = false,
    ) {
        controller.elementBecameVisible()

        assertThat(eventReporter.embeddedLinkInlineAuthEventCalls.awaitItem())
            .isEqualTo(EmbeddedLinkInlineAuthEvent.Shown)
        assertThat(eventReporter.embeddedLinkInlineAuthEventCalls.awaitItem())
            .isEqualTo(EmbeddedLinkInlineAuthEvent.FellBack(FallbackReason.AttestationFailed))
        assertThat(controller.phase.value).isEqualTo(Phase.Finished)
        assertThat(controller.card.value).isNull()
    }

    @Test
    fun `failed first send collapses the card`() = runScenario {
        accountManager.startVerificationResultProvider = {
            Result.failure(APIException(stripeError = StripeError(code = "rate_limit", message = "Slow down")))
        }

        controller.elementBecameVisible()

        eventReporter.embeddedLinkInlineAuthEventCalls.awaitItem()
        accountManager.awaitStartVerificationCall()
        assertThat(eventReporter.embeddedLinkInlineAuthEventCalls.awaitItem())
            .isEqualTo(EmbeddedLinkInlineAuthEvent.FellBack(FallbackReason.SendFailed))
        assertThat(controller.phase.value).isEqualTo(Phase.Finished)
        // The card is released, but the email still counts as having been sent a code.
        assertThat(registry.ineligibleReason(TestFactory.EMAIL, OTHER_ELEMENT))
            .isEqualTo(EmbeddedLinkInlineVerificationEligibility.IneligibleReason.CodeRecentlySent)
    }

    @Test
    fun `closing the card keeps the attempt and its code`() = runScenario {
        sendFirstCode()

        controller.onClose()

        assertThat(eventReporter.embeddedLinkInlineAuthEventCalls.awaitItem())
            .isEqualTo(EmbeddedLinkInlineAuthEvent.Dismissed)
        assertThat(controller.phase.value).isEqualTo(Phase.Collapsed)
        assertThat(controller.card.value).isNull()
        assertThat(controller.verifiesBeforeConfirming).isTrue()
        assertThat(registry.ineligibleReason(TestFactory.EMAIL.uppercase(), OTHER_ELEMENT))
            .isEqualTo(EmbeddedLinkInlineVerificationEligibility.IneligibleReason.DismissedThisSession)
        linkEventsReporter.cancelCalls.expectNoEvents()
    }

    @Test
    fun `switching accounts logs out and ends the attempt`() = runScenario {
        sendFirstCode()

        controller.onSwitchAccount()

        assertThat(linkEventsReporter.cancelCalls.awaitItem()).isEqualTo(LinkAuthSurface.EmbeddedInline)
        assertThat(host.logOutCalls.awaitItem()).isEqualTo(account)
        assertThat(controller.phase.value).isEqualTo(Phase.Finished)
        assertThat(controller.card.value).isNull()
    }

    @Test
    fun `right code from the card verifies from the card`() = runScenario {
        sendFirstCode()

        enterCode(controller.card.value?.otpElement)

        assertThat(accountManager.awaitConfirmVerificationCall().analyticsSurface)
            .isEqualTo(LinkAuthSurface.EmbeddedInline)
        assertThat(host.verifiedCalls.awaitItem()).isTrue()
        assertThat(controller.phase.value).isEqualTo(Phase.Finished)
        assertThat(registry.ineligibleReason(TestFactory.EMAIL, OTHER_ELEMENT))
            .isEqualTo(EmbeddedLinkInlineVerificationEligibility.IneligibleReason.CodeRecentlySent)
    }

    @Test
    fun `wrong code clears the card's code and keeps the card`() = runScenario {
        sendFirstCode()
        accountManager.confirmVerificationResultProvider = {
            Result.failure(APIException(stripeError = StripeError(code = "invalid", message = "Wrong code")))
        }
        val otpElement = requireNotNull(controller.card.value?.otpElement)

        enterCode(otpElement)

        accountManager.awaitConfirmVerificationCall()
        assertThat(otpElement.controller.fieldValue.value).isEmpty()
        assertThat(controller.card.value?.state?.errorMessage).isNotNull()
        assertThat(controller.phase.value).isEqualTo(Phase.Expanded)
    }

    @Test
    fun `server switch sends code entry to the dialog`() = runScenario(forcesDialog = true) {
        sendFirstCode()

        controller.onCodeTapped()

        assertDialogShown(DialogReason.ForcedByServer)
    }

    @Test
    fun `screen reader sends code entry to the dialog`() = runScenario(isScreenReaderOn = true) {
        sendFirstCode()

        controller.onCodeTapped()

        assertDialogShown(DialogReason.ScreenReader)
    }

    @Test
    fun `layout decides where the code is entered otherwise`() = runScenario {
        sendFirstCode()

        controller.onCodeTapped()

        assertDialogShown(DialogReason.NotInWindow)
    }

    @Test
    fun `codes after the first SMS code are entered in the dialog`() = runScenario(
        emailOtpRequiresAdditionalInfo = false,
    ) {
        sendFirstCode()
        controller.onAction(EmbeddedLinkInlineCardState.Action.Email)
        assertThat(accountManager.awaitStartVerificationCall().type).isEqualTo(VerificationType.EMAIL)
        assertDialogShown(DialogReason.Email)
        controller.dialog.value?.onBack()

        controller.onCodeTapped()

        assertDialogShown(DialogReason.UnsupportedStep)
    }

    @Test
    fun `email code that needs a phone number match continues in the dialog`() = runScenario {
        sendFirstCode()

        controller.onAction(EmbeddedLinkInlineCardState.Action.Email)

        assertDialogShown(DialogReason.Email)
        assertThat(controller.dialog.value?.viewState?.value?.authFlow?.screen)
            .isEqualTo(LinkAuthFlowState.Screen.PhoneMatch)
        assertThat(controller.phase.value).isEqualTo(Phase.Expanded)
    }

    @Test
    fun `closing the dialog on a phone number match closes the card`() = runScenario {
        sendFirstCode()
        controller.onAction(EmbeddedLinkInlineCardState.Action.Email)
        assertDialogShown(DialogReason.Email)

        controller.dialog.value?.onBack()

        assertThat(eventReporter.embeddedLinkInlineAuthEventCalls.awaitItem())
            .isEqualTo(EmbeddedLinkInlineAuthEvent.Dismissed)
        assertThat(controller.phase.value).isEqualTo(Phase.Collapsed)
        assertThat(controller.verifiesBeforeConfirming).isTrue()
    }

    @Test
    fun `dialog isn't shown when the host can't present it`() = runScenario {
        sendFirstCode()
        host.canPresentDialog = false

        controller.onCodeTapped()

        assertThat(controller.dialog.value).isNull()
    }

    @Test
    fun `closing the dialog keeps the attempt without cancelling or resending`() = runScenario {
        sendFirstCode()
        controller.onCodeTapped()
        assertDialogShown(DialogReason.NotInWindow)

        controller.dialog.value?.onBack()

        assertThat(controller.dialog.value).isNull()
        assertThat(controller.phase.value).isEqualTo(Phase.Expanded)
        assertThat(controller.card.value?.state?.isCodeEntryEnabled).isTrue()
        linkEventsReporter.cancelCalls.expectNoEvents()
    }

    @Test
    fun `dialog verifies the same attempt with the code the card sent`() = runScenario {
        sendFirstCode()
        controller.onCodeTapped()
        assertDialogShown(DialogReason.NotInWindow)

        enterCode(controller.dialog.value?.otpElement)

        assertThat(accountManager.awaitConfirmVerificationCall().analyticsSurface)
            .isEqualTo(LinkAuthSurface.EmbeddedDialog)
        assertThat(host.verifiedCalls.awaitItem()).isTrue()
        assertThat(controller.dialog.value).isNull()
        assertThat(controller.phase.value).isEqualTo(Phase.Finished)
    }

    @Test
    fun `dialog verifies on the element's thread`() = runScenario {
        sendFirstCode()
        controller.onCodeTapped()
        assertDialogShown(DialogReason.NotInWindow)
        val elementThread = Thread.currentThread()
        val confirmThreads = Turbine<Thread>()
        accountManager.confirmVerificationResultProvider = {
            confirmThreads.add(Thread.currentThread())
            Result.success(needsVerificationAccount(current = AuthenticationLevel.OneFactorAuthentication))
        }

        enterCode(controller.dialog.value?.otpElement)

        accountManager.awaitConfirmVerificationCall()
        assertThat(confirmThreads.awaitItem()).isSameInstanceAs(elementThread)
        host.verifiedCalls.awaitItem()
    }

    @Test
    fun `dialog shows the resend countdown of the card`() = runScenario {
        sendFirstCode()
        controller.onAction(EmbeddedLinkInlineCardState.Action.Resend)
        assertThat(linkEventsReporter.resendCalls.awaitItem()).isEqualTo(LinkAuthSurface.EmbeddedInline)
        assertThat(accountManager.awaitStartVerificationCall().isResend).isTrue()

        controller.onCodeTapped()
        assertDialogShown(DialogReason.NotInWindow)

        assertThat(controller.dialog.value?.viewState?.value?.authFlow?.resendSecondsRemaining).isGreaterThan(0)
    }

    @Test
    fun `verifying before paying reuses the code after the card was closed`() = runScenario {
        sendFirstCode()
        controller.onClose()
        eventReporter.embeddedLinkInlineAuthEventCalls.awaitItem()

        val result = testScope.async { controller.verifyBeforeConfirming() }
        assertDialogShown(DialogReason.Confirm)
        enterCode(controller.dialog.value?.otpElement)

        assertThat(result.await()).isEqualTo(ConfirmVerificationResult.Verified)
        accountManager.awaitConfirmVerificationCall()
        assertThat(host.verifiedCalls.awaitItem()).isFalse()
    }

    @Test
    fun `closing the dialog before paying cancels and keeps the attempt`() = runScenario {
        sendFirstCode()
        controller.onClose()
        eventReporter.embeddedLinkInlineAuthEventCalls.awaitItem()

        val result = testScope.async { controller.verifyBeforeConfirming() }
        assertDialogShown(DialogReason.Confirm)
        controller.dialog.value?.onBack()

        assertThat(result.await()).isEqualTo(ConfirmVerificationResult.Canceled)
        assertThat(controller.phase.value).isEqualTo(Phase.Collapsed)
        assertThat(controller.verifiesBeforeConfirming).isTrue()
    }

    @Test
    fun `verifying before paying is unavailable while the card shows`() = runScenario {
        sendFirstCode()

        assertThat(controller.verifiesBeforeConfirming).isFalse()
        assertThat(controller.verifyBeforeConfirming()).isEqualTo(ConfirmVerificationResult.Unavailable)
    }

    @Test
    fun `verifying before paying is unavailable when the dialog can't be shown`() = runScenario {
        sendFirstCode()
        controller.onClose()
        eventReporter.embeddedLinkInlineAuthEventCalls.awaitItem()
        host.canPresentDialog = false

        assertThat(controller.verifyBeforeConfirming()).isEqualTo(ConfirmVerificationResult.Unavailable)
    }

    @Test
    fun `another account ends the attempt silently`() = runScenario {
        sendFirstCode()

        controller.onAccountChanged(needsVerificationAccount(email = "other@example.com"))

        assertThat(controller.phase.value).isEqualTo(Phase.Finished)
        assertThat(controller.card.value).isNull()
        linkEventsReporter.cancelCalls.expectNoEvents()
    }

    @Test
    fun `the same account doesn't end the attempt`() = runScenario {
        sendFirstCode()

        controller.onAccountChanged(needsVerificationAccount(email = TestFactory.EMAIL.uppercase()))

        assertThat(controller.phase.value).isEqualTo(Phase.Expanded)
    }

    @Test
    fun `logging out ends the attempt`() = runScenario {
        sendFirstCode()

        controller.onAccountChanged(null)

        assertThat(controller.phase.value).isEqualTo(Phase.Finished)
    }

    @Test
    fun `account requiring the web collapses the card`() = runScenario {
        sendFirstCode()
        accountManager.confirmVerificationResultProvider = {
            Result.success(
                needsVerificationAccount(
                    sessions = listOf(startedSession(VerificationType.SMS)),
                    webviewRequired = true,
                )
            )
        }

        enterCode(controller.card.value?.otpElement)

        accountManager.awaitConfirmVerificationCall()
        assertThat(eventReporter.embeddedLinkInlineAuthEventCalls.awaitItem())
            .isEqualTo(EmbeddedLinkInlineAuthEvent.FellBack(FallbackReason.WebHandoff))
        assertThat(controller.phase.value).isEqualTo(Phase.Finished)
    }

    @Test
    fun `invalidating releases the card`() = runScenario {
        controller.invalidate()

        assertThat(controller.phase.value).isEqualTo(Phase.Finished)
        assertThat(registry.ineligibleReason(TestFactory.EMAIL, OTHER_ELEMENT)).isNull()
        assertThat(controller.needsVisibility.value).isFalse()
    }

    private suspend fun Scenario.sendFirstCode() {
        controller.elementBecameVisible()
        assertThat(eventReporter.embeddedLinkInlineAuthEventCalls.awaitItem())
            .isEqualTo(EmbeddedLinkInlineAuthEvent.Shown)
        accountManager.awaitStartVerificationCall()
    }

    private suspend fun Scenario.assertDialogShown(reason: DialogReason) {
        assertThat(eventReporter.embeddedLinkInlineAuthEventCalls.awaitItem())
            .isEqualTo(EmbeddedLinkInlineAuthEvent.DialogShown(reason))
        assertThat(controller.dialog.value).isNotNull()
    }

    private fun enterCode(otpElement: OTPElement?) {
        requireNotNull(otpElement).controller.onValueChanged(index = 0, text = CODE)
    }

    @Suppress("LongMethod")
    private fun runScenario(
        emailOtpRequiresAdditionalInfo: Boolean = true,
        showsCard: Boolean = true,
        exposure: LoggableExperiment? = null,
        forcesDialog: Boolean = false,
        requiresAttestation: Boolean = false,
        attestationSucceeds: Boolean = true,
        isScreenReaderOn: Boolean = false,
        block: suspend Scenario.() -> Unit,
    ) = runTest(UnconfinedTestDispatcher()) {
        val account = needsVerificationAccount(emailOtpRequiresAdditionalInfo = emailOtpRequiresAdditionalInfo)
        val accountManager = FakeLinkAccountManager()
        accountManager.setsAccountOnVerification = true
        accountManager.linkAccountHolder.set(LinkAccountUpdate.Value(account))
        accountManager.startVerificationResultProvider = { call ->
            Result.success(
                needsVerificationAccount(
                    emailOtpRequiresAdditionalInfo = emailOtpRequiresAdditionalInfo,
                    sessions = listOf(startedSession(call.type)),
                )
            )
        }
        accountManager.confirmVerificationResultProvider = {
            Result.success(needsVerificationAccount(current = AuthenticationLevel.OneFactorAuthentication))
        }
        val eventReporter = FakeEventReporter()
        val linkEventsReporter = RecordingLinkEventsReporter()
        val host = FakeHost()
        val registry = EmbeddedLinkInlineAuthRegistry.shared
        val scope = CoroutineScope(
            backgroundScope.coroutineContext + SupervisorJob(backgroundScope.coroutineContext[Job])
        )

        var controller: EmbeddedLinkInlineAuthController? = null
        val authFlow = LinkAuthFlow(
            linkAccountManager = accountManager,
            linkEventsReporter = linkEventsReporter,
            capabilities = listOf(VerificationType.SMS, VerificationType.EMAIL),
            consentGranted = { null },
            scope = scope,
            now = { testScheduler.currentTime },
            onFinish = { controller?.onAuthFlowFinished(it) },
        )
        controller = EmbeddedLinkInlineAuthController(
            linkAccount = account,
            authFlow = authFlow,
            showsCard = showsCard,
            exposure = exposure,
            paymentMethodMetadata = PaymentMethodMetadataFactory.create(),
            forcesDialog = forcesDialog,
            linkBrand = LinkBrand.Link,
            host = host,
            eventReporter = eventReporter,
            registry = registry,
            registryOwner = ELEMENT,
            requiresAttestation = requiresAttestation,
            prepareAttestation = { attestationSucceeds },
            isScreenReaderOn = { isScreenReaderOn },
            layoutCodeEntry = { CodeEntry.Dialog(DialogReason.NotInWindow) },
            cardOtpElement = OTPElementFactory.create(),
            scope = scope,
        )

        Scenario(
            controller = controller,
            accountManager = accountManager,
            eventReporter = eventReporter,
            linkEventsReporter = linkEventsReporter,
            host = host,
            registry = registry,
            account = account,
            testScope = this,
        ).block()

        scope.cancel()
        accountManager.ensureVerificationEventsConsumed()
        eventReporter.validate()
        linkEventsReporter.ensureAllEventsConsumed()
        host.ensureAllEventsConsumed()
    }

    private class Scenario(
        val controller: EmbeddedLinkInlineAuthController,
        val accountManager: FakeLinkAccountManager,
        val eventReporter: FakeEventReporter,
        val linkEventsReporter: RecordingLinkEventsReporter,
        val host: FakeHost,
        val registry: EmbeddedLinkInlineAuthRegistry,
        val account: LinkAccount,
        val testScope: TestScope,
    )

    private class FakeHost : EmbeddedLinkInlineAuthController.Host {
        var canPresentDialog = true
        val verifiedCalls = Turbine<Boolean>()
        val logOutCalls = Turbine<LinkAccount>()

        override fun canPresentDialog(): Boolean = canPresentDialog

        override fun onVerified(fromCard: Boolean) {
            verifiedCalls.add(fromCard)
        }

        override fun logOut(account: LinkAccount) {
            logOutCalls.add(account)
        }

        fun ensureAllEventsConsumed() {
            verifiedCalls.ensureAllEventsConsumed()
            logOutCalls.ensureAllEventsConsumed()
        }
    }

    private class RecordingLinkEventsReporter : FakeLinkEventsReporter() {
        val cancelCalls = Turbine<LinkAuthSurface?>()
        val resendCalls = Turbine<LinkAuthSurface?>()

        override fun on2FACancel(surface: LinkAuthSurface?) {
            cancelCalls.add(surface)
        }

        override fun on2FAResendCode(verificationType: String, surface: LinkAuthSurface?) {
            resendCalls.add(surface)
        }

        fun ensureAllEventsConsumed() {
            cancelCalls.ensureAllEventsConsumed()
            resendCalls.ensureAllEventsConsumed()
        }
    }

    private companion object {
        const val CODE = "123456"
        val ELEMENT = Any()
        val OTHER_ELEMENT = Any()

        val EXPOSURE = LoggableExperiment.OcsMobileEmbeddedLinkInlineOtp(
            experimentsData = ElementsSession.ExperimentsData(
                arbId = "arb_123",
                experimentAssignments = mapOf(ExperimentAssignment.OCS_MOBILE_EMBEDDED_LINK_INLINE_OTP to "treatment"),
            ),
            group = "treatment",
            metadata = PaymentMethodMetadataFactory.create(),
            mobileSessionId = "mobile_session",
            mobileSdkVersion = "1.0.0",
            hasSavedPaymentMethod = false,
            rowStyle = PaymentSheet.Appearance.Embedded.RowStyle.FlatWithRadio.default,
            formSheetAction = EmbeddedPaymentElement.FormSheetAction.Continue,
            isRowSelectionImmediateAction = false,
            canVerifyWithEmail = true,
            isLargeTextSize = false,
        )

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
            email: String = TestFactory.EMAIL,
            emailOtpRequiresAdditionalInfo: Boolean = true,
            sessions: List<VerificationSession> = emptyList(),
            current: AuthenticationLevel = AuthenticationLevel.NotAuthenticated,
            webviewRequired: Boolean = false,
        ) = LinkAccount(
            TestFactory.CONSUMER_SESSION.copy(
                emailAddress = email,
                verificationSessions = sessions,
                availableVerificationFactors = listOf(SMS_FACTOR, EMAIL_FACTOR),
                currentAuthenticationLevel = current,
                minimumAuthenticationLevel = AuthenticationLevel.OneFactorAuthentication,
                emailOtpRequiresAdditionalInfo = emailOtpRequiresAdditionalInfo,
                mobileFallbackWebviewParams = TestFactory.MOBILE_FALLBACK_WEBVIEW_PARAMS.takeIf { webviewRequired },
            )
        )

        fun startedSession(type: VerificationType) = VerificationSession(
            type = when (type) {
                VerificationType.SMS -> VerificationSession.SessionType.Sms
                VerificationType.EMAIL -> VerificationSession.SessionType.Email
            },
            state = VerificationSession.SessionState.Started,
        )
    }
}
