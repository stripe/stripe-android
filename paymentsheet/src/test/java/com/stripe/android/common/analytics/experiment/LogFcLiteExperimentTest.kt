package com.stripe.android.common.analytics.experiment

import com.google.common.truth.Truth.assertThat
import com.stripe.android.core.version.StripeSdkVersion
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
    fun `logs exposure for both experiments when both are assigned`() = runTest {
        val elementsSession = createElementsSession(
            experimentsData = ElementsSession.ExperimentsData(
                arbId = "test_arb_id",
                experimentAssignments = mapOf(
                    CONNECTIONS_FC_LITE_VS_NATIVE to "treatment",
                    CONNECTIONS_FC_LITE_VS_NATIVE_AA to "control",
                )
            )
        )

        createExperiment()(elementsSession)

        val firstExposure = eventReporter.experimentExposureCalls.awaitItem()
        val secondExposure = eventReporter.experimentExposureCalls.awaitItem()

        val experiments = listOf(firstExposure.experiment, secondExposure.experiment)
        assertThat(experiments.all { it is LoggableExperiment.ConnectionsFCLiteVsNative }).isTrue()

        val assignments = experiments.map { it.experiment }.toSet()
        assertThat(assignments).isEqualTo(setOf(CONNECTIONS_FC_LITE_VS_NATIVE, CONNECTIONS_FC_LITE_VS_NATIVE_AA))
    }

    @Test
    fun `logs correct group for each experiment`() = runTest {
        val elementsSession = createElementsSession(
            experimentsData = ElementsSession.ExperimentsData(
                arbId = "test_arb_id",
                experimentAssignments = mapOf(
                    CONNECTIONS_FC_LITE_VS_NATIVE to "treatment",
                    CONNECTIONS_FC_LITE_VS_NATIVE_AA to "control",
                )
            )
        )

        createExperiment()(elementsSession)

        val exposures = listOf(
            eventReporter.experimentExposureCalls.awaitItem().experiment,
            eventReporter.experimentExposureCalls.awaitItem().experiment,
        ).map { it as LoggableExperiment.ConnectionsFCLiteVsNative }

        val mainExperiment = exposures.single { it.experiment == CONNECTIONS_FC_LITE_VS_NATIVE }
        val aaExperiment = exposures.single { it.experiment == CONNECTIONS_FC_LITE_VS_NATIVE_AA }

        assertThat(mainExperiment.group).isEqualTo("treatment")
        assertThat(aaExperiment.group).isEqualTo("control")
    }

    @Test
    fun `logs correct dimensions`() = runTest {
        val elementsSession = createElementsSession(
            experimentsData = ElementsSession.ExperimentsData(
                arbId = "test_arb_id",
                experimentAssignments = mapOf(CONNECTIONS_FC_LITE_VS_NATIVE to "treatment")
            ),
            orderedPaymentMethodTypesAndWallets = listOf("card", "us_bank_account"),
        )

        createExperiment()(elementsSession)

        val exposure = eventReporter.experimentExposureCalls.awaitItem().experiment
            as LoggableExperiment.ConnectionsFCLiteVsNative

        assertThat(exposure.arbId).isEqualTo("test_arb_id")
        assertThat(exposure.elementsSessionId).isEqualTo("session_1234")
        assertThat(exposure.mobileSessionId).isEqualTo("test_mobile_session_id")
        assertThat(exposure.mobileSdkVersion).isEqualTo(StripeSdkVersion.VERSION_NAME)
        assertThat(exposure.availableLpms).isEqualTo("card,us_bank_account")
    }

    @Test
    fun `sets fc_sdk_availability to FULL when full FC SDK is available`() = runTest {
        val elementsSession = createElementsSession(
            experimentsData = ElementsSession.ExperimentsData(
                arbId = "test_arb_id",
                experimentAssignments = mapOf(CONNECTIONS_FC_LITE_VS_NATIVE to "treatment")
            )
        )

        createExperiment(isFullSdkAvailable = IsFinancialConnectionsSdkAvailable { true })(elementsSession)

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

        createExperiment(isFullSdkAvailable = IsFinancialConnectionsSdkAvailable { false })(elementsSession)

        val exposure = eventReporter.experimentExposureCalls.awaitItem().experiment
            as LoggableExperiment.ConnectionsFCLiteVsNative

        assertThat(exposure.fcSdkAvailability).isEqualTo("LITE")
    }

    @Test
    fun `does not log when experimentsData is null`() = runTest {
        val elementsSession = createElementsSession(experimentsData = null)

        createExperiment()(elementsSession)

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

        createExperiment()(elementsSession)

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

        createExperiment()(elementsSession)

        val exposure = eventReporter.experimentExposureCalls.awaitItem().experiment
            as LoggableExperiment.ConnectionsFCLiteVsNative

        assertThat(exposure.experiment).isEqualTo(CONNECTIONS_FC_LITE_VS_NATIVE)
        eventReporter.experimentExposureCalls.expectNoEvents()
    }

    private fun createElementsSession(
        experimentsData: ElementsSession.ExperimentsData? = null,
        orderedPaymentMethodTypesAndWallets: List<String> =
            PaymentIntentFixtures.PI_REQUIRES_PAYMENT_METHOD.paymentMethodTypes,
    ): ElementsSession {
        return ElementsSession(
            stripeIntent = PaymentIntentFixtures.PI_REQUIRES_PAYMENT_METHOD,
            cardBrandChoice = null,
            merchantCountry = null,
            isGooglePayEnabled = false,
            customer = null,
            linkSettings = null,
            orderedPaymentMethodTypesAndWallets = orderedPaymentMethodTypesAndWallets,
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
