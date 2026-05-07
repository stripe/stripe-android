package com.stripe.android.paymentsheet.state

import com.google.common.truth.Truth.assertThat
import com.stripe.android.core.utils.FeatureFlags
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadataFactory
import com.stripe.android.model.ElementsSession
import com.stripe.android.model.PaymentMethod
import com.stripe.android.model.PaymentMethodMessageLearnMore
import com.stripe.android.model.PaymentMethodMessagePromotion
import com.stripe.android.paymentsheet.analytics.FakePaymentMethodMessagePromotionsExperimentHandler
import com.stripe.android.paymentsheet.repositories.PrefetchedPaymentMethodMessagePromotionsHelper
import kotlinx.coroutines.test.runTest
import org.junit.Test

class PrefetchedPaymentMethodMessagePromotionsHelperTest {

    @Test
    fun `returns promotion when in available and in treatment`() = runScenario {
        val result = helper.getPromotionIfAvailableForCode(
            PaymentMethod.Type.Affirm.code,
            PaymentMethodMetadataFactory.create(
                experimentsData = ElementsSession.ExperimentsData(
                        arbId = "arb_123",
                        experimentAssignments = mapOf(
                            ElementsSession.ExperimentAssignment
                                .OCS_MOBILE_PAYMENT_METHOD_MESSAGING_PROMOTIONS to "treatment"
                        )
                    )
                )
            )

        assertThat(result).isEqualTo(promotion)
        experimentHandler.logExposureCalls.awaitItem()
    }

    @Test
    fun `does not return promotion when in available and in control`() = runScenario {
        val result = helper.getPromotionIfAvailableForCode(
            PaymentMethod.Type.Affirm.code,
            PaymentMethodMetadataFactory.create(
                experimentsData = ElementsSession.ExperimentsData(
                    arbId = "arb_123",
                    experimentAssignments = mapOf(
                        ElementsSession.ExperimentAssignment
                            .OCS_MOBILE_PAYMENT_METHOD_MESSAGING_PROMOTIONS to "control"
                    )
                )
            )
        )
        assertThat(result).isNull()
        experimentHandler.logExposureCalls.awaitItem()
    }

    @Test
    fun `does not return promotion when experiment not assigned`() = runScenario {
        val result = helper.getPromotionIfAvailableForCode(
            PaymentMethod.Type.Affirm.code,
            PaymentMethodMetadataFactory.create(
                experimentsData = ElementsSession.ExperimentsData(
                    arbId = "arb_123",
                    experimentAssignments = mapOf()
                )
            )
        )
        assertThat(result).isNull()
        experimentHandler.logExposureCalls.awaitItem()
    }

    private fun runScenario(
        block: suspend Scenario.() -> Unit
    ) = runTest {
        FeatureFlags.paymentMethodMessagePromotions.setEnabled(true)
        val experimentHandler = FakePaymentMethodMessagePromotionsExperimentHandler()
        val helper = PrefetchedPaymentMethodMessagePromotionsHelper(
            promotions = listOf(promotion),
            paymentMethodMessagePromotionsExperimentHandler = experimentHandler
        )

        Scenario(
            helper = helper,
            experimentHandler = experimentHandler
        ).block()
    }

    private class Scenario(
        val helper: PrefetchedPaymentMethodMessagePromotionsHelper,
        val experimentHandler: FakePaymentMethodMessagePromotionsExperimentHandler
    )

    private companion object {
        val promotion = PaymentMethodMessagePromotion(
            paymentMethodType = "affirm",
            message = "message",
            learnMore = PaymentMethodMessageLearnMore(
                message = "message",
                url = "url"
            )
        )
    }
}
