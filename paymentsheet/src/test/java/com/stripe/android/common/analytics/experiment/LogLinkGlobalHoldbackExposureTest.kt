package com.stripe.android.common.analytics.experiment

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.stripe.android.common.model.CommonConfigurationFactory
import com.stripe.android.core.utils.FeatureFlags
import com.stripe.android.link.repositories.FakeLinkRepository
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadataFactory
import com.stripe.android.model.ElementsSession
import com.stripe.android.model.ElementsSession.ExperimentAssignment.LINK_GLOBAL_HOLD_BACK
import com.stripe.android.model.PaymentIntentFixtures
import com.stripe.android.paymentsheet.analytics.FakeEventReporter
import com.stripe.android.paymentsheet.state.PaymentElementLoader
import com.stripe.android.testing.FakeLogger
import com.stripe.android.ui.core.cbc.CardBrandChoiceEligibility
import com.stripe.android.utils.FakeCustomerRepository
import kotlinx.coroutines.test.UnconfinedTestDispatcher
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
    private lateinit var linkRepsository: FakeLinkRepository
    private lateinit var logLinkGlobalHoldbackExposure: LogLinkGlobalHoldbackExposure
    private lateinit var customerRepository: FakeCustomerRepository

    @get:Rule
    var rule: TestRule = InstantTaskExecutorRule()

    private val testDispatcher = UnconfinedTestDispatcher()

    @get:Rule
    val linkGlobalHoldbackExposureEnabledRule = FeatureFlagTestRule(
        featureFlag = FeatureFlags.linkGlobalHoldbackExposureEnabled,
        isEnabled = true,
    )

    @Before
    fun setUp() {
        eventReporter = FakeEventReporter()
        logger = FakeLogger()
        linkRepsository = FakeLinkRepository()
        customerRepository = FakeCustomerRepository()

        logLinkGlobalHoldbackExposure = DefaultLogLinkGlobalHoldbackExposure(
            linkDisabledApiRepository = linkRepsository,
            eventReporter = eventReporter,
            logger = logger,
            customerRepository = customerRepository,
            workContext = testDispatcher
        )
    }

    @Test
    fun `invoke should log exposure TREATMENT when feature flag is enabled and holdback is on`() = runTest {
        FeatureFlags.linkGlobalHoldbackExposureEnabled.setEnabled(true)
        val elementsSession = createElementsSession(
            linkSettings = createLinkSettings(holdbackOn = true),
            experimentsData = ElementsSession.ExperimentsData(
                arbId = "test_arb_id",
                experimentAssignments = mapOf(
                    LINK_GLOBAL_HOLD_BACK to "holdback"
                )
            )
        )
        val state = createElementsState()

        logLinkGlobalHoldbackExposure(elementsSession, state)

        val exposureCall = eventReporter.experimentExposureCalls.awaitItem()

        assertTrue(exposureCall.experiment is LoggableExperiment.LinkGlobalHoldback)
        assertEquals(exposureCall.experiment.group, "holdback")
    }

    @Test
    fun `invoke should log exposure CONTROL when feature flag is enabled and holdback is off`() = runTest {
        FeatureFlags.linkGlobalHoldbackExposureEnabled.setEnabled(true)
        val elementsSession = createElementsSession(
            linkSettings = createLinkSettings(holdbackOn = false),
            experimentsData = ElementsSession.ExperimentsData(
                arbId = "test_arb_id",
                experimentAssignments = mapOf(
                    LINK_GLOBAL_HOLD_BACK to "control"
                )
            )
        )
        val state = createElementsState()

        logLinkGlobalHoldbackExposure(elementsSession, state)

        val exposureCall = eventReporter.experimentExposureCalls.awaitItem()

        assertTrue(exposureCall.experiment is LoggableExperiment.LinkGlobalHoldback)
        assertEquals(exposureCall.experiment.group, "control")
    }

    @Test
    fun `invoke should not log exposure when feature flag is disabled`() = runTest {
        linkGlobalHoldbackExposureEnabledRule.setEnabled(false)
        val elementsSession = createElementsSession(
            linkSettings = createLinkSettings(holdbackOn = false),
            experimentsData = ElementsSession.ExperimentsData(
                arbId = "test_arb_id",
                experimentAssignments = mapOf(
                    LINK_GLOBAL_HOLD_BACK to "holdback"
                )
            )
        )
        val state = createElementsState()

        logLinkGlobalHoldbackExposure(elementsSession, state)

        eventReporter.experimentExposureCalls.expectNoEvents()
    }

    @Test
    fun `invoke should log error when exception occurs`() {
        FeatureFlags.linkGlobalHoldbackExposureEnabled.setEnabled(true)
        val elementsSession = createElementsSession(
            linkSettings = createLinkSettings(holdbackOn = false),
            experimentsData = null
        )
        val state = createElementsState()

        logLinkGlobalHoldbackExposure(elementsSession, state)

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
        linkFundingSources = emptyList(),
        linkPassthroughModeEnabled = false,
        linkFlags = emptyMap(),
        linkMode = null,
        linkConsumerIncentive = null,
        disableLinkSignup = true,
        suppress2faModal = true,
        useAttestationEndpoints = holdbackOn
    )

    private fun createElementsState(): PaymentElementLoader.State {
        val configuration = CommonConfigurationFactory.create()
        return PaymentElementLoader.State(
            config = configuration,
            customer = null,
            paymentSelection = null,
            validationError = null,
            paymentMethodMetadata = PaymentMethodMetadataFactory.create(
                stripeIntent = PaymentIntentFixtures.PI_REQUIRES_PAYMENT_METHOD,
                billingDetailsCollectionConfiguration = configuration
                    .billingDetailsCollectionConfiguration,
                allowsDelayedPaymentMethods = configuration.allowsDelayedPaymentMethods,
                allowsPaymentMethodsRequiringShippingAddress = configuration
                    .allowsPaymentMethodsRequiringShippingAddress,
                isGooglePayReady = true,
                cbcEligibility = CardBrandChoiceEligibility.Ineligible,
            ),
        )
    }
}
