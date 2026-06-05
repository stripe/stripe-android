package com.stripe.android.common.analytics.experiment

import com.google.common.truth.Truth.assertThat
import com.stripe.android.core.version.StripeSdkVersion
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadataFactory
import com.stripe.android.model.ElementsSession
import com.stripe.android.model.ElementsSession.ExperimentAssignment.CONNECTIONS_FC_LITE_VS_NATIVE
import com.stripe.android.model.ElementsSession.ExperimentAssignment.CONNECTIONS_FC_LITE_VS_NATIVE_AA
import com.stripe.android.model.PaymentIntentFixtures
import com.stripe.android.payments.financialconnections.IsFinancialConnectionsSdkAvailable
import com.stripe.android.paymentsheet.analytics.FakeEventReporter
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test

class LogFcLiteExperimentTest {

    private lateinit var eventReporter: FakeEventReporter

    @Before
    fun setUp() {
        eventReporter = FakeEventReporter()
    }

    private fun createExperiment(
        isFullSdkAvailable: IsFinancialConnectionsSdkAvailable = IsFinancialConnectionsSdkAvailable { true },
    ) = DefaultLogFcLiteExperiment(
        eventReporter = eventReporter,
        mobileSessionId = "test_mobile_session_id",
        isFullSdkAvailable = isFullSdkAvailable,
    )

    @Test
    fun `logs exposure for main experiment when assigned`() = runTest {
        val elementsSession = createElementsSession(
            experimentsData = ElementsSession.ExperimentsData(
                arbId = "test_arb_id",
                experimentAssignments = mapOf(CONNECTIONS_FC_LITE_VS_NATIVE to "treatment")
            )
        )

        createExperiment()(elementsSession, PaymentMethodMetadataFactory.create())

        val exposure = eventReporter.experimentExposureCalls.awaitItem().experiment
            as LoggableExperiment.ConnectionsFCLiteVsNative
        assertThat(exposure.experiment).isEqualTo(CONNECTIONS_FC_LITE_VS_NATIVE)
        eventReporter.experimentExposureCalls.expectNoEvents()
    }

    @Test
    fun `logs exposure for AA experiment when assigned`() = runTest {
        val elementsSession = createElementsSession(
            experimentsData = ElementsSession.ExperimentsData(
                arbId = "test_arb_id",
                experimentAssignments = mapOf(CONNECTIONS_FC_LITE_VS_NATIVE_AA to "control")
            )
        )

        createExperiment()(elementsSession, PaymentMethodMetadataFactory.create())

        val exposure = eventReporter.experimentExposureCalls.awaitItem().experiment
            as LoggableExperiment.ConnectionsFCLiteVsNative
        assertThat(exposure.experiment).isEqualTo(CONNECTIONS_FC_LITE_VS_NATIVE_AA)
        eventReporter.experimentExposureCalls.expectNoEvents()
    }

    @Test
    fun `logs correct dimensions`() = runTest {
        val metadata = PaymentMethodMetadataFactory.create()
        val elementsSession = createElementsSession(
            experimentsData = ElementsSession.ExperimentsData(
                arbId = "test_arb_id",
                experimentAssignments = mapOf(CONNECTIONS_FC_LITE_VS_NATIVE to "treatment")
            ),
        )

        createExperiment()(elementsSession, metadata)

        val exposure = eventReporter.experimentExposureCalls.awaitItem().experiment
            as LoggableExperiment.ConnectionsFCLiteVsNative

        val expectedLpms = metadata.sortedSupportedPaymentMethods().map { it.code }.joinToString(",")
        assertThat(exposure.arbId).isEqualTo("test_arb_id")
        assertThat(exposure.elementsSessionId).isEqualTo("session_1234")
        assertThat(exposure.mobileSessionId).isEqualTo("test_mobile_session_id")
        assertThat(exposure.mobileSdkVersion).isEqualTo(StripeSdkVersion.VERSION_NAME)
        assertThat(exposure.availableLpms).isEqualTo(expectedLpms)
    }

    @Test
    fun `sets fc_sdk_availability to FULL when full FC SDK is available`() = runTest {
        val elementsSession = createElementsSession(
            experimentsData = ElementsSession.ExperimentsData(
                arbId = "test_arb_id",
                experimentAssignments = mapOf(CONNECTIONS_FC_LITE_VS_NATIVE to "treatment")
            )
        )

        createExperiment(isFullSdkAvailable = IsFinancialConnectionsSdkAvailable { true })(
            elementsSession,
            PaymentMethodMetadataFactory.create()
        )

        val exposure = eventReporter.experimentExposureCalls.awaitItem().experiment
            as LoggableExperiment.ConnectionsFCLiteVsNative

        assertThat(exposure.fcSdkAvailability).isEqualTo("FULL")
    }

    @Test
    fun `sets fc_sdk_availability to LITE when full FC SDK is unavailable`() = runTest {
        val elementsSession = createElementsSession(
            experimentsData = ElementsSession.ExperimentsData(
                arbId = "test_arb_id",
                experimentAssignments = mapOf(CONNECTIONS_FC_LITE_VS_NATIVE to "control")
            )
        )

        createExperiment(isFullSdkAvailable = IsFinancialConnectionsSdkAvailable { false })(
            elementsSession,
            PaymentMethodMetadataFactory.create()
        )

        val exposure = eventReporter.experimentExposureCalls.awaitItem().experiment
            as LoggableExperiment.ConnectionsFCLiteVsNative

        assertThat(exposure.fcSdkAvailability).isEqualTo("LITE")
    }

    @Test
    fun `does not log when experimentsData is null`() = runTest {
        val elementsSession = createElementsSession(experimentsData = null)

        createExperiment()(elementsSession, PaymentMethodMetadataFactory.create())

        eventReporter.experimentExposureCalls.expectNoEvents()
    }

    @Test
    fun `does not log when experiment assignment is absent`() = runTest {
        val elementsSession = createElementsSession(
            experimentsData = ElementsSession.ExperimentsData(
                arbId = "test_arb_id",
                experimentAssignments = emptyMap()
            )
        )

        createExperiment()(elementsSession, PaymentMethodMetadataFactory.create())

        eventReporter.experimentExposureCalls.expectNoEvents()
    }

    @Test
    fun `logs only the experiment that is assigned`() = runTest {
        val elementsSession = createElementsSession(
            experimentsData = ElementsSession.ExperimentsData(
                arbId = "test_arb_id",
                experimentAssignments = mapOf(CONNECTIONS_FC_LITE_VS_NATIVE to "control")
            )
        )

        createExperiment()(elementsSession, PaymentMethodMetadataFactory.create())

        val exposure = eventReporter.experimentExposureCalls.awaitItem().experiment
            as LoggableExperiment.ConnectionsFCLiteVsNative

        assertThat(exposure.experiment).isEqualTo(CONNECTIONS_FC_LITE_VS_NATIVE)
        eventReporter.experimentExposureCalls.expectNoEvents()
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
            orderedPaymentMethodTypesAndWallets = PaymentIntentFixtures.PI_REQUIRES_PAYMENT_METHOD.paymentMethodTypes,
            customPaymentMethods = emptyList(),
            externalPaymentMethodData = null,
            paymentMethodSpecs = null,
            elementsSessionId = "session_1234",
            flags = emptyMap(),
            experimentsData = experimentsData,
            passiveCaptcha = null,
            merchantLogoUrl = null,
            elementsSessionConfigId = null,
            accountId = null,
            merchantId = null,
        )
    }
}
