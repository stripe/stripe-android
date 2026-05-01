package com.stripe.android.paymentsheet.state

import app.cash.turbine.ReceiveTurbine
import app.cash.turbine.Turbine
import com.google.common.truth.Truth.assertThat
import com.stripe.android.PaymentConfiguration
import com.stripe.android.core.networking.ApiRequest
import com.stripe.android.core.utils.FeatureFlags
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadata
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadataFactory
import com.stripe.android.model.ElementsSession
import com.stripe.android.model.PaymentIntentFixtures
import com.stripe.android.model.PaymentMethodMessageLearnMore
import com.stripe.android.model.PaymentMethodMessagePromotion
import com.stripe.android.model.PaymentMethodMessagePromotionList
import com.stripe.android.paymentsheet.analytics.FakePaymentMethodMessagePromotionsExperimentHandler
import com.stripe.android.paymentsheet.repositories.DefaultPaymentMethodMessagePromotionsHelper
import com.stripe.android.testing.AbsFakeStripeRepository
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Test
import java.util.Locale

class DefaultPaymentMethodMessagePromotionsHelperTest {

    private val testDispatcher = StandardTestDispatcher()

    @Test
    fun `fetchPromotionsAsync does nothing when feature flag is disabled`() = runScenario {
        helper.fetchPromotionsAsync(PaymentIntentFixtures.PI_REQUIRES_PAYMENT_METHOD)
        fakeRepository.calls.expectNoEvents()
    }

    @Test
    fun `fetchPromotionsAsync calls repository when feature flag is enabled`() = runScenario(
        featureFlagEnabled = true,
    ) {
        helper.fetchPromotionsAsync(PaymentIntentFixtures.PI_REQUIRES_PAYMENT_METHOD)
        val request = fakeRepository.calls.awaitItem()
        assertThat(request.amount).isEqualTo(1099)
        assertThat(request.currency).isEqualTo("usd")
        assertThat(request.country).isNull()
        assertThat(request.locale).isEqualTo(Locale.getDefault().language)
    }

    @Test
    fun `getPromotionIfAvailableForCode returns promotion if available and in treatment`() = runScenario(
        featureFlagEnabled = true
    ) {
        helper.fetchPromotionsAsync(PaymentIntentFixtures.PI_REQUIRES_PAYMENT_METHOD)
        dispatcher.scheduler.advanceUntilIdle()
        val metadata = getMetadata("treatment")
        val result = helper.getPromotionIfAvailableForCode(
            "afterpay_clearpay",
            metadata
        )

        assertThat(result).isEqualTo(AFTERPAY_PROMOTION)
        val exposure = experimentHandler.logExposureCalls.awaitItem()
        assertThat(exposure.promotion).isEqualTo(AFTERPAY_PROMOTION)
        assertThat(exposure.code).isEqualTo("afterpay_clearpay")
        assertThat(exposure.metadata).isEqualTo(metadata)
    }

    @Test
    fun `getPromotionIfAvailableForCode does not return promotion if variant is control`() = runScenario(
        featureFlagEnabled = true
    ) {
        helper.fetchPromotionsAsync(PaymentIntentFixtures.PI_REQUIRES_PAYMENT_METHOD)
        dispatcher.scheduler.advanceUntilIdle()
        val metadata = getMetadata("control")
        assertThat(helper.getPromotionIfAvailableForCode("afterpay_clearpay", metadata)).isNull()
        val exposure = experimentHandler.logExposureCalls.awaitItem()
        assertThat(exposure.promotion).isNull()
        assertThat(exposure.code).isEqualTo("afterpay_clearpay")
        assertThat(exposure.metadata).isEqualTo(metadata)
    }

    private fun runScenario(
        featureFlagEnabled: Boolean = false,
        repositoryResult: Result<PaymentMethodMessagePromotionList> = Result.success(
            PaymentMethodMessagePromotionList(
                listOf(AFTERPAY_PROMOTION)
            )
        ),
        block: suspend Scenario.() -> Unit,
    ) = runTest(testDispatcher) {
        FeatureFlags.paymentMethodMessagePromotions.setEnabled(featureFlagEnabled)

        val fakeRepository = FakePromotionsStripeRepository(repositoryResult)
        val testDispatcher = StandardTestDispatcher(testScheduler)
        val experimentHandler = FakePaymentMethodMessagePromotionsExperimentHandler()

        val helper = DefaultPaymentMethodMessagePromotionsHelper(
            stripeRepository = fakeRepository,
            lazyPaymentConfig = {
                PaymentConfiguration("pk_123")
            },
            viewModelScope = this,
            workContext = testDispatcher,
            paymentMethodMessagePromotionsExperimentHandler = experimentHandler
        )

        Scenario(
            helper = helper,
            fakeRepository = fakeRepository,
            dispatcher = testDispatcher,
            experimentHandler = experimentHandler
        ).block()
    }

    private data class Scenario(
        val helper: DefaultPaymentMethodMessagePromotionsHelper,
        val fakeRepository: FakePromotionsStripeRepository,
        val dispatcher: TestDispatcher,
        val experimentHandler: FakePaymentMethodMessagePromotionsExperimentHandler
    )

    private class FakePromotionsStripeRepository(
        private val promotionsResult: Result<PaymentMethodMessagePromotionList>,
    ) : AbsFakeStripeRepository() {
        private val _calls = Turbine<Request>()
        val calls: ReceiveTurbine<Request> = _calls

        override suspend fun retrievePaymentMethodMessagePromotionsForPaymentSheet(
            amount: Int,
            currency: String,
            country: String?,
            locale: String,
            requestOptions: ApiRequest.Options
        ): Result<PaymentMethodMessagePromotionList> {
            _calls.add(
                Request(
                    amount = amount,
                    currency = currency,
                    country = country,
                    locale = locale
                )
            )
            return promotionsResult
        }

        data class Request(
            val amount: Int,
            val currency: String,
            val country: String?,
            val locale: String
        )
    }

    private fun getMetadata(assignment: String? = null): PaymentMethodMetadata {
        return PaymentMethodMetadataFactory.create(
            experimentsData = if (assignment != null) {
                ElementsSession.ExperimentsData(
                    arbId = "arb_123",
                    experimentAssignments = mapOf(
                        ElementsSession.ExperimentAssignment
                            .OCS_MOBILE_PAYMENT_METHOD_MESSAGING_PROMOTIONS to assignment
                    )
                )
            } else {
                null
            }
        )
    }

    private companion object {
        val AFTERPAY_PROMOTION = PaymentMethodMessagePromotion(
            paymentMethodType = "Afterpay_Clearpay",
            message = "Pay in 4 interest-free payments",
            learnMore = PaymentMethodMessageLearnMore(
                url = "https://stripe.com/learn-more",
                message = "Learn more",
            ),
        )
    }
}
