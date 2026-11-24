package com.stripe.android.paymentelement.embedded.linkinline

import android.app.Application
import androidx.lifecycle.SavedStateHandle
import androidx.test.core.app.ApplicationProvider
import app.cash.turbine.Turbine
import com.google.common.truth.Truth.assertThat
import com.stripe.android.common.analytics.experiment.LoggableExperiment
import com.stripe.android.link.FakeIntegrityRequestManager
import com.stripe.android.link.LinkAccountUpdate
import com.stripe.android.link.TestFactory
import com.stripe.android.link.account.FakeLinkAccountManager
import com.stripe.android.link.analytics.FakeLinkEventsReporter
import com.stripe.android.link.analytics.LinkAuthSurface
import com.stripe.android.link.gate.FakeLinkGate
import com.stripe.android.link.model.LinkAccount
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadata
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadataFactory
import com.stripe.android.model.ConsumerSession.AuthenticationLevel
import com.stripe.android.model.ConsumerSession.VerificationFactor
import com.stripe.android.model.ConsumerSession.VerificationFactor.FactorType
import com.stripe.android.model.ConsumerSession.VerificationSession
import com.stripe.android.model.ElementsSession
import com.stripe.android.model.ElementsSession.ExperimentAssignment
import com.stripe.android.model.LinkBrand
import com.stripe.android.paymentelement.EmbeddedPaymentElement
import com.stripe.android.paymentelement.confirmation.ConfirmationHandler
import com.stripe.android.paymentelement.confirmation.FakeConfirmationHandler
import com.stripe.android.paymentelement.confirmation.FakeConfirmationOption
import com.stripe.android.paymentelement.embedded.content.SheetStateHolder
import com.stripe.android.paymentelement.embedded.linkinline.EmbeddedLinkInlineVerificationEligibility.IneligibleReason
import com.stripe.android.paymentsheet.analytics.FakeEventReporter
import com.stripe.android.paymentsheet.model.PaymentSelection
import com.stripe.android.paymentsheet.state.LinkState
import com.stripe.android.utils.FakeLinkComponent
import com.stripe.android.utils.FakeLinkConfigurationCoordinator
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
internal class DefaultEmbeddedLinkInlineAuthTest {

    @Test
    fun `eligible returning customer sees the card with Link first`() = runScenario {
        configure()

        assertThat(linkInlineAuth.card.value?.state?.email).isEqualTo(TestFactory.EMAIL)
        assertThat(linkInlineAuth.listOptions.value).isEqualTo(
            EmbeddedLinkInlineListOptions(placesLinkFirst = true, linkPickIsChangeable = true)
        )
        assertThat(linkInlineAuth.needsVisibility.value).isTrue()
    }

    @Test
    fun `card sends its code once the element is visible`() = runScenario {
        configure()

        linkInlineAuth.onElementVisible()

        assertThat(eventReporter.embeddedLinkInlineAuthEventCalls.awaitItem())
            .isEqualTo(EmbeddedLinkInlineAuthEvent.Shown)
        assertThat(accountManager.awaitStartVerificationCall().analyticsSurface)
            .isEqualTo(LinkAuthSurface.EmbeddedInline)
        assertThat(linkInlineAuth.needsVisibility.value).isFalse()
    }

    @Test
    fun `server flag off keeps the regular Link row without logging`() = runScenario {
        configure(paymentMethodMetadata = metadata(isEnabledByServer = false))

        assertThat(linkInlineAuth.card.value).isNull()
        assertThat(linkInlineAuth.listOptions.value.placesLinkFirst).isFalse()
        assertThat(linkInlineAuth.needsVisibility.value).isFalse()
    }

    @Test
    fun `merchant opt out is logged once across load and configure`() = runScenario {
        val configuration = EmbeddedPaymentElement.Configuration.Builder("Merchant")
            .allowsLinkInlineVerification(false)
            .build()

        configure(configuration = configuration)
        configure(configuration = configuration)

        assertThat(eventReporter.embeddedLinkInlineAuthEventCalls.awaitItem())
            .isEqualTo(EmbeddedLinkInlineAuthEvent.Ineligible(IneligibleReason.DisabledByMerchant))
        assertThat(linkInlineAuth.card.value).isNull()
    }

    @Test
    fun `customer without a Link account logs nothing`() = runScenario(account = null) {
        configure()

        assertThat(linkInlineAuth.card.value).isNull()
    }

