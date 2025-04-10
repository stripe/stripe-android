package com.stripe.android.common.analytics.experiment

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.google.common.truth.Truth.assertThat
import com.stripe.android.common.analytics.experiment.LoggableExperiment.LinkGlobalHoldback.EmailRecognitionSource
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
import com.stripe.android.model.ElementsSession.Customer.Components
import com.stripe.android.model.ElementsSession.Customer.Components.CustomerSheet
import com.stripe.android.model.ElementsSession.Customer.Components.MobilePaymentElement
import com.stripe.android.model.ElementsSession.ExperimentAssignment.LINK_GLOBAL_HOLD_BACK
import com.stripe.android.model.ElementsSession.Flag.ELEMENTS_DISABLE_LINK_GLOBAL_HOLDBACK_LOOKUP
import com.stripe.android.model.PaymentIntentFixtures
import com.stripe.android.paymentsheet.analytics.EventReporter
import com.stripe.android.paymentsheet.analytics.FakeEventReporter
import com.stripe.android.paymentsheet.state.DefaultRetrieveCustomerEmail
import com.stripe.android.paymentsheet.state.LinkState
import com.stripe.android.paymentsheet.state.PaymentElementLoader
import com.stripe.android.paymentsheet.state.RetrieveCustomerEmail
import com.stripe.android.testing.FakeLogger
import com.stripe.android.testing.FeatureFlagTestRule
import com.stripe.android.utils.FakeCustomerRepository
import com.stripe.android.utils.FakeLinkConfigurationCoordinator
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
    private lateinit var linkConfigurationCoordinator: FakeLinkConfigurationCoordinator

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
        linkConfigurationCoordinator = FakeLinkConfigurationCoordinator()

        logLinkGlobalHoldbackExposure = DefaultLogLinkGlobalHoldbackExposure(
            linkDisabledApiRepository = linkRepository,
            eventReporter = eventReporter,
            logger = logger,
            workContext = testDispatcher,
            retrieveCustomerEmail = retrieveCustomerEmail,
            linkConfigurationCoordinator = linkConfigurationCoordinator,
            mode = EventReporter.Mode.Complete,
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
            ),
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
            ),
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
    fun `invoke should not log exposure when lookup kill-switch flag is on`() = runTest {
        val elementsSession = createElementsSession(
            experimentsData = ElementsSession.ExperimentsData(
                arbId = "test_arb_id",
                experimentAssignments = mapOf(
                    LINK_GLOBAL_HOLD_BACK to "holdback"
                )
            )
        ).copy(
            flags = mapOf(ELEMENTS_DISABLE_LINK_GLOBAL_HOLDBACK_LOOKUP to true)
        )
        val state = createElementsState(PaymentMethodMetadataFactory.create())

        logLinkGlobalHoldbackExposure(elementsSession, state)

        eventReporter.experimentExposureCalls.expectNoEvents()
    }

    @Test
    fun `invoke should log error when exception occurs`() {
        val elementsSession = createElementsSession()
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
            ),
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
        assertThat(experiment).isInstanceOf(LoggableExperiment.LinkGlobalHoldback::class.java)
        assertThat(experiment.group).isEqualTo("holdback")
        assertThat(experiment.isReturningLinkUser).isTrue()
    }

    @Test
    fun `invoke should not log exposure and log local error if lookup fails`() = runTest {
        val elementsSession = createElementsSession(
            experimentsData = ElementsSession.ExperimentsData(
                arbId = "test_arb_id",
                experimentAssignments = mapOf(
                    LINK_GLOBAL_HOLD_BACK to "holdback"
                )
            ),
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
            ),
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
        assertThat(exposureCall.experiment.isReturningLinkUser).isFalse()
    }

    @Test
    fun `invoke should log exposure with useLinkNative true when link native is used`() = runTest {
        val elementsSession = createElementsSession(
            experimentsData = ElementsSession.ExperimentsData(
                arbId = "test_arb_id",
                experimentAssignments = mapOf(
                    LINK_GLOBAL_HOLD_BACK to "holdback"
                )
            ),
        )
        val state = createElementsState(
            paymentMethodMetadata = PaymentMethodMetadataFactory.create(
                linkState = LinkState(
                    configuration = TestFactory.LINK_CONFIGURATION.copy(useAttestationEndpointsForLink = true),
                    loginState = LinkState.LoginState.NeedsVerification,
                    signupMode = null
                )
            )
        )

        logLinkGlobalHoldbackExposure(elementsSession, state)

        val exposureCall = eventReporter.experimentExposureCalls.awaitItem()
        assertTrue(exposureCall.experiment is LoggableExperiment.LinkGlobalHoldback)
        assertThat(exposureCall.experiment.useLinkNative).isTrue()
    }

    @Test
    fun `invoke should log exposure with emailRecognitionSource EMAIL when customer email is present`() = runTest {
        val elementsSession = createElementsSession(
            experimentsData = ElementsSession.ExperimentsData(
                arbId = "test_arb_id",
                experimentAssignments = mapOf(
                    LINK_GLOBAL_HOLD_BACK to "holdback"
                )
            ),
        )
        val state = createElementsState(
            paymentMethodMetadata = PaymentMethodMetadataFactory.create(
                linkState = LinkState(
                    configuration = TestFactory.LINK_CONFIGURATION.copy(
                        customerInfo = TestFactory.LINK_CONFIGURATION.customerInfo.copy(email = "test@example.com")
                    ),
                    loginState = LinkState.LoginState.NeedsVerification,
                    signupMode = null
                )
            )
        )

        logLinkGlobalHoldbackExposure(elementsSession, state)

        val exposureCall = eventReporter.experimentExposureCalls.awaitItem()
        assertTrue(exposureCall.experiment is LoggableExperiment.LinkGlobalHoldback)
        assertThat(exposureCall.experiment.emailRecognitionSource).isEqualTo(EmailRecognitionSource.EMAIL)
    }

    @Test
    fun `invoke should log exposure with spmEnabled true when SPM is enabled`() = runTest {
        val elementsSession = createElementsSession(
            experimentsData = ElementsSession.ExperimentsData(
                arbId = "test_arb_id",
                experimentAssignments = mapOf(
                    LINK_GLOBAL_HOLD_BACK to "holdback"
                )
            ),
            customer = createCustomer(
                MobilePaymentElement.Enabled(
                    isPaymentMethodSaveEnabled = true,
                    isPaymentMethodRemoveEnabled = true,
                    canRemoveLastPaymentMethod = true,
                    isPaymentMethodSetAsDefaultEnabled = true,
                    allowRedisplayOverride = null
                )
            )
        )

        val state = createElementsState(PaymentMethodMetadataFactory.create())

        logLinkGlobalHoldbackExposure(elementsSession, state)

        val exposureCall = eventReporter.experimentExposureCalls.awaitItem()
        assertTrue(exposureCall.experiment is LoggableExperiment.LinkGlobalHoldback)
        assertThat(exposureCall.experiment.spmEnabled).isTrue()
    }

    private fun createElementsSession(
        experimentsData: ElementsSession.ExperimentsData? = null,
        customer: ElementsSession.Customer = createCustomer(MobilePaymentElement.Disabled),
    ): ElementsSession {
        return ElementsSession(
            stripeIntent = PaymentIntentFixtures.PI_REQUIRES_PAYMENT_METHOD,
            cardBrandChoice = null,
            merchantCountry = null,
            isGooglePayEnabled = false,
            customer = customer,
            linkSettings = null,
            customPaymentMethods = emptyList(),
            externalPaymentMethodData = null,
            paymentMethodSpecs = null,
            elementsSessionId = "session_1234",
            flags = emptyMap(),
            experimentsData = experimentsData
        )
    }

    private fun createCustomer(
        mobilePaymentElementComponent: MobilePaymentElement
    ): ElementsSession.Customer =
        ElementsSession.Customer(
            paymentMethods = listOf(),
            session = ElementsSession.Customer.Session(
                id = "cuss_123",
                customerId = "cus_123",
                liveMode = false,
                apiKey = "123",
                apiKeyExpiry = 999999999,
                components = Components(
                    mobilePaymentElement = mobilePaymentElementComponent,
                    customerSheet = CustomerSheet.Disabled,
                )
            ),
            defaultPaymentMethod = null,
        )

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
