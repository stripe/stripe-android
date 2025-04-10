package com.stripe.android.common.analytics.experiment

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.google.common.truth.Truth.assertThat
import com.stripe.android.common.model.CommonConfigurationFactory
import com.stripe.android.core.utils.FeatureFlags
import com.stripe.android.link.TestFactory
import com.stripe.android.link.TestFactory.CONSUMER_SESSION
import com.stripe.android.link.TestFactory.PUBLISHABLE_KEY
import com.stripe.android.link.repositories.FakeLinkRepository
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadata
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadataFactory
import com.stripe.android.model.ConsumerSessionLookup
import com.stripe.android.model.ElementsSession
import com.stripe.android.model.ElementsSession.ExperimentAssignment.LINK_GLOBAL_HOLD_BACK
import com.stripe.android.model.PaymentIntentFixtures
import com.stripe.android.paymentsheet.analytics.FakeEventReporter
import com.stripe.android.paymentsheet.state.DefaultRetrieveCustomerEmail
import com.stripe.android.paymentsheet.state.LinkState
import com.stripe.android.paymentsheet.state.PaymentElementLoader
import com.stripe.android.paymentsheet.state.RetrieveCustomerEmail
import com.stripe.android.testing.FakeLogger
import com.stripe.android.testing.FeatureFlagTestRule
import com.stripe.android.utils.FakeCustomerRepository
import kotlinx.coroutines.test.UnconfinedTestDispatcher
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
    private lateinit var linkRepository: FakeLinkRepository
    private lateinit var logLinkGlobalHoldbackExposure: LogLinkGlobalHoldbackExposure
    private lateinit var customerRepository: FakeCustomerRepository
    private lateinit var retrieveCustomerEmail: RetrieveCustomerEmail

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
        linkRepository = FakeLinkRepository()
        customerRepository = FakeCustomerRepository()
        retrieveCustomerEmail = DefaultRetrieveCustomerEmail(customerRepository)

        logLinkGlobalHoldbackExposure = DefaultLogLinkGlobalHoldbackExposure(
            linkDisabledApiRepository = linkRepository,
            eventReporter = eventReporter,
            logger = logger,
            workContext = testDispatcher,
            retrieveCustomerEmail = retrieveCustomerEmail
        )
    }

    @Test
    fun `invoke should log exposure TREATMENT when feature flag is enabled and holdback is on`() = runTest {
        val elementsSession = createElementsSession(
            experimentsData = ElementsSession.ExperimentsData(
                arbId = "test_arb_id",
                experimentAssignments = mapOf(
                    LINK_GLOBAL_HOLD_BACK to "holdback"
                )
            )
        )
        val state = createElementsState(PaymentMethodMetadataFactory.create())

        logLinkGlobalHoldbackExposure(elementsSession, state)

        val exposureCall = eventReporter.experimentExposureCalls.awaitItem()

        assertTrue(exposureCall.experiment is LoggableExperiment.LinkGlobalHoldback)
        assertEquals(exposureCall.experiment.group, "holdback")
    }

    @Test
    fun `invoke should log exposure CONTROL when feature flag is enabled and holdback is off`() = runTest {
        val elementsSession = createElementsSession(
            experimentsData = ElementsSession.ExperimentsData(
                arbId = "test_arb_id",
                experimentAssignments = mapOf(
                    LINK_GLOBAL_HOLD_BACK to "control"
                )
            )
        )
        val state = createElementsState(PaymentMethodMetadataFactory.create())

        logLinkGlobalHoldbackExposure(elementsSession, state)

        val exposureCall = eventReporter.experimentExposureCalls.awaitItem()

        assertTrue(exposureCall.experiment is LoggableExperiment.LinkGlobalHoldback)
        assertEquals(exposureCall.experiment.group, "control")
    }

    @Test
    fun `invoke should not log exposure when feature flag is disabled`() = runTest {
        linkGlobalHoldbackExposureEnabledRule.setEnabled(false)
        val elementsSession = createElementsSession(
            experimentsData = ElementsSession.ExperimentsData(
                arbId = "test_arb_id",
                experimentAssignments = mapOf(
                    LINK_GLOBAL_HOLD_BACK to "holdback"
                )
            )
        )
        val state = createElementsState(PaymentMethodMetadataFactory.create())

        logLinkGlobalHoldbackExposure(elementsSession, state)

        eventReporter.experimentExposureCalls.expectNoEvents()
    }

    @Test
    fun `invoke should log error when exception occurs`() {
        val elementsSession = createElementsSession(
            experimentsData = null
        )
        val state = createElementsState(PaymentMethodMetadataFactory.create())

        logLinkGlobalHoldbackExposure(elementsSession, state)

        val loggedError = logger.errorLogs.first()
        assertEquals(
            "Failed to log Global holdback exposure",
            loggedError.first
        )
    }

    @Test
    fun `invoke should log exposure with returning user when user is returning`() = runTest {
        val elementsSession = createElementsSession(
            experimentsData = ElementsSession.ExperimentsData(
                arbId = "test_arb_id",
                experimentAssignments = mapOf(
                    LINK_GLOBAL_HOLD_BACK to "holdback"
                )
            )
        )
        val state = createElementsState(
            paymentMethodMetadata = PaymentMethodMetadataFactory.create(
                linkState = LinkState(
                    // preset link configuration with existing email.
                    configuration = TestFactory.LINK_CONFIGURATION,
                    loginState = LinkState.LoginState.NeedsVerification,
                    signupMode = null
                )
            )
        )

        linkRepository.lookupConsumerWithoutBackendLoggingResult = Result.success(
            ConsumerSessionLookup(
                exists = true,
                consumerSession = CONSUMER_SESSION,
                errorMessage = null,
                publishableKey = PUBLISHABLE_KEY
            )
        )

        logLinkGlobalHoldbackExposure(elementsSession, state)

        val lookupCall = linkRepository.awaitLookupWithoutBackendLogging()
        assertEquals(lookupCall.email, TestFactory.LINK_CONFIGURATION.customerInfo.email!!)

        val exposureCall = eventReporter.experimentExposureCalls.awaitItem()
        val experiment = exposureCall.experiment
        assertTrue(experiment is LoggableExperiment.LinkGlobalHoldback)
        assertThat(experiment.group).isEqualTo("holdback")
        assertThat(experiment.isReturningLinkConsumer).isTrue()
    }

    @Test
    fun `invoke should not log exposure and log local error if lookup fails`() = runTest {
        val elementsSession = createElementsSession(
            experimentsData = ElementsSession.ExperimentsData(
                arbId = "test_arb_id",
                experimentAssignments = mapOf(
                    LINK_GLOBAL_HOLD_BACK to "holdback"
                )
            )
        )
        val state = createElementsState(
            paymentMethodMetadata = PaymentMethodMetadataFactory.create(
                linkState = LinkState(
                    // preset link configuration with existing email.
                    configuration = TestFactory.LINK_CONFIGURATION,
                    loginState = LinkState.LoginState.NeedsVerification,
                    signupMode = null
                )
            )
        )

        linkRepository.lookupConsumerWithoutBackendLoggingResult = Result.failure<ConsumerSessionLookup>(
            IllegalArgumentException("Test exception")
        )

        logLinkGlobalHoldbackExposure(elementsSession, state)

        eventReporter.experimentExposureCalls.expectNoEvents()
        assertThat(logger.errorLogs.last().first).isEqualTo("Failed to log Global holdback exposure")
    }

    @Test
    fun `invoke should log exposure with non-returning user when user is not returning`() = runTest {
        val elementsSession = createElementsSession(
            experimentsData = ElementsSession.ExperimentsData(
                arbId = "test_arb_id",
                experimentAssignments = mapOf(
                    LINK_GLOBAL_HOLD_BACK to "holdback"
                )
            )
        )
        val state = createElementsState(
            paymentMethodMetadata = PaymentMethodMetadataFactory.create(
                linkState = LinkState(
                    // preset link configuration with existing email.
                    configuration = TestFactory.LINK_CONFIGURATION,
                    loginState = LinkState.LoginState.NeedsVerification,
                    signupMode = null
                )
            )
        )

        linkRepository.lookupConsumerWithoutBackendLoggingResult = Result.success(
            ConsumerSessionLookup(
                // simulate a non-returning user
                exists = false,
                consumerSession = CONSUMER_SESSION,
                errorMessage = null,
                publishableKey = PUBLISHABLE_KEY
            )
        )

        logLinkGlobalHoldbackExposure(elementsSession, state)

        val lookupCall = linkRepository.awaitLookupWithoutBackendLogging()
        assertEquals(lookupCall.email, TestFactory.LINK_CONFIGURATION.customerInfo.email!!)

        val exposureCall = eventReporter.experimentExposureCalls.awaitItem()
        assertTrue(exposureCall.experiment is LoggableExperiment.LinkGlobalHoldback)
        assertThat(exposureCall.experiment.group).isEqualTo("holdback")
        assertThat(exposureCall.experiment.isReturningLinkConsumer).isFalse()
    }

    private fun createElementsSession(
        experimentsData: ElementsSession.ExperimentsData? = null,
    ): ElementsSession {
        return ElementsSession(
            stripeIntent = PaymentIntentFixtures.PI_REQUIRES_PAYMENT_METHOD,
            cardBrandChoice = null,
            merchantCountry = null,
            isGooglePayEnabled = false,
            customer = null,
            linkSettings = null,
            customPaymentMethods = emptyList(),
            externalPaymentMethodData = null,
            paymentMethodSpecs = null,
            elementsSessionId = "session_1234",
            flags = emptyMap(),
            experimentsData = experimentsData
        )
    }

    private fun createElementsState(
        paymentMethodMetadata: PaymentMethodMetadata = PaymentMethodMetadataFactory.create()
    ): PaymentElementLoader.State {
        val configuration = CommonConfigurationFactory.create()
        return PaymentElementLoader.State(
            config = configuration,
            customer = null,
            paymentSelection = null,
            validationError = null,
            paymentMethodMetadata = paymentMethodMetadata,
        )
    }
}