    @Test
    fun `control group logs exposure but keeps Link in its usual place`() = runScenario {
        configure(paymentMethodMetadata = metadata(experimentGroup = "control"))

        assertThat(linkInlineAuth.card.value).isNull()
        assertThat(linkInlineAuth.listOptions.value.placesLinkFirst).isFalse()

        linkInlineAuth.onElementVisible()

        val exposure = eventReporter.experimentExposureCalls.awaitItem().experiment
        assertThat(exposure).isInstanceOf(LoggableExperiment.OcsMobileEmbeddedLinkInlineOtp::class.java)
        assertThat(exposure.group).isEqualTo("control")
        assertThat(exposure.dimensions).containsAtLeast(
            "link_verification_methods", "sms_email",
            "embedded_form_sheet_action", "continue",
            "embedded_row_selection_behavior", "default",
            "has_saved_payment_method", "false",
            "large_text_size", "false",
        )
    }

    @Test
    fun `configure keeps the attempt that sent the code`() = runScenario {
        configure()
        linkInlineAuth.onElementVisible()
        eventReporter.embeddedLinkInlineAuthEventCalls.awaitItem()
        accountManager.awaitStartVerificationCall()
        val card = linkInlineAuth.card.value

        configure()

        assertThat(linkInlineAuth.card.value?.otpElement).isSameInstanceAs(card?.otpElement)
        assertThat(linkInlineAuth.needsVisibility.value).isFalse()
    }

    @Test
    fun `configure that hides Link ends the attempt and keeps Link first`() = runScenario {
        configure()

        configure(paymentMethodMetadata = PaymentMethodMetadataFactory.create())

        assertThat(linkInlineAuth.card.value).isNull()
        assertThat(linkInlineAuth.listOptions.value.placesLinkFirst).isTrue()
    }

    @Test
    fun `configure with the server flag turned off ends the attempt`() = runScenario {
        configure()

        configure(paymentMethodMetadata = metadata(isEnabledByServer = false))

        assertThat(linkInlineAuth.card.value).isNull()
    }

    @Test
    fun `configure updates the server's dialog switch`() = runScenario {
        configure()
        linkInlineAuth.registerDialogHost(isReady = { true })
        linkInlineAuth.onElementVisible()
        eventReporter.embeddedLinkInlineAuthEventCalls.awaitItem()
        accountManager.awaitStartVerificationCall()

        configure(paymentMethodMetadata = metadata(forcesDialog = true))
        linkInlineAuth.onCodeTapped()

        assertThat(eventReporter.embeddedLinkInlineAuthEventCalls.awaitItem()).isEqualTo(
            EmbeddedLinkInlineAuthEvent.DialogShown(EmbeddedLinkInlineAuthEvent.DialogReason.ForcedByServer)
        )
        assertThat(linkInlineAuth.isPresentingDialog).isTrue()
        assertThat(linkInlineAuth.isUiPresentedOverElement()).isTrue()
    }

    @Test
    fun `code is sent on the element's thread`() = runScenario {
        configure()
        val elementThread = Thread.currentThread()
        val startThreads = Turbine<Thread>()
        accountManager.startVerificationResultProvider = {
            startThreads.add(Thread.currentThread())
            Result.success(needsVerificationAccount(sessions = listOf(STARTED_SMS_SESSION)))
        }

        linkInlineAuth.onElementVisible()

        eventReporter.embeddedLinkInlineAuthEventCalls.awaitItem()
        accountManager.awaitStartVerificationCall()
        assertThat(startThreads.awaitItem()).isSameInstanceAs(elementThread)
    }

    @Test
    fun `Link can't start out selected while the card shows`() = runScenario {
        configure()

        assertThat(linkInlineAuth.initialSelection(PaymentSelection.Link(brand = LinkBrand.Link))).isNull()
        assertThat(linkInlineAuth.initialSelection(PaymentSelection.GooglePay)).isEqualTo(PaymentSelection.GooglePay)
    }

    @Test
    fun `Link can be the initial selection once the card is closed`() = runScenario {
        configure()
        linkInlineAuth.onClose()
        eventReporter.embeddedLinkInlineAuthEventCalls.awaitItem()

        val link = PaymentSelection.Link(brand = LinkBrand.Link)
        assertThat(linkInlineAuth.initialSelection(link)).isEqualTo(link)
        assertThat(linkInlineAuth.listOptions.value.placesLinkFirst).isTrue()
    }

    @Test
    fun `confirmed intent ends the card for good`() = runScenario {
        configure()

        linkInlineAuth.onIntentConfirmed()
        configure()

        assertThat(linkInlineAuth.card.value).isNull()
    }

