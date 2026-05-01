package com.stripe.android.common.analytics.experiment

import com.google.common.truth.Truth.assertThat
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadata
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadataFactory
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodOrientation
import com.stripe.android.model.ElementsSession
import com.stripe.android.model.ElementsSession.ExperimentAssignment
import com.stripe.android.model.PaymentIntentFixtures
import com.stripe.android.model.PaymentMethod
import com.stripe.android.model.PaymentMethodCode
import com.stripe.android.model.PaymentMethodMessageLearnMore
import com.stripe.android.model.PaymentMethodMessagePromotion
import com.stripe.android.paymentsheet.analytics.EventReporter
import com.stripe.android.paymentsheet.analytics.FakeEventReporter
import kotlinx.coroutines.test.runTest
import org.junit.Test

class DefaultPaymentMethodMessagePromotionsExperimentHandlerTest {

    @Test
    fun `does not log when experimentsData is null`() = runScenario {
        logExposure(
            metadata = defaultMetadata.copy(
                experimentsData = null
            )
        )
        eventReporter.experimentExposureCalls.expectNoEvents()
    }

    @Test
    fun `does not log when experiment assignment is missing`() = runScenario {
        logExposure(
            metadata = defaultMetadata.copy(
                experimentsData = ElementsSession.ExperimentsData(
                    arbId = "arb_123",
                    experimentAssignments = mapOf()
                )
            )
        )

        eventReporter.experimentExposureCalls.expectNoEvents()
    }

    @Test
    fun `logs exposure with correct group for treatment`() = runScenario {
        logExposure()

        val call = eventReporter.experimentExposureCalls.awaitItem()
        val experiment = call.experiment as LoggableExperiment.OcsMobilePaymentMethodMessagingPromotions
        assertThat(experiment.group).isEqualTo("treatment")
        assertThat(experiment.arbId).isEqualTo("arb_123")
        assertThat(experiment.experiment).isEqualTo(ExperimentAssignment.OCS_MOBILE_PAYMENT_METHOD_MESSAGING_PROMOTIONS)
    }

    @Test
    fun `logs exposure with correct group for control`() = runScenario {
        logExposure(
            metadata = defaultMetadata.copy(
                experimentsData = ElementsSession.ExperimentsData(
                    arbId = "arb_123",
                    experimentAssignments = mapOf(
                        ExperimentAssignment.OCS_MOBILE_PAYMENT_METHOD_MESSAGING_PROMOTIONS to "control"
                    )
                )
            )
        )

        val call = eventReporter.experimentExposureCalls.awaitItem()
        val experiment = call.experiment as LoggableExperiment.OcsMobilePaymentMethodMessagingPromotions
        assertThat(experiment.group).isEqualTo("control")
        assertThat(experiment.arbId).isEqualTo("arb_123")
        assertThat(experiment.experiment).isEqualTo(ExperimentAssignment.OCS_MOBILE_PAYMENT_METHOD_MESSAGING_PROMOTIONS)
        assertThat(experiment.selectedPaymentMethodType).isEqualTo("affirm")
    }

    @Test
    fun `logs promotionDisplayedSuccessfully as false when promotion is null and in treatment`() = runScenario {
        logExposure(
            promotion = null
        )

        val call = eventReporter.experimentExposureCalls.awaitItem()
        val experiment = call.experiment as LoggableExperiment.OcsMobilePaymentMethodMessagingPromotions
        assertThat(experiment.promotionDisplayedSuccessfully).isFalse()
        eventReporter.pmmPromotionsIncomplete.awaitItem()
    }

    @Test
    fun `logs promotionDisplayedSuccessfully as null when promotion is null and in control`() = runScenario {
        logExposure(
            promotion = null,
            metadata = defaultMetadata.copy(
                experimentsData = ElementsSession.ExperimentsData(
                    arbId = "arb_123",
                    experimentAssignments = mapOf(
                        ExperimentAssignment.OCS_MOBILE_PAYMENT_METHOD_MESSAGING_PROMOTIONS to "control"
                    )
                )
            )
        )

        val call = eventReporter.experimentExposureCalls.awaitItem()
        val experiment = call.experiment as LoggableExperiment.OcsMobilePaymentMethodMessagingPromotions
        assertThat(experiment.promotionDisplayedSuccessfully).isNull()
        eventReporter.pmmPromotionsIncomplete.expectNoEvents()
    }

    @Test
    fun `logs vertical layout correctly`() = runScenario {
        logExposure(
            metadata = defaultMetadata.copy(
                paymentMethodOrientation = PaymentMethodOrientation.Vertical
            )
        )

        val call = eventReporter.experimentExposureCalls.awaitItem()
        val experiment = call.experiment as LoggableExperiment.OcsMobilePaymentMethodMessagingPromotions
        assertThat(experiment.layout).isEqualTo("vertical")
    }

    @Test
    fun `logs horizontal layout correctly`() = runScenario {
        logExposure(
            metadata = defaultMetadata.copy(
                paymentMethodOrientation = PaymentMethodOrientation.Horizontal
            )
        )

        val call = eventReporter.experimentExposureCalls.awaitItem()
        val experiment = call.experiment as LoggableExperiment.OcsMobilePaymentMethodMessagingPromotions
        assertThat(experiment.layout).isEqualTo("horizontal")
    }

    private fun runScenario(
        block: suspend Scenario.() -> Unit
    ) = runTest {
        val eventReporter = FakeEventReporter()
        val handler = DefaultPaymentMethodMessagePromotionsExperimentHandler(
            eventReporter = eventReporter,
            mode = EventReporter.Mode.Complete
        )
        Scenario(
            handler = handler,
            eventReporter = eventReporter
        ).apply { block() }

        eventReporter.validate()
    }

    private class Scenario(
        val handler: DefaultPaymentMethodMessagePromotionsExperimentHandler,
        val eventReporter: FakeEventReporter
    ) {
        fun logExposure(
            code: PaymentMethodCode = PaymentMethod.Type.Affirm.code,
            metadata: PaymentMethodMetadata = defaultMetadata,
            promotion: PaymentMethodMessagePromotion? = defaultPromotion
        ) {
            handler.logExposure(
                code = code,
                metadata = metadata,
                promotion = promotion
            )
        }
    }

    private companion object {
        val defaultMetadata = PaymentMethodMetadataFactory.create(
            stripeIntent = PaymentIntentFixtures.PI_REQUIRES_PAYMENT_METHOD.copy(
                paymentMethodTypes = listOf("affirm,klarna,afterpay_clearpay")
            ),
            experimentsData = ElementsSession.ExperimentsData(
                arbId = "arb_123",
                experimentAssignments = mapOf(
                    ExperimentAssignment.OCS_MOBILE_PAYMENT_METHOD_MESSAGING_PROMOTIONS to "treatment"
                )
            )
        )

        val defaultPromotion = PaymentMethodMessagePromotion(
            paymentMethodType = "affirm",
            message = "This is a message",
            learnMore = PaymentMethodMessageLearnMore(
                message = "Click me",
                url = "https://www.text.com"
            )
        )
    }
}
