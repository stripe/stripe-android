package com.stripe.android.common.analytics.experiment

import com.google.common.truth.Truth.assertThat
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadataFactory
import com.stripe.android.model.ElementsSession
import com.stripe.android.model.ElementsSession.ExperimentAssignment
import com.stripe.android.model.PaymentIntentFixtures
import com.stripe.android.model.PaymentMethod
import com.stripe.android.model.PaymentMethodFixtures
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.analytics.EventReporter
import com.stripe.android.paymentsheet.analytics.FakeEventReporter
import com.stripe.android.paymentsheet.model.PaymentSelection
import com.stripe.android.paymentsheet.state.PaymentElementLoader
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
    fun `layout is horizontal for PaymentSheet with Horizontal layout`() = runScenario {
        invoke(
            integrationConfiguration = paymentSheetConfiguration(
                layout = PaymentSheet.PaymentMethodLayout.Horizontal,
            ),
        )

        val experiment = awaitExperiment()
        assertThat(experiment.dimensions).containsEntry("in_app_elements_layout", "horizontal")
    }

    @Test
    fun `layout is vertical for PaymentSheet with Vertical layout`() = runScenario {
        invoke(
            integrationConfiguration = paymentSheetConfiguration(
                layout = PaymentSheet.PaymentMethodLayout.Vertical,
            ),
        )

        val experiment = awaitExperiment()
        assertThat(experiment.dimensions).containsEntry("in_app_elements_layout", "vertical")
    }

    @Test
    fun `layout is horizontal for PaymentSheet with Automatic layout`() = runScenario {
        invoke(
            integrationConfiguration = paymentSheetConfiguration(
                layout = PaymentSheet.PaymentMethodLayout.Automatic,
            ),
        )

        val experiment = awaitExperiment()
        assertThat(experiment.dimensions).containsEntry("in_app_elements_layout", "horizontal")
    }

    @Test
    fun `saved_payment_method_count reflects total saved PMs`() = runScenario {
        invoke(
            savedPaymentMethods = listOf(
                createCardPaymentMethod(id = "pm_1"),
                createCardPaymentMethod(id = "pm_2"),
                createSepaPaymentMethod(id = "pm_3"),
            ),
        )

        val experiment = awaitExperiment()
        assertThat(experiment.dimensions).containsEntry("saved_payment_method_count", "3")
    }

    @Test
    fun `saved_card_payment_method_count counts only card PMs`() = runScenario {
        invoke(
            savedPaymentMethods = listOf(
                createCardPaymentMethod(id = "pm_1"),
                createCardPaymentMethod(id = "pm_2"),
                createSepaPaymentMethod(id = "pm_3"),
            ),
        )

        val experiment = awaitExperiment()
        assertThat(experiment.dimensions).containsEntry("saved_card_payment_method_count", "2")
    }

    @Test
    fun `saved_card_payment_method_with_card_art_count counts cards with art`() = runScenario {
        invoke(
            savedPaymentMethods = listOf(
                createCardPaymentMethod(id = "pm_1", hasCardArt = true),
                createCardPaymentMethod(id = "pm_2", hasCardArt = false),
                createCardPaymentMethod(id = "pm_3", hasCardArt = true),
            ),
        )

        val experiment = awaitExperiment()
        assertThat(experiment.dimensions).containsEntry("saved_card_payment_method_with_card_art_count", "2")
    }

    @Test
    fun `selected_payment_method_type is set from default selection`() = runScenario {
        val savedPm = createCardPaymentMethod(id = "pm_1")

        invoke(
            savedPaymentMethods = listOf(savedPm),
            defaultPaymentSelection = PaymentSelection.Saved(savedPm),
        )

        val experiment = awaitExperiment()
        assertThat(experiment.dimensions).containsEntry("selected_payment_method_type", "card")
    }

    @Test
    fun `selected_payment_method_type is google_pay for GooglePay selection`() = runScenario {
        invoke(
            defaultPaymentSelection = PaymentSelection.GooglePay,
        )

        val experiment = awaitExperiment()
        assertThat(experiment.dimensions).containsEntry("selected_payment_method_type", "google_pay")
    }

    @Test
    fun `selected_payment_method_type is null when no selection`() = runScenario {
        invoke(
            defaultPaymentSelection = null,
        )

        val experiment = awaitExperiment()
        assertThat(experiment.dimensions).containsEntry("selected_payment_method_type", "null")
    }

    @Test
    fun `selected_payment_method_has_card_art is true when saved card has art`() = runScenario {
        val savedPm = createCardPaymentMethod(id = "pm_1", hasCardArt = true)

        invoke(
            savedPaymentMethods = listOf(savedPm),
            defaultPaymentSelection = PaymentSelection.Saved(savedPm),
        )

        val experiment = awaitExperiment()
        assertThat(experiment.dimensions).containsEntry("selected_payment_method_has_card_art", "true")
    }

    @Test
    fun `selected_payment_method_has_card_art is false when saved card has no art`() = runScenario {
        val savedPm = createCardPaymentMethod(id = "pm_1", hasCardArt = false)

        invoke(
            savedPaymentMethods = listOf(savedPm),
            defaultPaymentSelection = PaymentSelection.Saved(savedPm),
        )

        val experiment = awaitExperiment()
        assertThat(experiment.dimensions).containsEntry("selected_payment_method_has_card_art", "false")
    }

    @Test
    fun `selected_payment_method_has_card_art is false for non-saved selection`() = runScenario {
        invoke(
            defaultPaymentSelection = PaymentSelection.GooglePay,
        )

        val experiment = awaitExperiment()
        assertThat(experiment.dimensions).containsEntry("selected_payment_method_has_card_art", "false")
    }

    @Test
    fun `all counts are zero when no saved PMs`() = runScenario {
        invoke(
            savedPaymentMethods = emptyList(),
        )

        val experiment = awaitExperiment()
        assertThat(experiment.dimensions).containsEntry("saved_payment_method_count", "0")
        assertThat(experiment.dimensions).containsEntry("saved_card_payment_method_count", "0")
        assertThat(experiment.dimensions).containsEntry("saved_card_payment_method_with_card_art_count", "0")
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
            savedPaymentMethods: List<PaymentMethod> = emptyList(),
            integrationConfiguration: PaymentElementLoader.Configuration = paymentSheetConfiguration(),
            defaultPaymentSelection: PaymentSelection? = null,
        ): Boolean {
            return sut(
                elementsSession = elementsSession,
                paymentMethodMetadata = paymentMethodMetadata,
                savedPaymentMethods = savedPaymentMethods,
                integrationConfiguration = integrationConfiguration,
                defaultPaymentSelection = defaultPaymentSelection,
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

        private fun paymentSheetConfiguration(
            layout: PaymentSheet.PaymentMethodLayout = PaymentSheet.PaymentMethodLayout.Automatic,
        ): PaymentElementLoader.Configuration.PaymentSheet {
            return PaymentElementLoader.Configuration.PaymentSheet(
                PaymentSheet.Configuration.Builder("Test").paymentMethodLayout(layout).build()
            )
        }

        private fun createCardPaymentMethod(
            id: String,
            hasCardArt: Boolean = false,
        ): PaymentMethod {
            return PaymentMethodFixtures.CARD_PAYMENT_METHOD.copy(
                id = id,
                card = PaymentMethodFixtures.CARD_PAYMENT_METHOD.card?.copy(
                    cardArt = if (hasCardArt) {
                        PaymentMethod.Card.CardArt(
                            artImage = PaymentMethod.Card.CardArt.ArtImage(
                                format = "png",
                                url = "https://example.com/art.png",
                            ),
                            programName = "Test Program",
                        )
                    } else {
                        null
                    },
                ),
            )
        }

        private fun createSepaPaymentMethod(id: String): PaymentMethod {
            return PaymentMethod(
                id = id,
                created = 1000L,
                liveMode = false,
                type = PaymentMethod.Type.SepaDebit,
                code = "sepa_debit",
            )
        }
    }
}