    @Test
    fun `logging out elsewhere collapses the card`() = runScenario {
        configure()

        linkAccountHolder().set(LinkAccountUpdate.Value(account = null))

        assertThat(linkInlineAuth.card.value).isNull()
    }

    @Test
    fun `switching accounts logs the Link account out`() = runScenario {
        configure()

        linkInlineAuth.onSwitchAccount()

        val update = linkAccountHolder().linkAccountInfo.value
        assertThat(update.account).isNull()
        assertThat(update.lastUpdateReason).isEqualTo(LinkAccountUpdate.Value.UpdateReason.LoggedOut)
        assertThat(linkInlineAuth.card.value).isNull()
    }

    @Test
    fun `session in the experiment stays in it`() = runScenario {
        assertThat(linkInlineAuth.isInExperiment(metadata(), null, CONFIGURATION)).isTrue()
        configure()
        linkInlineAuth.onClose()
        eventReporter.embeddedLinkInlineAuthEventCalls.awaitItem()

        assertThat(linkInlineAuth.isInExperiment(metadata(isEnabledByServer = false), null, CONFIGURATION)).isTrue()
    }

    @Test
    fun `ineligible session isn't in the experiment`() = runScenario {
        assertThat(linkInlineAuth.isInExperiment(metadata(isEnabledByServer = false), null, CONFIGURATION))
            .isFalse()
    }

    @Test
    fun `dialog needs composed UI to host it`() = runScenario(
        paymentMethodMetadata = metadata(forcesDialog = true),
    ) {
        configure()
        linkInlineAuth.onElementVisible()
        eventReporter.embeddedLinkInlineAuthEventCalls.awaitItem()
        accountManager.awaitStartVerificationCall()

        linkInlineAuth.onCodeTapped()

        assertThat(linkInlineAuth.dialog.value).isNull()
    }

    @Test
    fun `dialog closes when its host goes away`() = runScenario(
        paymentMethodMetadata = metadata(forcesDialog = true),
    ) {
        configure()
        val host = linkInlineAuth.registerDialogHost(isReady = { true })
        linkInlineAuth.onElementVisible()
        eventReporter.embeddedLinkInlineAuthEventCalls.awaitItem()
        accountManager.awaitStartVerificationCall()
        linkInlineAuth.onCodeTapped()
        eventReporter.embeddedLinkInlineAuthEventCalls.awaitItem()
        assertThat(linkInlineAuth.dialog.value).isNotNull()

        host.close()

        assertThat(linkInlineAuth.dialog.value).isNull()
        assertThat(linkInlineAuth.card.value).isNotNull()
    }

    @Test
    fun `dialog isn't shown while a sheet is open`() = runScenario(
        paymentMethodMetadata = metadata(forcesDialog = true),
    ) {
        configure()
        linkInlineAuth.registerDialogHost(isReady = { true })
        sheetStateHolder.sheetIsOpen = true

        linkInlineAuth.onCodeTapped()

        assertThat(linkInlineAuth.dialog.value).isNull()
        assertThat(linkInlineAuth.isUiPresentedOverElement()).isTrue()
    }

    @Test
    fun `confirmation in flight covers the element`() = runScenario {
        confirmationHandler.state.value = ConfirmationHandler.State.Confirming(
            option = FakeConfirmationOption(),
        )

        assertThat(linkInlineAuth.isUiPresentedOverElement()).isTrue()
    }

