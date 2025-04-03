package com.stripe.android.common.analytics.experiment

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.stripe.android.core.utils.FeatureFlags
import com.stripe.android.model.ElementsSession
import com.stripe.android.model.PaymentIntentFixtures
import com.stripe.android.paymentsheet.analytics.FakeEventReporter
import com.stripe.android.testing.FakeLogger
import com.stripe.android.testing.FeatureFlagTestRule
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TestRule
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class LogLinkGlobalHoldbackExposureTest {

    private lateinit var eventReporter: FakeEventReporter
    private lateinit var logger: FakeLogger
    private lateinit var logLinkGlobalHoldbackExposure: LogLinkGlobalHoldbackExposure

    @get:Rule
    var rule: TestRule = InstantTaskExecutorRule()

    @get:Rule
    val linkGlobalHoldbackExposureEnabledRule = FeatureFlagTestRule(
        featureFlag = FeatureFlags.linkGlobalHoldbackExposureEnabled,
        isEnabled = true,
    )

    @Before
    fun setUp() {
        eventReporter = FakeEventReporter()
        logger = FakeLogger()
        logLinkGlobalHoldbackExposure = LogLinkGlobalHoldbackExposure(eventReporter, logger)
    }

    @Test
    fun `invoke should log exposure TREATMENT when feature flag is enabled and holdback is on`() = runTest {
        val elementsSession = createElementsSession(
            linkSettings = createLinkSettings(holdbackOn = true),
            experimentsData = ElementsSession.ExperimentsData(arbId = "test_arb_id")
        )

        logLinkGlobalHoldbackExposure(elementsSession)

        val exposureCall = eventReporter.experimentExposureCalls.awaitItem()

        assertTrue(exposureCall.experiment is LoggableExperiment.LinkGlobalHoldback)
        assertEquals(exposureCall.experiment.group, ExperimentGroup.TREATMENT)
    }

    @Test
    fun `invoke should log exposure CONTROL when feature flag is enabled and holdback is off`() = runTest {
        val elementsSession = createElementsSession(
            linkSettings = createLinkSettings(holdbackOn = false),
            experimentsData = ElementsSession.ExperimentsData(arbId = "test_arb_id")
        )

        logLinkGlobalHoldbackExposure(elementsSession)

        val exposureCall = eventReporter.experimentExposureCalls.awaitItem()

        assertTrue(exposureCall.experiment is LoggableExperiment.LinkGlobalHoldback)
        assertEquals(exposureCall.experiment.group, ExperimentGroup.CONTROL)
    }

    @Test
    fun `invoke should not log exposure when feature flag is disabled`() = runTest {
        linkGlobalHoldbackExposureEnabledRule.setEnabled(false)
        val elementsSession = createElementsSession(
            linkSettings = createLinkSettings(holdbackOn = false),
            experimentsData = ElementsSession.ExperimentsData(arbId = "test_arb_id")
        )

        logLinkGlobalHoldbackExposure(elementsSession)

        eventReporter.experimentExposureCalls.expectNoEvents()
    }

    @Test
    fun `invoke should log error when exception occurs`() {
        val elementsSession = createElementsSession(
            linkSettings = createLinkSettings(holdbackOn = false),
            experimentsData = null
        )

        logLinkGlobalHoldbackExposure(elementsSession)

        val loggedError = logger.errorLogs.first()
        assertEquals(
            "Failed to log Global holdback exposure",
            loggedError.first
        )
    }

    private fun createElementsSession(
        experimentsData: ElementsSession.ExperimentsData? = null,
        linkSettings: ElementsSession.LinkSettings? = null,
    ): ElementsSession {
        return ElementsSession(
            stripeIntent = PaymentIntentFixtures.PI_REQUIRES_PAYMENT_METHOD,
            cardBrandChoice = null,
            merchantCountry = null,
            isGooglePayEnabled = false,
            customer = null,
            linkSettings = linkSettings,
            customPaymentMethods = emptyList(),
            externalPaymentMethodData = null,
            paymentMethodSpecs = null,
            elementsSessionId = "session_1234",
            flags = emptyMap(),
            experimentsData = experimentsData
        )
    }

    private fun createLinkSettings(holdbackOn: Boolean) = ElementsSession.LinkSettings(
        linkGlobalHoldbackOn = holdbackOn,
        linkFundingSources = emptyList(),
        linkPassthroughModeEnabled = false,
        linkFlags = emptyMap(),
        linkMode = null,
        linkConsumerIncentive = null,
        disableLinkSignup = true,
        suppress2faModal = true,
        useAttestationEndpoints = holdbackOn
    )
}
