package com.stripe.android.common.analytics.experiment

import com.google.common.truth.Truth.assertThat
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadataFactory
import com.stripe.android.model.ElementsSession
import com.stripe.android.model.ElementsSession.ExperimentAssignment
import com.stripe.android.model.PaymentIntentFixtures
import com.stripe.android.model.PaymentMethod
import com.stripe.android.paymentsheet.analytics.EventReporter
import com.stripe.android.paymentsheet.analytics.FakeEventReporter
import kotlinx.coroutines.test.runTest
import org.junit.Test

class DefaultLogCardArtExperimentTest {

    @Test
    fun `does not log when experimentsData is null`() = runScenario {
        invoke(
            elementsSession = createElementsSession(experimentsData = null),
        )

        eventReporter.experimentExposureCalls.expectNoEvents()
    }

    @Test
    fun `does not log when experiment assignment is missing`() = runScenario {
        invoke(
            elementsSession = createElementsSession(
                experimentsData = ElementsSession.ExperimentsData(
                    arbId = "arb_123",
                    experimentAssignments = emptyMap(),
                ),
            ),
        )

        eventReporter.experimentExposureCalls.expectNoEvents()
    }

    @Test
    fun `logs exposure with correct group when experiment is assigned`() = runScenario {
        invoke(
            elementsSession = createElementsSession(
                experimentsData = ElementsSession.ExperimentsData(
                    arbId = "arb_123",
                    experimentAssignments = mapOf(
                        ExperimentAssignment.OCS_MOBILE_CARD_ART to "treatment",
                    ),
                ),
            ),
        )

        val call = eventReporter.experimentExposureCalls.awaitItem()
        val experiment = call.experiment as LoggableExperiment.OcsMobileCardArt
        assertThat(experiment.group).isEqualTo("treatment")
        assertThat(experiment.arbId).isEqualTo("arb_123")
        assertThat(experiment.experiment).isEqualTo(ExperimentAssignment.OCS_MOBILE_CARD_ART)
    }

    @Test
    fun `returns true when variant is treatment`() = runScenario {
        val result = invoke()

        awaitExperiment()
        assertThat(result).isTrue()
    }

    @Test
    fun `returns false when variant is control`() = runScenario {
        val result = invoke(
            elementsSession = createElementsSession(
                experimentsData = ElementsSession.ExperimentsData(
                    arbId = "arb_123",
                    experimentAssignments = mapOf(
                        ExperimentAssignment.OCS_MOBILE_CARD_ART to "control",
                    ),
                ),
            ),
        )

        awaitExperiment()
        assertThat(result).isFalse()
    }

    @Test
    fun `returns false when experimentsData is null`() = runScenario {
        val result = invoke(
            elementsSession = createElementsSession(experimentsData = null),
        )

        assertThat(result).isFalse()
    }

    @Test
    fun `returns false when experiment assignment is missing`() = runScenario {
        val result = invoke(
            elementsSession = createElementsSession(
                experimentsData = ElementsSession.ExperimentsData(
                    arbId = "arb_123",
                    experimentAssignments = emptyMap(),
                ),
            ),
        )

        assertThat(result).isFalse()
    }

    @Test
    fun `has_card_art dimension is true when card arts present`() = runScenario {
        val result = invoke(
            paymentMethodMetadata = PaymentMethodMetadataFactory.create(
                cardArts = listOf(
                    PaymentMethod.Card.CardArt(
                        artImage = PaymentMethod.Card.CardArt.ArtImage(
                            format = "png",
                            url = "https://example.com/art.png",
                        ),
                        programName = "Test Program",
                    ),
                ),
            ),
        )

        val experiment = awaitExperiment()
        assertThat(experiment.dimensions).containsEntry("has_card_art", "true")
        assertThat(result).isTrue()
    }

    @Test
    fun `has_card_art dimension is false when no card arts`() = runScenario {
        invoke(
            paymentMethodMetadata = PaymentMethodMetadataFactory.create(
                cardArts = emptyList(),
            ),
        )

        val experiment = awaitExperiment()
        assertThat(experiment.dimensions).containsEntry("has_card_art", "false")
    }

    private fun runScenario(
        block: suspend Scenario.() -> Unit,
    ) = runTest {
        val eventReporter = FakeEventReporter()

        val sut = DefaultLogCardArtExperiment(
            eventReporter = eventReporter,
            mode = EventReporter.Mode.Complete,
        )

        Scenario(
            sut = sut,
            eventReporter = eventReporter,
        ).apply { block() }

        eventReporter.validate()
    }

    private data class Scenario(
        val sut: DefaultLogCardArtExperiment,
        val eventReporter: FakeEventReporter,
    ) {
        private val defaultElementsSession = createElementsSession(
            experimentsData = ElementsSession.ExperimentsData(
                arbId = "arb_123",
                experimentAssignments = mapOf(
                    ExperimentAssignment.OCS_MOBILE_CARD_ART to "treatment",
                ),
            ),
        )

        fun invoke(
            elementsSession: ElementsSession = defaultElementsSession,
            paymentMethodMetadata: com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadata =
                PaymentMethodMetadataFactory.create(),
        ): Boolean {
            return sut(
                elementsSession = elementsSession,
                paymentMethodMetadata = paymentMethodMetadata,
            )
        }

        suspend fun awaitExperiment(): LoggableExperiment.OcsMobileCardArt {
            val call = eventReporter.experimentExposureCalls.awaitItem()
            return call.experiment as LoggableExperiment.OcsMobileCardArt
        }
    }

    companion object {
        private fun createElementsSession(
            experimentsData: ElementsSession.ExperimentsData?,
        ): ElementsSession {
            return ElementsSession(
                stripeIntent = PaymentIntentFixtures.PI_REQUIRES_PAYMENT_METHOD,
                cardBrandChoice = null,
                merchantCountry = null,
                isGooglePayEnabled = false,
                customer = null,
                linkSettings = null,
                orderedPaymentMethodTypesAndWallets =
                    PaymentIntentFixtures.PI_REQUIRES_PAYMENT_METHOD.paymentMethodTypes,
                customPaymentMethods = emptyList(),
                externalPaymentMethodData = null,
                paymentMethodSpecs = null,
                elementsSessionId = "session_1234",
                flags = emptyMap(),
                experimentsData = experimentsData,
                passiveCaptcha = null,
                merchantLogoUrl = null,
                elementsSessionConfigId = null,
                accountId = "acct_123",
                merchantId = "acct_123",
            )
        }
    }
}