    private fun runScenario(
        account: LinkAccount? = needsVerificationAccount(),
        paymentMethodMetadata: PaymentMethodMetadata = metadata(),
        block: suspend Scenario.() -> Unit,
    ) = runTest(UnconfinedTestDispatcher()) {
        val savedStateHandle = SavedStateHandle()
        val accountManager = FakeLinkAccountManager()
        accountManager.setsAccountOnVerification = true
        account?.let { accountManager.linkAccountHolder.set(LinkAccountUpdate.Value(it)) }
        accountManager.startVerificationResultProvider = {
            Result.success(needsVerificationAccount(sessions = listOf(STARTED_SMS_SESSION)))
        }
        val linkGate = FakeLinkGate().apply { setUseAttestationEndpoints(false) }
        val linkConfigurationCoordinator = FakeLinkConfigurationCoordinator(
            linkGate = linkGate,
            component = FakeLinkComponent(
                linkAccountManager = accountManager,
                linkAccountEventsReporter = NoOpLinkEventsReporter(),
                linkGate = linkGate,
            ),
        )
        val eventReporter = FakeEventReporter()
        val sheetStateHolder = SheetStateHolder(savedStateHandle)
        val confirmationHandler = FakeConfirmationHandler()
        val integrityRequestManager = FakeIntegrityRequestManager()
        val scope = CoroutineScope(
            backgroundScope.coroutineContext + SupervisorJob(backgroundScope.coroutineContext[Job])
        )

        val linkInlineAuth = DefaultEmbeddedLinkInlineAuth(
            viewModelScope = scope,
            savedStateHandle = savedStateHandle,
            linkAccountHolder = accountManager.linkAccountHolder,
            linkConfigurationCoordinator = linkConfigurationCoordinator,
            eventReporter = eventReporter,
            sheetStateHolder = sheetStateHolder,
            confirmationHandler = confirmationHandler,
            integrityRequestManager = integrityRequestManager,
            internalRowSelectionCallback = { null },
            context = ApplicationProvider.getApplicationContext<Application>(),
            mobileSessionId = "mobile_session_123",
            registry = EmbeddedLinkInlineAuthRegistry(now = { testScheduler.currentTime }),
        )

        Scenario(
            linkInlineAuth = linkInlineAuth,
            accountManager = accountManager,
            eventReporter = eventReporter,
            sheetStateHolder = sheetStateHolder,
            confirmationHandler = confirmationHandler,
            paymentMethodMetadata = paymentMethodMetadata,
        ).block()

        scope.cancel()
        accountManager.ensureVerificationEventsConsumed()
        eventReporter.validate()
        integrityRequestManager.ensureAllEventsConsumed()
    }

    private class Scenario(
        val linkInlineAuth: DefaultEmbeddedLinkInlineAuth,
        val accountManager: FakeLinkAccountManager,
        val eventReporter: FakeEventReporter,
        val sheetStateHolder: SheetStateHolder,
        val confirmationHandler: FakeConfirmationHandler,
        private val paymentMethodMetadata: PaymentMethodMetadata,
    ) {
        fun configure(
            paymentMethodMetadata: PaymentMethodMetadata = this.paymentMethodMetadata,
            configuration: EmbeddedPaymentElement.Configuration = CONFIGURATION,
        ) {
            linkInlineAuth.onConfigured(
                paymentMethodMetadata = paymentMethodMetadata,
                customer = null,
                configuration = configuration,
            )
        }

        fun linkAccountHolder() = accountManager.linkAccountHolder
    }

    private class NoOpLinkEventsReporter : FakeLinkEventsReporter() {
        override fun on2FACancel(surface: LinkAuthSurface?) = Unit
    }

    private companion object {
        val CONFIGURATION = EmbeddedPaymentElement.Configuration.Builder("Merchant").build()

        fun metadata(
            isEnabledByServer: Boolean = true,
            forcesDialog: Boolean = false,
            experimentGroup: String? = null,
        ) = PaymentMethodMetadataFactory.create(
            linkState = LinkState(
                configuration = TestFactory.LINK_CONFIGURATION.copy(
                    flags = mapOf(
                        "link_mobile_enable_embedded_inline_otp" to isEnabledByServer,
                        "link_mobile_embedded_inline_otp_force_dialog" to forcesDialog,
                        "link_mobile_enable_mfa_auth_flow" to true,
                    ),
                ),
                loginState = LinkState.LoginState.NeedsVerification,
                signupMode = null,
            ),
            experimentsData = experimentGroup?.let { group ->
                ElementsSession.ExperimentsData(
                    arbId = "arb_123",
                    experimentAssignments = mapOf(ExperimentAssignment.OCS_MOBILE_EMBEDDED_LINK_INLINE_OTP to group),
                )
            },
        )

        val STARTED_SMS_SESSION = VerificationSession(
            type = VerificationSession.SessionType.Sms,
            state = VerificationSession.SessionState.Started,
        )

        fun needsVerificationAccount(
            sessions: List<VerificationSession> = emptyList(),
        ) = LinkAccount(
            TestFactory.CONSUMER_SESSION.copy(
                verificationSessions = sessions,
                availableVerificationFactors = listOf(
                    VerificationFactor(
                        type = FactorType.Sms,
                        id = "sms_1",
                        providesFurtherVerification = true,
                        temporarilyDisabled = false,
                    ),
                    VerificationFactor(
                        type = FactorType.Email,
                        id = "email_1",
                        providesFurtherVerification = true,
                        temporarilyDisabled = false,
                    ),
                ),
                currentAuthenticationLevel = AuthenticationLevel.NotAuthenticated,
                minimumAuthenticationLevel = AuthenticationLevel.OneFactorAuthentication,
            )
        )
    }
}
